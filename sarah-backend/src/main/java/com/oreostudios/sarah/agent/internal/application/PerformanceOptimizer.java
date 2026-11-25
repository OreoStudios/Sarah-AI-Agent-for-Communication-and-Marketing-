package com.oreostudios.sarah.agent.internal.application;

import com.oreostudios.sarah.autonomy.api.AutonomyFacade;
import com.oreostudios.sarah.audit.api.AuditFacade;
import com.oreostudios.sarah.publishing.api.*;
import com.oreostudios.sarah.shared.Platform;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class PerformanceOptimizer {
    private final JdbcTemplate db;private final PublishingFacade publishing;private final AutonomyFacade autonomy;
    private final AuditFacade audit;private final boolean enabled;
    public PerformanceOptimizer(JdbcTemplate db,PublishingFacade publishing,AutonomyFacade autonomy,AuditFacade audit,
            @Value("${SARAH_OPTIMIZATION_ENABLED:false}") boolean enabled){
        this.db=db;this.publishing=publishing;this.autonomy=autonomy;this.audit=audit;this.enabled=enabled;
    }
    public record Observation(UUID publicationId,long impressions,long clicks,long conversions,String source){}
    public record Score(String platform,long impressions,long conversions,int posts,double weight,String explanation){}
    public record Decision(List<Score> scores,boolean applied){}
    @Transactional
    public void observe(Observation o,String actor){
        if(o.publicationId()==null||!valid(o.impressions(),o.clicks(),o.conversions())||
            o.source()==null||o.source().isBlank()||o.source().length()>200)
            throw new IllegalArgumentException("Supply valid cumulative impressions, clicks, conversions and measurement source");
        var p=publishing.get(o.publicationId());
        if(p.status()!=PublicationStatus.PUBLISHED)throw new IllegalArgumentException("Only confirmed publications can have performance observations");
        db.update("INSERT INTO marketing_observation(publication_id,platform,impressions,clicks,conversions,source,observed_at) VALUES (?,?,?,?,?,?,now()) ON CONFLICT(publication_id) DO UPDATE SET impressions=excluded.impressions,clicks=excluded.clicks,conversions=excluded.conversions,source=excluded.source,observed_at=now()",o.publicationId(),p.platform().code(),o.impressions(),o.clicks(),o.conversions(),o.source());
        audit.record(actor,"PERFORMANCE_OBSERVED","publication",o.publicationId().toString(),"source="+o.source());
    }
    @Scheduled(cron="${SARAH_OPTIMIZATION_CRON:0 0 7 * * *}",zone="Europe/Paris")
    public void daily(){if(enabled&&autonomy.isEnabled()) recompute("system");}
    @Transactional
    public Decision recompute(String actor){
        if(!autonomy.isEnabled())return new Decision(List.of(),false);
        var rows=db.query("SELECT platform,sum(impressions) impressions,sum(conversions) conversions,count(*) posts FROM marketing_observation WHERE observed_at>=now()-interval '30 days' GROUP BY platform",
            (rs,i)->score(rs.getString("platform"),rs.getLong("impressions"),rs.getLong("conversions"),rs.getInt("posts")));
        Map<String,Score> by=new HashMap<>();for(var row:rows)by.put(row.platform(),row);
        List<Score> scores=new ArrayList<>();for(Platform p:Platform.values())scores.add(by.getOrDefault(p.code(),score(p.code(),0,0,0)));
        for(var s:scores)db.update("INSERT INTO marketing_strategy_weight(platform,weight,impressions,conversions,posts,explanation,updated_at) VALUES (?,?,?,?,?,?,now()) ON CONFLICT(platform) DO UPDATE SET weight=excluded.weight,impressions=excluded.impressions,conversions=excluded.conversions,posts=excluded.posts,explanation=excluded.explanation,updated_at=now()",s.platform(),s.weight(),s.impressions(),s.conversions(),s.posts(),s.explanation());
        audit.record(actor,"MARKETING_STRATEGY_OPTIMIZED","marketing",null,"channels="+scores.size());
        return new Decision(List.copyOf(scores),true);
    }
    public List<Score> current(){return db.query("SELECT platform,impressions,conversions,posts,weight,explanation FROM marketing_strategy_weight ORDER BY weight DESC,platform",(rs,i)->new Score(rs.getString(1),rs.getLong(2),rs.getLong(3),rs.getInt(4),rs.getDouble(5),rs.getString(6)));}
    static boolean valid(long impressions,long clicks,long conversions){return impressions>0&&impressions<=1_000_000_000L&&clicks>=0&&conversions>=0&&conversions<=clicks&&clicks<=impressions;}
    static Score score(String platform,long impressions,long conversions,int posts){
        if(impressions<500||posts<3)return new Score(platform,impressions,conversions,posts,1,"Insufficient evidence; neutral allocation");
        double rate=(conversions+10.0)/(impressions+1000.0);
        double weight=Math.max(0.5,Math.min(2.0,rate/0.01));
        return new Score(platform,impressions,conversions,posts,weight,"Smoothed conversion rate vs 1% reference; capped allocation weight");
    }
}
