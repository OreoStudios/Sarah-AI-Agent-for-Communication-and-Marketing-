package com.oreostudios.sarah.prospecting.internal.application;

import com.oreostudios.sarah.prospecting.internal.domain.Prospect;
import com.oreostudios.sarah.prospecting.internal.infrastructure.JpaProspectRepository;
import com.oreostudios.sarah.prospecting.internal.infrastructure.web.CompanyDiscoveryController;
import com.oreostudios.sarah.autonomy.api.AutonomyFacade;
import com.oreostudios.sarah.audit.api.AuditFacade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.sql.Date;
import java.util.*;

@Service
public class AutoProspectingService {
    private final CompanyDiscoveryController discovery; private final JpaProspectRepository repo;
    private final AutonomyFacade autonomy; private final AuditFacade audit; private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final boolean enabled; private final String query,postal,activityPrefix;
    public AutoProspectingService(CompanyDiscoveryController discovery,JpaProspectRepository repo,AutonomyFacade autonomy,
            AuditFacade audit,JdbcTemplate jdbc,org.springframework.transaction.PlatformTransactionManager tx,
            @Value("${SARAH_PROSPECTING_ENABLED:false}") boolean enabled,
            @Value("${SARAH_PROSPECTING_QUERY:}") String query,
            @Value("${SARAH_PROSPECTING_POSTAL_CODE:}") String postal,
            @Value("${SARAH_PROSPECTING_ACTIVITY_PREFIX:}") String activityPrefix){
        this.discovery=discovery;this.repo=repo;this.autonomy=autonomy;this.audit=audit;this.jdbc=jdbc;this.transactions=new TransactionTemplate(tx);
        this.enabled=enabled;this.query=query;this.postal=postal;this.activityPrefix=activityPrefix;
    }
    @Scheduled(cron="${SARAH_PROSPECTING_CRON:0 15 7 * * *}",zone="Europe/Paris")
    public void daily(){if(enabled&&autonomy.isEnabled()&&!query.isBlank()) run(query,postal,activityPrefix,"system");}
    public record Result(String status,int imported,int existing,int filtered){}
    public Result run(String q,String postalCode,String prefix,String actor){
        if(!autonomy.isEnabled())return new Result("PAUSED",0,0,0);
        if(q==null||q.strip().length()<3||q.length()>120||prefix==null||!prefix.matches("[0-9]{2}(?:\\.[0-9]{1,2})?"))
            throw new IllegalArgumentException("Specify a business search and target NAF activity prefix");
        LocalDate today=LocalDate.now(ZoneId.of("Europe/Paris"));
        int claim=jdbc.update("INSERT INTO prospect_discovery_run(run_day,status) VALUES (?, 'RUNNING') ON CONFLICT DO NOTHING",Date.valueOf(today));
        if(claim==0)return new Result("ALREADY_RUN",0,0,0);
        int added=0,existing=0,filtered=0;
        try {
            for(var c:discovery.search(q,postalCode==null||postalCode.isBlank()?null:postalCode,1).candidates()){
                if(!autonomy.isEnabled())break;
                if(!c.activityCode().startsWith(prefix)){filtered++;continue;}
                Boolean inserted=transactions.execute(status->{
                    if(repo.findByRegistrySiren(c.siren()).isPresent())return false;
                    Prospect p=new Prospect(c.companyName(),null,c.activityCode(),c.locality());
                    p.attachRegistrySiren(c.siren());
                    p.update(c.activityCode(),c.locality(),score(c.activityCode(),prefix,c.locality()),"Registry source: "+c.sourceUrl()+"; fit based only on sector/locality. Contact consent not established.",null);
                    repo.saveAndFlush(p);return true;
                });
                if(Boolean.TRUE.equals(inserted))added++;else existing++;
            }
            jdbc.update("UPDATE prospect_discovery_run SET status=?, imported=? WHERE run_day=?",autonomy.isEnabled()?"DONE":"PAUSED",added,Date.valueOf(today));
            audit.record(actor,"B2B_DISCOVERY","marketing",null,"imported="+added+" existing="+existing);
            return new Result(autonomy.isEnabled()?"DONE":"PAUSED",added,existing,filtered);
        }catch(RuntimeException e){
            jdbc.update("UPDATE prospect_discovery_run SET status='FAILED', imported=? WHERE run_day=?",added,Date.valueOf(today));
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY,"Discovery failed; inspect registry access and run history");
        }
    }
    static int score(String code,String prefix,String locality){return code.startsWith(prefix)?(locality==null||locality.isBlank()?60:75):0;}
    public List<Map<String,Object>> history(){return jdbc.queryForList("SELECT run_day,status,imported,created_at FROM prospect_discovery_run ORDER BY run_day DESC LIMIT 30");}
}
