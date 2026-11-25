package com.oreostudios.sarah.agent.internal.application;

import com.oreostudios.sarah.audit.api.AuditFacade;
import com.oreostudios.sarah.autonomy.api.AutonomyFacade;
import com.oreostudios.sarah.brand.api.BrandFacade;
import com.oreostudios.sarah.campaign.api.*;
import com.oreostudios.sarah.connector.api.*;
import com.oreostudios.sarah.llm.api.LlmClient;
import com.oreostudios.sarah.publishing.api.*;
import com.oreostudios.sarah.shared.Platform;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.*;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DailyMarketingPlanner {
    private final JdbcTemplate db;private final AutonomyFacade autonomy;private final BrandFacade brand;
    private final CampaignFacade campaigns;private final ConnectorFacade connectors;private final PublishingFacade publishing;
    private final ContentAgent content;private final ComplianceAgent compliance;private final LlmClient llm;
    private final PerformanceOptimizer optimizer;private final AuditFacade audit;
    private final boolean enabled,autoPublish;private final String objective,audience,language,platforms;private final int maxChannels;
    public DailyMarketingPlanner(JdbcTemplate db,AutonomyFacade autonomy,BrandFacade brand,CampaignFacade campaigns,
            ConnectorFacade connectors,PublishingFacade publishing,ContentAgent content,ComplianceAgent compliance,
            LlmClient llm,PerformanceOptimizer optimizer,AuditFacade audit,
            @Value("${SARAH_DAILY_PLAN_ENABLED:false}") boolean enabled,
            @Value("${SARAH_DAILY_AUTO_PUBLISH:false}") boolean autoPublish,
            @Value("${SARAH_DAILY_OBJECTIVE:}") String objective,
            @Value("${SARAH_DAILY_AUDIENCE:}") String audience,
            @Value("${SARAH_DAILY_LANGUAGE:en}") String language,
            @Value("${SARAH_DAILY_PLATFORMS:discord}") String platforms,
            @Value("${SARAH_DAILY_MAX_CHANNELS:2}") int maxChannels){
        this.db=db;this.autonomy=autonomy;this.brand=brand;this.campaigns=campaigns;this.connectors=connectors;
        this.publishing=publishing;this.content=content;this.compliance=compliance;this.llm=llm;this.optimizer=optimizer;this.audit=audit;
        this.enabled=enabled;this.autoPublish=autoPublish;this.objective=objective;this.audience=audience;
        this.language=language;this.platforms=platforms;this.maxChannels=Math.max(1,Math.min(3,maxChannels));
    }
    @Scheduled(cron="${SARAH_DAILY_PLAN_CRON:0 30 7 * * *}",zone="Europe/Paris")
    public void daily(){if(enabled&&autonomy.isEnabled()&&llm.isLive())run("system");}
    public record PlanResult(String status,UUID campaignId,int drafts,int scheduled,String detail){}
    public PlanResult run(String actor){
        if(!enabled||!autonomy.isEnabled()||!llm.isLive())return new PlanResult("DISABLED",null,0,0,"Enable daily planning, resume activity and configure a live LLM");
        if(objective.isBlank()||objective.length()>1000||audience.isBlank()||audience.length()>200||!Set.of("en","fr").contains(language))
            return new PlanResult("NEEDS_SETUP",null,0,0,"Configure objective, audience and language");
        Set<Platform> allowed=new LinkedHashSet<>();
        try{for(String p:platforms.split(","))allowed.add(Platform.fromCode(p.strip()));}
        catch(RuntimeException e){return new PlanResult("NEEDS_SETUP",null,0,0,"Invalid channel allowlist");}
        var settings=autonomy.current();
        int cap=Math.min(maxChannels,Math.min(settings.maxLlmCallsPerRun(),settings.dailyPostLimit()));
        if(cap<=0||allowed.isEmpty())return new PlanResult("LIMIT_REACHED",null,0,0,"No channel budget");
        LocalDate today=LocalDate.now(ZoneId.of("Europe/Paris"));Date day=Date.valueOf(today);
        if(db.update("INSERT INTO marketing_daily_plan(day,status) VALUES (?,'RUNNING') ON CONFLICT DO NOTHING",day)!=1)
            return new PlanResult("ALREADY_RUN",null,0,0,"Daily run already claimed");
        UUID campaignId=null;int drafts=0,scheduled=0;
        try{
            Map<String,Double> weights=optimizer.current().stream().collect(Collectors.toMap(PerformanceOptimizer.Score::platform,PerformanceOptimizer.Score::weight));
            List<Platform> selected=select(allowed,weights,cap,today.toEpochDay());
            var campaign=campaigns.create(new CreateCampaignRequest("Daily marketing "+today,objective,audience,
                    selected.stream().map(Platform::code).toList(),language,0,Instant.now(),null),actor);
            campaignId=campaign.id();
            db.update("UPDATE marketing_daily_plan SET campaign_id=?, platforms=? WHERE day=?",campaignId,selected.stream().map(Platform::code).collect(Collectors.joining(",")),day);
            var profile=brand.active();
            // A rotating owner-approved service focus avoids generating the identical brief daily.
            List<String> services=profile.serviceList();
            String focus=services.isEmpty()?objective:services.get(Math.floorMod(today.toEpochDay(),services.size()));
            String brief=objective+"\nToday's service focus: "+focus+". Offer one helpful, specific insight and an honest call to action. Avoid invented claims.";
            var channels=connectors.listChannels();
            for(Platform p:selected){
                if(!autonomy.isEnabled())break;
                var gen=content.generate(profile,brief,audience,language,p);var verdict=compliance.evaluate(p,language,gen.body());
                boolean approved=verdict.approved()&&verdict.violations().isEmpty()&&gen.body().length()<=p.maxChars();
                var draft=campaigns.addDraft(new AddDraftCommand(campaignId,p.code(),language,gen.title(),gen.body(),gen.imageBrief(),verdict.score(),verdict.advisory(),approved));drafts++;
                if(autoPublish&&approved&&p!=Platform.INSTAGRAM&&p!=Platform.REDDIT&&autonomy.isEnabled()){
                    var channel=channels.stream().filter(c->c.type()==p&&c.live()&&c.status()==ConnectorStatus.CONNECTED).findFirst();
                    if(channel.isPresent()){
                        publishing.schedule(new SchedulePublicationCommand(campaignId,draft.id(),channel.get().id(),p,gen.body(),Instant.now().plus(Duration.ofMinutes(5+scheduled*30L)),1,"daily:"+today+":"+p.code()));
                        scheduled++;
                    }
                }
            }
            campaigns.transition(campaignId,CampaignStatus.PLANNED,actor);
            campaigns.transition(campaignId,CampaignStatus.REVIEWED,actor);
            if(scheduled>0)campaigns.transition(campaignId,CampaignStatus.SCHEDULED,actor);
            String status=autonomy.isEnabled()?"DONE":"PAUSED";
            db.update("UPDATE marketing_daily_plan SET status=?,drafts=?,scheduled=? WHERE day=?",status,drafts,scheduled,day);
            audit.record(actor,"DAILY_MARKETING_PLAN","campaign",campaignId.toString(),"drafts="+drafts+" scheduled="+scheduled);
            return new PlanResult(status,campaignId,drafts,scheduled,"Owner-approved scope; Instagram and Reddit always require review");
        }catch(RuntimeException e){
            db.update("UPDATE marketing_daily_plan SET status='FAILED',drafts=?,scheduled=? WHERE day=?",drafts,scheduled,day);
            return new PlanResult("FAILED",campaignId,drafts,scheduled,"Inspect campaign and calendar before further action; no automatic duplicate retry");
        }
    }
    static List<Platform> select(Set<Platform> allowed,Map<String,Double> weights,int cap,long day){
        List<Platform> sorted=new ArrayList<>(allowed);
        sorted.sort(Comparator.<Platform>comparingDouble(p->weights.getOrDefault(p.code(),1.0)).reversed()
            .thenComparingInt(p->Math.floorMod(p.ordinal()-(int)Math.floorMod(day,Platform.values().length),Platform.values().length)));
        // Every fourth day reserve one exploratory slot so undermeasured channels can recover.
        if(Math.floorMod(day,4)==0&&sorted.size()>1)Collections.rotate(sorted,Math.floorMod(day/4,sorted.size()));
        return List.copyOf(sorted.subList(0,Math.min(cap,sorted.size())));
    }
    public List<Map<String,Object>> history(){return db.queryForList("SELECT day,status,campaign_id,platforms,drafts,scheduled,created_at FROM marketing_daily_plan ORDER BY day DESC LIMIT 30");}
}
