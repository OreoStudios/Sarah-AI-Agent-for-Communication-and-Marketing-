package com.oreostudios.sarah.seo.internal;

import com.fasterxml.jackson.databind.*;
import com.oreostudios.sarah.brand.api.BrandFacade;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.Instant;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@RestController
@RequestMapping("/api/v1/seo")
@PreAuthorize("hasRole('OWNER')")
public class SeoController {
    private final SiteCrawler crawler;
    private final BrandFacade brand;
    private final SeoProviders providers;
    private final JdbcTemplate db;
    private final ObjectMapper json;
    private final Environment env;
    private final AtomicBoolean busy=new AtomicBoolean();
    private final SeoJobs jobs;
    private final TransactionTemplate transactions;
    public SeoController(SiteCrawler crawler,SeoProviders providers,JdbcTemplate db,ObjectMapper json,Environment env,BrandFacade brand,SeoJobs jobs,PlatformTransactionManager manager){
        this.crawler=crawler;this.providers=providers;this.db=db;this.json=json;this.env=env;this.brand=brand;this.jobs=jobs;this.transactions=new TransactionTemplate(manager);
    }
    record Request(String target,String keyword,String provider,String query,String url,String strategy,boolean confirmSubmission){}
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
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> seoError(ResponseStatusException ex){
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("detail",
            Objects.requireNonNullElse(ex.getReason(),"SEO request failed")));
    }
    private UUID workspaceId(){
        return UUID.fromString(db.queryForObject("SELECT current_setting('sarah.workspace_id')",String.class));
    }
    private long snapshot(String kind,String subject,Object value){
        return Objects.requireNonNull(db.queryForObject(
            "INSERT INTO seo_snapshot(kind,subject,report) VALUES (?,?,CAST(? AS jsonb)) RETURNING id",
            Long.class,kind,subject,json.valueToTree(value).toString()));
    }
    private long storeJob(UUID workspace,String kind,String subject,Object value,Map<String,Object> actionPlan){
        return Objects.requireNonNull(transactions.execute(status -> {
            db.execute("SET LOCAL ROLE sarah_workspace_runtime");
            db.queryForObject("SELECT set_config('sarah.workspace_id', ?, true)",String.class,workspace.toString());
            if(actionPlan!=null) snapshot("action-plan",subject,actionPlan);
            return snapshot(kind,subject,value);
        }));
    }
    @PostMapping("/jobs/crawl")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SeoJobs.View startCrawl(){
        UUID workspace=workspaceId();
        SiteCrawler site=companyCrawler();
        return jobs.start(workspace,"crawl",progress -> {
            var report=site.crawl(progress);
            return storeJob(workspace,"crawl",site.host(),report,plan(report));
        });
    }
    @PostMapping("/jobs/geo")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SeoJobs.View startGeo(@RequestBody Request request){
        UUID workspace=workspaceId();
        SiteCrawler site=companyCrawler();
        String q=bounded(request.query(),300), p=bounded(request.provider(),30);
        if(!Set.of("openai","anthropic","gemini").contains(p)) throw new IllegalArgumentException("Unknown AI provider");
        String company=brand.active().companyName();
        return jobs.start(workspace,"geo",progress -> {
            progress.accept(new SeoJobs.Progress("Waiting for "+p+" web search",0,0,0));
            var report=providers.geo(p,q,site.host(),company);
            progress.accept(new SeoJobs.Progress("Saving report",0,0,0));
            return storeJob(workspace,"geo",p+": "+q,report,null);
        });
    }
    @GetMapping("/jobs/{id}")
    public SeoJobs.View job(@PathVariable UUID id){return jobs.get(workspaceId(),id);}

    @PostMapping("/crawl")
    public JsonNode crawl(){
        SiteCrawler crawler = companyCrawler();
        return exclusive(()->{
        var report=crawler.crawl();save("action-plan",crawler.host(),plan(report));return save("crawl",crawler.host(),report);
    });}
    private void attempt(String kind,String subject,java.util.function.Supplier<Object> action){
        try{save(kind,subject,action.get());}
        catch(RuntimeException ex){save("provider-error",kind+": "+subject,Map.of("status","UNAVAILABLE","note","Check credentials, quota or connectivity"));}
    }
    private Map<String,Object> plan(SiteCrawler.Report report){
        Map<String,Long> counts=new TreeMap<>();
        for(var page:report.results())for(String issue:page.issues())counts.merge(issue.startsWith("BROKEN_LINK:")?"BROKEN_LINK":issue,1L,Long::sum);
        List<Map<String,Object>> actions=new ArrayList<>();
        for(var entry:counts.entrySet()){
            String issue=entry.getKey();
            String priority=issue.startsWith("HTTP_")||Set.of("BROKEN_LINK","FETCH_FAILED","NOINDEX","ROBOTS_BLOCKED").contains(issue)?"HIGH":"REVIEW";
            List<String> urls=report.results().stream().filter(p->p.issues().stream().anyMatch(i->i.equals(issue)||i.startsWith(issue+":"))).map(SiteCrawler.Page::url).limit(50).toList();
            actions.add(Map.of("issue",issue,"affectedPagesOrLinks",entry.getValue(),"priority",priority,"sampleUrls",urls));
        }
        actions.sort(Comparator.comparing(a->a.get("priority").toString()));
        return Map.of("complete",report.complete(),"actions",actions,"notes",report.notes(),"guidance","Review intentional noindex, robots and redirect rules before modifying the site. No changes applied.");
    }
    @Scheduled(cron="${SARAH_SEO_CRON:0 0 7 * * *}",zone="Europe/Paris")
    public void daily(){
        if(!env.getProperty("SARAH_WORKSPACE_LEGACY_SCHEDULERS",Boolean.class,false))return;
        if(!env.getProperty("SARAH_SEO_SCHEDULE_ENABLED",Boolean.class,false)||!busy.compareAndSet(false,true))return;
        try{
            var report=crawler.crawl();
            save("crawl",crawler.host(),report);
            save("action-plan",crawler.host(),plan(report));
            if(env.getProperty("SARAH_SEO_PAID_DAILY_ENABLED",Boolean.class,false)){
                // One request of each type; no automatic retries. Failures are separate from successful observations.
                attempt("summary",crawler.host(),()->providers.summary(crawler.host()));
                attempt("backlink-trend",crawler.host(),()->providers.backlinkTrend(crawler.host()));
                attempt("ranked",crawler.host(),()->providers.ranked(crawler.host()));
                String q=env.getProperty("SARAH_SEO_DAILY_GEO_QUERY","Quelle agence web choisir à Strasbourg ?");
                for(String provider:List.of("openai","anthropic","gemini"))
                    attempt("geo",provider+": "+q,()->providers.geo(provider,q,crawler.host()));
            }
            db.update("DELETE FROM seo_snapshot WHERE created_at < now() - interval '90 days'");
        }finally{busy.set(false);}
    }
    @PostMapping("/{kind:backlinks|summary|keywords|ranked|speed|geo|backlink-trend|referring-domains|lost-links|bing|indexnow}")
    public JsonNode analyze(@PathVariable String kind,@RequestBody Request request){
        SiteCrawler crawler = companyCrawler();
        return exclusive(()->{
            String target=request.target()==null||request.target().isBlank()?crawler.host():request.target().strip();
            if(!target.matches("(?i)[a-z0-9](?:[a-z0-9.-]{0,250}[a-z0-9])?\\.[a-z]{2,63}")||target.contains(".."))
                throw new IllegalArgumentException("Expected a public domain name");
            return switch(kind){
                case "backlinks" -> save(kind,target,providers.backlinks(target));
                case "summary" -> save(kind,target,providers.summary(target));
                case "backlink-trend" -> save(kind,target,providers.backlinkTrend(target));
                case "referring-domains" -> save(kind,target,providers.referringDomains(target));
                case "lost-links" -> save(kind,target,providers.lostLinks(target));
                case "bing" -> save(kind,crawler.host(),providers.bing("https://"+crawler.host()+"/"));
                case "indexnow" -> {
                    if(!request.confirmSubmission())throw new IllegalArgumentException("Submission requires confirmation");
                    URI u=crawler.normalize(bounded(request.url(),1000),URI.create("https://"+crawler.host()));
                    if(u==null)throw new IllegalArgumentException("URL must belong to configured website");
                    yield save(kind,u.toString(),providers.indexNow(crawler.host(),u.toString()));
                }
                case "ranked" -> save(kind,target,providers.ranked(target));
                case "keywords" -> {String k=bounded(request.keyword(),160);yield save(kind,k,providers.keywords(k));}
                case "speed" -> {
                    URI u=crawler.normalize(bounded(request.url(),1000),URI.create("https://"+crawler.host()));
                    if(u==null)throw new IllegalArgumentException("URL must belong to configured website");
                    String s=request.strategy();if(!Set.of("mobile","desktop").contains(s))throw new IllegalArgumentException("Invalid strategy");
                    yield save(kind,u+" "+s,providers.speed(u.toString(),s));
                }
                case "geo" -> {String q=bounded(request.query(),300);String p=bounded(request.provider(),30);yield save(kind,p+": "+q,providers.geo(p,q,crawler.host(),brand.active().companyName()));}
                default -> throw new IllegalArgumentException("Unknown report");
            };
        });
    }
    /**
     * Run manual SEO actions against the selected company, not the server-wide
     * SARAH_SEO_ORIGIN default. SiteCrawler performs public-IP and robots checks.
     */
    private SiteCrawler companyCrawler() {
        String website = brand.active().website();
        if (website == null || website.isBlank())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Set your company website in Brand settings first");
        try {
            URI uri = URI.create(website);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null ||
                uri.getUserInfo() != null || uri.getPort() != -1)
                throw new IllegalArgumentException("Expected HTTPS website");
            int max = env.getProperty("SARAH_SEO_MAX_PAGES", Integer.class, 100);
            return new SiteCrawler("https://" + uri.getHost(), max);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Configure a valid HTTPS company website");
        }
    }
    private String bounded(String s,int n){if(s==null||s.isBlank()||s.length()>n)throw new IllegalArgumentException("Invalid input");return s.strip();}
    private JsonNode exclusive(java.util.function.Supplier<JsonNode> action){
        if(!busy.compareAndSet(false,true))throw new ResponseStatusException(HttpStatus.CONFLICT,"SEO run already active");
        try{return action.get();}
        catch(ResponseStatusException e){throw e;}
        catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid SEO input");}
        catch(RuntimeException e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,SeoJobs.safeError("provider",e));}
        finally{busy.set(false);}
    }
}
