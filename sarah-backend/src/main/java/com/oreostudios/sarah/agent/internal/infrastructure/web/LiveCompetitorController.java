package com.oreostudios.sarah.agent.internal.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.oreostudios.sarah.brand.api.BrandFacade;
import com.oreostudios.sarah.llm.api.*;
import com.oreostudios.sarah.audit.api.AuditFacade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/marketing/competitors")
@PreAuthorize("hasRole('OWNER')")
public class LiveCompetitorController {
    private final LlmClient llm; private final PromptSanitizer sanitizer; private final BrandFacade brand;
    private final AuditFacade audit; private final String key; private final RestClient search;
    @org.springframework.beans.factory.annotation.Autowired
    public LiveCompetitorController(LlmClient llm,PromptSanitizer sanitizer,BrandFacade brand,AuditFacade audit,
            @Value("${SARAH_BRAVE_SEARCH_KEY:}") String key) {
        this(llm,sanitizer,brand,audit,key,client());
    }
    LiveCompetitorController(LlmClient llm,PromptSanitizer sanitizer,BrandFacade brand,AuditFacade audit,String key,RestClient search){
        this.llm=llm;this.sanitizer=sanitizer;this.brand=brand;this.audit=audit;this.key=key;this.search=search;
    }
    private static RestClient client(){
        var f=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build());
        f.setReadTimeout(Duration.ofSeconds(15));return RestClient.builder().baseUrl("https://api.search.brave.com").requestFactory(f).build();
    }
    public record Request(String market,List<String> competitors,String language){}
    public record Evidence(int id,String title,String url,String snippet,String query){}
    public record Report(Instant retrievedAt,List<Evidence> sources,String analysis,String provider,String limitations){}
    @PostMapping
    public Report analyze(@RequestBody Request req,Authentication auth){
        if(key.isBlank()||!llm.isLive())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Configure Brave Search and a live LLM");
        String market=bounded(req.market(),160);
        if(req.competitors()==null||req.competitors().isEmpty()||req.competitors().size()>5)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Supply one to five competitors");
        List<Evidence> sources=new ArrayList<>();Set<String> seen=new HashSet<>();
        try{
            for(String name:req.competitors()){
                String query=bounded(name,100)+" "+market;
                JsonNode body=search.get().uri(b->b.path("/res/v1/web/search").queryParam("q",query).queryParam("count",5).build())
                    .header("X-Subscription-Token",key).retrieve().body(JsonNode.class);
                if(body==null)continue;
                for(JsonNode hit:body.path("web").path("results")){
                    String url=hit.path("url").asText("");if(!publicWebLink(url)||!seen.add(url)||sources.size()>=25)continue;
                    sources.add(new Evidence(sources.size()+1,clip(hit.path("title").asText(""),200),url,
                            clip(hit.path("description").asText(""),800),query));
                }
            }
        }catch(ResponseStatusException e){throw e;}catch(RuntimeException e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Competitor search failed; check search quota and credentials");}
        if(sources.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"No usable competitor evidence returned");
        String evidence=sources.stream().map(e->"["+e.id()+"] "+e.title()+" "+e.url()+" "+e.snippet()).reduce("",(a,b)->a+"\n"+b);
        var profile=brand.active();
        String report=llm.generate(sanitizer.guardClause()+"\nCompare verified search snippets only. Produce positioning, offerings, differentiators, gaps, risks and concrete actions. Cite source numbers in every factual claim. Distinguish hypotheses from observations. Do not infer revenue, traffic, prices, rankings or market share without evidence. Never publish or execute.",
            sanitizer.wrapUntrusted("market",market+" language "+clip(req.language(),20))+"\n"+sanitizer.wrapUntrusted("brand",profile.companyName()+" "+profile.services())+"\n"+sanitizer.wrapUntrusted("search snippets",evidence));
        audit.record(auth.getName(),"COMPETITOR_RESEARCH","marketing",null,"sources="+sources.size());
        return new Report(Instant.now(),List.copyOf(sources),report,llm.providerName(),"Search snippets, not independently verified full pages. Review citations; no traffic or market-share measurement.");
    }
    static boolean publicWebLink(String raw){
        try{URI u=URI.create(raw);return raw.length()<=2000 && ("https".equals(u.getScheme())||"http".equals(u.getScheme()))&&u.getHost()!=null&&u.getUserInfo()==null;}catch(Exception e){return false;}
    }
    static String clip(String s,int n){return s==null?"":s.substring(0,Math.min(n,s.length()));}
    static String bounded(String s,int n){if(s==null||s.isBlank()||s.length()>n)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid research term");return s.strip();}
}
