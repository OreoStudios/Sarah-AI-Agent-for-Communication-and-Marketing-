package com.oreostudios.sarah.agent.internal.infrastructure.web;

import com.oreostudios.sarah.audit.api.AuditFacade;
import com.oreostudios.sarah.autonomy.api.AutonomyFacade;
import com.oreostudios.sarah.brand.api.BrandFacade;
import com.oreostudios.sarah.llm.api.*;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/marketing/email")
@PreAuthorize("hasRole('OWNER')")
public class EmailMarketingController {
    private final JdbcTemplate db;private final AutonomyFacade autonomy;private final AuditFacade audit;
    private final LlmClient llm;private final PromptSanitizer sanitizer;private final BrandFacade brand;
    private final ObjectProvider<JavaMailSender> mail;
    private final boolean enabled;private final String from,replyTo,postalAddress;private final int dailyLimit;
    public EmailMarketingController(JdbcTemplate db,AutonomyFacade autonomy,AuditFacade audit,LlmClient llm,
            PromptSanitizer sanitizer,BrandFacade brand,ObjectProvider<JavaMailSender> mail,
            @Value("${SARAH_EMAIL_MARKETING_ENABLED:false}") boolean enabled,
            @Value("${SARAH_MARKETING_FROM:}") String from,@Value("${SARAH_MARKETING_REPLY_TO:}") String replyTo,
            @Value("${SARAH_MARKETING_POSTAL_ADDRESS:}") String postalAddress,
            @Value("${SARAH_MARKETING_EMAIL_DAILY_LIMIT:10}") int dailyLimit){
        this.db=db;this.autonomy=autonomy;this.audit=audit;this.llm=llm;this.sanitizer=sanitizer;this.brand=brand;this.mail=mail;
        this.enabled=enabled;this.from=from;this.replyTo=replyTo;this.postalAddress=postalAddress;
        this.dailyLimit=Math.max(1,Math.min(50,dailyLimit));
    }
    public record Consent(String email,String evidence,boolean confirmed){}
    public record Brief(String subject,String objective,String language){}
    public record Draft(UUID id,String subject,String body,String status){}
    public record Approval(List<String> recipients,boolean confirmed){}
    public record Delivery(UUID campaignId,int accepted,int failed,int skipped){}
    @PostMapping("/subscribers")
    public Map<String,String> consent(@RequestBody Consent input,Authentication auth){
        String email=address(input.email());
        if(!input.confirmed()||input.evidence()==null||input.evidence().isBlank()||input.evidence().length()>500)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Document explicit marketing consent");
        // Suppression is permanent here; importing again never resurrects an opted-out recipient.
        int count=db.update("INSERT INTO marketing_subscriber(email,consent_evidence) VALUES (?,?) ON CONFLICT DO NOTHING",email,input.evidence());
        audit.record(auth.getName(),"MARKETING_CONSENT_RECORDED","marketing",null,"new="+count);
        return Map.of("status",count==1?"ADDED":"EXISTING_NOT_CHANGED");
    }
    @PostMapping("/unsubscribe")
    public void suppress(@RequestBody Map<String,String> input,Authentication auth){
        String email=address(input.get("email"));
        db.update("INSERT INTO marketing_subscriber(email,consent_evidence,suppressed) VALUES (?,'opt-out',true) ON CONFLICT(email) DO UPDATE SET suppressed=true",email);
        audit.record(auth.getName(),"MARKETING_EMAIL_SUPPRESSED","marketing",null,null);
    }
    @GetMapping("/subscribers")
    public List<Map<String,Object>> subscribers(){return db.queryForList("SELECT email,suppressed,created_at FROM marketing_subscriber ORDER BY created_at DESC LIMIT 200");}
    @PostMapping("/drafts")
    public Draft draft(@RequestBody Brief input,Authentication auth){
        if(!llm.isLive())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Configure a live AI provider");
        String subject=subject(input.subject());
        if(input.objective()==null||input.objective().isBlank()||input.objective().length()>1000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Campaign objective required");
        String body=llm.generate(sanitizer.guardClause()+"\nWrite a concise plain-text permission-based marketing email. No invented claims, fake urgency, tracking links, attachments or misleading identity. Return body only.",
            sanitizer.wrapUntrusted("brief",subject+"\n"+input.objective()+"\n"+input.language())+"\n"+sanitizer.wrapUntrusted("brand",brand.active().companyName()+" "+brand.active().services()));
        if(body==null||body.isBlank()||body.length()>8000)throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Invalid email draft");
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO marketing_email_campaign(id,subject,body) VALUES (?,?,?)",id,subject,body);
        audit.record(auth.getName(),"MARKETING_EMAIL_DRAFT","marketing",id.toString(),null);
        return new Draft(id,subject,body,"DRAFT");
    }
    @GetMapping("/drafts")
    public List<Map<String,Object>> drafts(){return db.queryForList("SELECT id,subject,body,status,created_at FROM marketing_email_campaign ORDER BY created_at DESC LIMIT 50");}
    @PostMapping("/drafts/{id}/send")
    public Delivery send(@PathVariable UUID id,@RequestBody Approval input,Authentication auth){
        if(!enabled||mail.getIfAvailable()==null||postalAddress.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Configure marketing SMTP, sender identity and postal address");
        String sender=address(from),reply=address(replyTo);
        if(!autonomy.isEnabled())throw new ResponseStatusException(HttpStatus.CONFLICT,"Marketing paused");
        if(!input.confirmed()||input.recipients()==null||input.recipients().isEmpty()||input.recipients().size()>dailyLimit)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Explicit approval and bounded recipient list required");
        Set<String> recipients=new LinkedHashSet<>();for(String e:input.recipients())recipients.add(address(e));
        var campaigns=db.queryForList("SELECT subject,body FROM marketing_email_campaign WHERE id=?",id);
        if(campaigns.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if(db.update("UPDATE marketing_email_campaign SET status='SENDING' WHERE id=? AND status='DRAFT'",id)!=1)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Campaign already approved; reconcile delivery history");
        int accepted=0,failed=0,skipped=0;
        for(String recipient:recipients){
            if(!autonomy.isEnabled()){skipped++;continue;}
            Integer consent=db.queryForObject("SELECT count(*) FROM marketing_subscriber WHERE email=? AND suppressed=false",Integer.class,recipient);
            if(consent==null||consent!=1){skipped++;continue;}
            int quota=db.update("INSERT INTO marketing_email_quota(day,used) VALUES (CURRENT_DATE,1) ON CONFLICT(day) DO UPDATE SET used=marketing_email_quota.used+1 WHERE marketing_email_quota.used<?",dailyLimit);
            if(quota!=1){skipped++;continue;}
            if(db.update("INSERT INTO marketing_email_delivery(campaign_id,email,status) VALUES (?,?,'CLAIMED') ON CONFLICT DO NOTHING",id,recipient)!=1){skipped++;continue;}
            try{
                // Check opt-out and kill switch again immediately before submission.
                if(!autonomy.isEnabled()||!Boolean.TRUE.equals(db.queryForObject("SELECT NOT suppressed FROM marketing_subscriber WHERE email=?",Boolean.class,recipient))){
                    db.update("UPDATE marketing_email_delivery SET status='SKIPPED' WHERE campaign_id=? AND email=?",id,recipient);skipped++;continue;
                }
                var msg=new SimpleMailMessage();msg.setFrom(sender);msg.setReplyTo(reply);msg.setTo(recipient);
                msg.setSubject((String)campaigns.getFirst().get("subject"));
                msg.setText(campaigns.getFirst().get("body")+"\n\n"+brand.active().companyName()+"\n"+postalAddress+"\nTo stop receiving marketing emails, reply UNSUBSCRIBE to "+reply+".");
                mail.getObject().send(msg);
                db.update("UPDATE marketing_email_delivery SET status='SMTP_ACCEPTED' WHERE campaign_id=? AND email=?",id,recipient);accepted++;
            }catch(RuntimeException e){
                db.update("UPDATE marketing_email_delivery SET status='UNCONFIRMED' WHERE campaign_id=? AND email=?",id,recipient);failed++;
            }
        }
        db.update("UPDATE marketing_email_campaign SET status='FINISHED' WHERE id=?",id);
        audit.record(auth.getName(),"MARKETING_EMAIL_APPROVED","marketing",id.toString(),"smtpAccepted="+accepted+" unconfirmed="+failed+" skipped="+skipped);
        return new Delivery(id,accepted,failed,skipped);
    }
    @GetMapping("/drafts/{id}/deliveries")
    public List<Map<String,Object>> deliveries(@PathVariable UUID id){return db.queryForList("SELECT email,status,created_at FROM marketing_email_delivery WHERE campaign_id=?",id);}
    static String address(String raw){
        try{
            if(raw==null||raw.length()>254||raw.contains("\n")||raw.contains("\r"))throw new IllegalArgumentException();
            InternetAddress a=new InternetAddress(raw.strip(),true);a.validate();
            if(!a.getAddress().equals(raw.strip())||!a.getAddress().contains("@"))throw new IllegalArgumentException();
            return a.getAddress().toLowerCase(Locale.ROOT);
        }catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid email address");}
    }
    static String subject(String raw){
        if(raw==null||raw.isBlank()||raw.length()>150||raw.contains("\n")||raw.contains("\r"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid subject");
        return raw.strip();
    }
}
