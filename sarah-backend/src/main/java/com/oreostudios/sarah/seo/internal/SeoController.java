package com.oreostudios.sarah.seo.internal;

import com.fasterxml.jackson.databind.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/seo")
@PreAuthorize("hasRole('OWNER')")
public class SeoController {
    private final SiteCrawler crawler;
    private final SeoProviders providers;
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final Environment env;
    private final AtomicBoolean busy=new AtomicBoolean();
    public SeoController(SiteCrawler crawler,SeoProviders providers,JdbcTemplate db,ObjectMapper json,Environment env){
        this.crawler=crawler;this.providers=providers;this.db=db;this.json=json;this.env=env;
    }
    record Request(String target,String keyword,String provider,String query,String url,String strategy){}
    @GetMapping("/history")
    public List<Map<String,Object>> history(){
        return db.query("SELECT id, kind, subject, created_at FROM seo_snapshot ORDER BY created_at DESC LIMIT 100",
          (r,i)->Map.of("id",r.getLong(1),"kind",r.getString(2),"subject",r.getString(3),"at",r.getTimestamp(4).toInstant()));
    }
    @GetMapping("/history/{id}")
    public JsonNode historyItem(@PathVariable long id){
        List<String> rows=db.query("SELECT report::text FROM seo_snapshot WHERE id=?",(r,i)->r.getString(1),id);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"SEO report not found");
        try{return json.readTree(rows.getFirst());}catch(Exception ex){throw new IllegalStateException("Invalid stored report");}
    }
    private JsonNode save(String kind,String subject,Object value){
        JsonNode result=json.valueToTree(value);
        db.update("INSERT INTO seo_snapshot(kind,subject,report) VALUES (?,?,CAST(? AS jsonb))",kind,subject,result.toString());
        return result;
    }
    @PostMapping("/crawl")
    public JsonNode crawl(){return exclusive(()->save("crawl",crawler.host(),crawler.crawl()));}
    @Scheduled(cron="${SARAH_SEO_CRON:0 0 7 * * *}",zone="Europe/Paris")
    public void daily(){
        if(!env.getProperty("SARAH_SEO_SCHEDULE_ENABLED",Boolean.class,false)||!busy.compareAndSet(false,true))return;
        try{
            save("crawl",crawler.host(),crawler.crawl());
            db.update("DELETE FROM seo_snapshot WHERE created_at < now() - interval '90 days'");
        }finally{busy.set(false);}
    }
    @PostMapping("/{kind:backlinks|summary|keywords|ranked|speed|geo}")
    public JsonNode analyze(@PathVariable String kind,@RequestBody Request request){
        return exclusive(()->{
            String target=request.target()==null||request.target().isBlank()?crawler.host():request.target().strip();
            if(!target.matches("(?i)[a-z0-9](?:[a-z0-9.-]{0,250}[a-z0-9])?\\.[a-z]{2,63}")||target.contains(".."))
                throw new IllegalArgumentException("Expected a public domain name");
            return switch(kind){
                case "backlinks" -> save(kind,target,providers.backlinks(target));
                case "summary" -> save(kind,target,providers.summary(target));
                case "ranked" -> save(kind,target,providers.ranked(target));
                case "keywords" -> {String k=bounded(request.keyword(),160);yield save(kind,k,providers.keywords(k));}
                case "speed" -> {
                    URI u=crawler.normalize(bounded(request.url(),1000),URI.create("https://"+crawler.host()));
                    if(u==null)throw new IllegalArgumentException("URL must belong to configured website");
                    String s=request.strategy();if(!Set.of("mobile","desktop").contains(s))throw new IllegalArgumentException("Invalid strategy");
                    yield save(kind,u+" "+s,providers.speed(u.toString(),s));
                }
                case "geo" -> {String q=bounded(request.query(),300);String p=bounded(request.provider(),30);yield save(kind,p+": "+q,providers.geo(p,q,crawler.host()));}
                default -> throw new IllegalArgumentException("Unknown report");
            };
        });
    }
    private String bounded(String s,int n){if(s==null||s.isBlank()||s.length()>n)throw new IllegalArgumentException("Invalid input");return s.strip();}
    private JsonNode exclusive(java.util.function.Supplier<JsonNode> action){
        if(!busy.compareAndSet(false,true))throw new ResponseStatusException(HttpStatus.CONFLICT,"SEO run already active");
        try{return action.get();}
        catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid SEO input");}
        catch(RuntimeException e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"SEO report failed; check provider configuration, quota or connectivity");}
        finally{busy.set(false);}
    }
}
