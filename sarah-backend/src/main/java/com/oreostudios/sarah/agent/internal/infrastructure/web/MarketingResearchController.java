package com.oreostudios.sarah.agent.internal.infrastructure.web;

import com.oreostudios.sarah.brand.api.BrandFacade;
import com.oreostudios.sarah.llm.api.LlmClient;
import com.oreostudios.sarah.llm.api.PromptSanitizer;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/marketing/research")
@PreAuthorize("hasRole('OWNER')")
public class MarketingResearchController {
    private final LlmClient llm;
    private final BrandFacade brand;
    private final PromptSanitizer sanitizer;
    public MarketingResearchController(LlmClient llm,BrandFacade brand,PromptSanitizer sanitizer){
        this.llm=llm;this.brand=brand;this.sanitizer=sanitizer;
    }
    public record Brief(String topic,String audience,String language,String evidence){}
    public record Draft(String type,String content,String provider,boolean live,String caveat){}
    @PostMapping("/seo")
    public Draft seo(@RequestBody Brief b){
        return generate("SEO_ARTICLE",b,"Write a draft with title, suggested slug, meta description, search intent, H2/H3 outline, 700-1000 word article, internal-link suggestions, CTA and fact-check checklist. Do not invent keyword volumes, rankings, backlinks, or testimonials.");
    }
    @PostMapping("/competitor")
    public Draft competitor(@RequestBody Brief b){
        return generate("COMPETITOR_ANALYSIS",b,"Analyze only supplied competitor evidence. Separate observations, assumptions, gaps and recommendations. Compare positioning and service offerings; cite supplied source identifiers, flag unknowns, and do not invent traffic, pricing or market share.");
    }
    private Draft generate(String type,Brief b,String task){
        String topic=bounded(b.topic(),200,true), audience=bounded(b.audience(),200,false);
        String language=bounded(b.language(),32,false), evidence=bounded(b.evidence(),10000,false);
        if(!llm.isLive()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Configure a live AI provider for research");
        var profile=brand.active();
        String system=sanitizer.guardClause()+"\nYou are a marketing research specialist. "+task+
                "\nNever execute or publish anything. Use supplied evidence only for factual competitor or performance claims.";
        String input=sanitizer.wrapUntrusted("brand",profile.companyName()+"\n"+profile.description()+"\n"+profile.services())
                +"\n"+sanitizer.wrapUntrusted("brief",topic+"\nAudience: "+audience+"\nLanguage: "+language)
                +"\n"+sanitizer.wrapUntrusted("evidence",evidence);
        return new Draft(type,llm.generate(system,input),llm.providerName(),true,"Draft only; review factual claims and sources before use. No external search performed.");
    }
    private String bounded(String value,int max,boolean required){
        if(value==null || value.isBlank()){
            if(required)throw new IllegalArgumentException("Topic is required");
            return "";
        }
        if(value.length()>max)throw new IllegalArgumentException("Input exceeds limit");
        return value.strip();
    }
}
