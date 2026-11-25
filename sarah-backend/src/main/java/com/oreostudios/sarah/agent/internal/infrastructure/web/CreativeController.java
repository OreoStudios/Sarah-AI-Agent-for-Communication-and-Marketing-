package com.oreostudios.sarah.agent.internal.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.oreostudios.sarah.audit.api.AuditFacade;
import com.oreostudios.sarah.autonomy.api.AutonomyFacade;
import com.oreostudios.sarah.brand.api.BrandFacade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.net.http.HttpClient;
import java.time.*;
import java.sql.Date;
import java.util.*;

@RestController
@RequestMapping("/api/v1/marketing/creatives")
@PreAuthorize("hasRole('OWNER')")
public class CreativeController {
    private final JdbcTemplate db; private final BrandFacade brand; private final AutonomyFacade autonomy;
    private final AuditFacade audit; private final RestClient api;
    private final String key,model; private final boolean enabled; private final int dailyLimit;
    public CreativeController(JdbcTemplate db,BrandFacade brand,AutonomyFacade autonomy,AuditFacade audit,
            @Value("${OPENAI_API_KEY:}") String key,@Value("${SARAH_IMAGE_MODEL:gpt-image-1.5}") String model,
            @Value("${SARAH_IMAGE_ENABLED:false}") boolean enabled,@Value("${SARAH_IMAGE_DAILY_LIMIT:3}") int dailyLimit){
        this.db=db;this.brand=brand;this.autonomy=autonomy;this.audit=audit;this.key=key;this.model=model;this.enabled=enabled;
        this.dailyLimit=Math.max(1,Math.min(10,dailyLimit));
        var f=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build());
        f.setReadTimeout(Duration.ofMinutes(3));api=RestClient.builder().baseUrl("https://api.openai.com").requestFactory(f).build();
    }
    public record Request(String brief,String size,boolean confirmed){}
    public record Creative(UUID id,String status,String model,String size,String downloadUrl){}
    @PostMapping public Creative generate(@RequestBody Request req,Authentication auth){
        if(!enabled||key.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Image provider is disabled");
        if(!autonomy.isEnabled())throw new ResponseStatusException(HttpStatus.CONFLICT,"Marketing activity paused");
        if(!req.confirmed()||req.brief()==null||req.brief().isBlank()||req.brief().length()>2000||!allowedSize(req.size()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Confirm paid image generation and provide a brief and supported size");
        UUID id=UUID.randomUUID();Date day=Date.valueOf(LocalDate.now(ZoneOffset.UTC));
        int reserved=db.update("INSERT INTO creative_daily_quota(day,used) VALUES (?,1) ON CONFLICT(workspace_id,day) DO UPDATE SET used=creative_daily_quota.used+1 WHERE creative_daily_quota.used < ?",day,dailyLimit);
        if(reserved!=1)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Daily creative budget reached");
        db.update("INSERT INTO creative_asset(id,brief,model,size,status) VALUES (?,?,?,?,'GENERATING')",id,req.brief(),model,req.size());
        try{
            var profile=brand.active();
            String prompt="Create an original marketing visual for "+profile.companyName()+". Brand services: "+profile.services()+". Creative brief: "+req.brief()+". No false claims, fabricated endorsements, or misleading logos.";
            JsonNode body=api.post().uri("/v1/images/generations").headers(h->h.setBearerAuth(key)).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model",model,"prompt",prompt,"size",req.size(),"quality","medium","n",1,"output_format","png"))
                .retrieve().body(JsonNode.class);
            String encoded=body==null?"":body.path("data").path(0).path("b64_json").asText("");
            if(encoded.isBlank()||encoded.length()>28_000_000)throw new IllegalStateException();
            byte[] image=Base64.getDecoder().decode(encoded);
            if(image.length>20_000_000||!isPng(image))throw new IllegalStateException();
            db.update("UPDATE creative_asset SET image=?,status='READY' WHERE id=?",image,id);
        }catch(RuntimeException e){
            db.update("UPDATE creative_asset SET status='FAILED' WHERE id=?",id);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Image generation failed; inspect provider quota and model access. Attempt counted.");
        }
        audit.record(auth.getName(),"CREATIVE_GENERATED","creative",id.toString(),"model="+model);
        return new Creative(id,"READY",model,req.size(),"/api/v1/marketing/creatives/"+id+"/image");
    }
    @GetMapping public List<Map<String,Object>> list(){return db.queryForList("SELECT id,brief,model,size,status,created_at FROM creative_asset ORDER BY created_at DESC LIMIT 50");}
    @GetMapping(value="/{id}/image",produces=MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> image(@PathVariable UUID id){
        var rows=db.query("SELECT image FROM creative_asset WHERE id=? AND status='READY'",(rs,i)->rs.getBytes(1),id);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore()).body(rows.getFirst());
    }
    @DeleteMapping("/{id}") public void remove(@PathVariable UUID id,Authentication auth){db.update("DELETE FROM creative_asset WHERE id=? AND status<>'GENERATING'",id);audit.record(auth.getName(),"CREATIVE_DELETED","creative",id.toString(),null);}
    static boolean allowedSize(String size){return size!=null&&Set.of("1024x1024","1024x1536","1536x1024").contains(size);}
    static boolean isPng(byte[] b){return b!=null&&b.length>=8&&Arrays.equals(Arrays.copyOf(b,8),new byte[]{(byte)137,80,78,71,13,10,26,10});}
}
