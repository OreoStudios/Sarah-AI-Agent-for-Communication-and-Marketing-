package com.oreostudios.sarah.dashboard.internal.infrastructure.web;

import com.oreostudios.sarah.publishing.api.PublishingFacade;
import com.oreostudios.sarah.publishing.api.PublicationStatus;
import com.oreostudios.sarah.publishing.api.PublicationView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/v1/dashboard/analytics")
@PreAuthorize("hasRole('OWNER')")
public class MarketingAnalyticsController {
    private final PublishingFacade publishing;
    public MarketingAnalyticsController(PublishingFacade publishing){this.publishing=publishing;}
    public record Metrics(long published,long failed,long pending,long cancelled,long attempts,Double deliverySuccessRate){}
    public record Report(Instant from,Instant to,Map<String,Metrics> byPlatform,Metrics total,String basis){}
    @GetMapping
    public Report report(@RequestParam(defaultValue="30") int days){
        if(days<1 || days>90) throw new IllegalArgumentException("days must be between 1 and 90");
        Instant to=Instant.now(),from=to.minus(Duration.ofDays(days));
        List<PublicationView> rows=publishing.calendar(from,to);
        Map<String,List<PublicationView>> grouped=new TreeMap<>();
        for(var row:rows) grouped.computeIfAbsent(row.platform().code(),k->new ArrayList<>()).add(row);
        Map<String,Metrics> result=new TreeMap<>();
        grouped.forEach((k,v)->result.put(k,metrics(v)));
        return new Report(from,to,result,metrics(rows),"Scheduled publication cohort; delivery records only, not reach or engagement");
    }
    static Metrics metrics(List<PublicationView> rows){
        long published=0,failed=0,pending=0,cancelled=0,attempts=0;
        for(var row:rows){
            attempts+=row.attempts();
            switch(row.status()){
                case PUBLISHED -> published++;
                case FAILED -> failed++;
                case CANCELLED -> cancelled++;
                case SCHEDULED,PUBLISHING -> pending++;
            }
        }
        return new Metrics(published,failed,pending,cancelled,attempts,
                published+failed==0?null:(double)published/(published+failed));
    }
}
