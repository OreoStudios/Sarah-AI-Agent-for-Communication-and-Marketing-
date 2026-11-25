package com.oreostudios.sarah.seo.internal;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;

@Component
public class SeoProviders {
    private final Environment env;
    private final RestClient client;
    SeoProviders(Environment env){
        this.env=env;
        var f=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build());
        f.setReadTimeout(Duration.ofSeconds(90));client=RestClient.builder().requestFactory(f).build();
    }
    String config(String k,String fallback){return env.getProperty(k,fallback);}
    String required(String k){String v=config(k,"");if(v.isBlank())throw new IllegalStateException("Configure "+k);return v;}
    private JsonNode data(String path,Map<String,Object> payload){
        JsonNode n=client.post().uri("https://api.dataforseo.com/v3/"+path)
          .headers(h->h.setBasicAuth(required("SARAH_DATAFORSEO_LOGIN"),required("SARAH_DATAFORSEO_PASSWORD")))
          .body(List.of(payload)).retrieve().body(JsonNode.class);
        if(n==null||n.path("status_code").asInt()!=20000||n.path("tasks").path(0).path("status_code").asInt()!=20000)
            throw new IllegalStateException("DataForSEO task failed; check account access and quota");
        return n.path("tasks").path(0).path("result");
    }
    JsonNode backlinks(String target){return data("backlinks/backlinks/live",Map.of("target",target,"limit",100,"mode","as_is"));}
    JsonNode backlinkTrend(String target){
        var today=java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        return data("backlinks/timeseries_new_lost_summary/live",Map.of("target",target,"date_from",today.minusDays(30).toString(),"date_to",today.toString(),"group_range","day"));
    }
    JsonNode referringDomains(String target){return data("backlinks/referring_domains/live",Map.of("target",target,"limit",100));}
    JsonNode lostLinks(String target){return data("backlinks/backlinks/live",Map.of("target",target,"limit",100,"mode","as_is","backlinks_status_type","lost"));}
    JsonNode bing(String origin){
        JsonNode n=client.get().uri("https://ssl.bing.com/webmaster/api.svc/json/GetRankAndTrafficStats?siteUrl={site}&apikey={key}",origin,required("SARAH_BING_WEBMASTER_KEY"))
          .retrieve().body(JsonNode.class);
        if(n==null||n.has("ErrorCode"))throw new IllegalStateException("Bing returned no usable traffic report");
        return n;
    }
    JsonNode indexNow(String host,String url){
        String key=required("SARAH_INDEXNOW_KEY");
        if(!key.matches("[A-Za-z0-9-]{8,128}"))throw new IllegalStateException("Invalid IndexNow key");
        var response=client.post().uri("https://api.indexnow.org/indexnow")
          .body(Map.of("host",host,"key",key,"keyLocation","https://"+host+"/"+key+".txt","urlList",List.of(url)))
          .retrieve().toBodilessEntity();
        return new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(Map.of("url",url,"httpStatus",response.getStatusCode().value(),"note","Submission accepted for processing; indexing is not guaranteed."));
    }
    JsonNode summary(String target){return data("backlinks/summary/live",Map.of("target",target));}
    JsonNode keywords(String keyword){return data("dataforseo_labs/google/keyword_suggestions/live",Map.of("keyword",keyword,"location_name","France","language_name","French","limit",100));}
    JsonNode ranked(String target){return data("dataforseo_labs/google/ranked_keywords/live",Map.of("target",target,"location_name","France","language_name","French","limit",100));}
    JsonNode speed(String url,String strategy){
        JsonNode n=client.get().uri("https://www.googleapis.com/pagespeedonline/v5/runPagespeed?url={url}&strategy={strategy}&category=performance&category=seo&category=accessibility&key={key}",url,strategy,required("SARAH_PAGESPEED_KEY"))
          .retrieve().body(JsonNode.class);
        if(n==null||!n.has("lighthouseResult"))throw new IllegalStateException("PageSpeed returned no report");
        var result=new LinkedHashMap<String,JsonNode>();
        result.put("loadingExperience",n.path("loadingExperience"));result.put("originLoadingExperience",n.path("originLoadingExperience"));
        result.put("categories",n.path("lighthouseResult").path("categories"));
        var audits=n.path("lighthouseResult").path("audits");
        for(String key:List.of("largest-contentful-paint","cumulative-layout-shift","total-blocking-time","first-contentful-paint","speed-index","interactive"))
            result.put(key,audits.path(key));
        return new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(result);
    }
    record Geo(String provider,String model,String query,String answer,boolean brandMention,List<String> citationUrls,boolean ownDomainCited,String status){}
    Geo geo(String provider,String query,String host){
        String model;JsonNode n;String prompt="Search the web, then answer this neutral user's question in French. Give concrete recommendations and cite sources. Question: "+query;
        switch(provider){
            case "openai" -> {
                model=config("SARAH_OPENAI_MODEL","gpt-6.2-astra");
                n=client.post().uri("https://api.openai.com/v1/responses").headers(h->h.setBearerAuth(required("OPENAI_API_KEY")))
                  .body(Map.of("model",model,"input",prompt,"tools",List.of(Map.of("type","web_search")),"max_output_tokens",4000,"store",false))
                  .retrieve().body(JsonNode.class);
            }
            case "anthropic" -> {
                model=config("SARAH_LLM_MODEL","claude-opus-5-5");
                n=client.post().uri("https://api.anthropic.com/v1/messages").header("x-api-key",required("ANTHROPIC_API_KEY")).header("anthropic-version","2023-06-01")
                  .body(Map.of("model",model,"max_tokens",4000,"messages",List.of(Map.of("role","user","content",prompt)),
                    "tools",List.of(Map.of("type","web_search_20260318","name","web_search","max_uses",3,"allowed_callers",List.of("direct")))))
                  .retrieve().body(JsonNode.class);
            }
            case "gemini" -> {
                model=config("SARAH_GEMINI_MODEL","gemini-4-carbon");
                n=client.post().uri("https://generativelanguage.googleapis.com/v1beta/interactions")
                  .header("x-goog-api-key",required("GEMINI_API_KEY")).header("Api-Revision","2026-05-20")
                  .body(Map.of("model",model,"input",prompt,"tools",List.of(Map.of("type","google_search")),"store",false))
                  .retrieve().body(JsonNode.class);
            }
            default -> throw new IllegalArgumentException("Unknown provider");
        }
        if(n==null||n.path("status").asText("").matches("failed|incomplete|cancelled|requires_action")||"pause_turn".equals(n.path("stop_reason").asText()))throw new IllegalStateException("Incomplete provider response");
        List<String> texts=new ArrayList<>();Set<String> urls=new LinkedHashSet<>();
        collect(n,texts,urls);
        String answer=String.join("\n",texts);
        if(answer.isBlank())throw new IllegalStateException("No answer returned");
        boolean own=urls.stream().anyMatch(u->{try{String h=java.net.URI.create(u).getHost();return h!=null&&(h.equalsIgnoreCase(host)||h.toLowerCase(Locale.ROOT).endsWith("."+host));}catch(Exception e){return false;}});
        return new Geo(provider,model,query,answer,answer.toLowerCase(Locale.ROOT).matches("(?s).*oreo\\s*studios.*"),List.copyOf(urls),own,urls.isEmpty()?"NO_CITATIONS":"CITATIONS_PRESENT");
    }
    private static void collect(JsonNode node,List<String> texts,Set<String> urls){
        if(node.isArray()){node.forEach(x->collect(x,texts,urls));return;}
        if(!node.isObject())return;
        String type=node.path("type").asText();
        if(Set.of("output_text","text").contains(type)&&node.has("text")&&node.path("text").isTextual()){
            texts.add(node.path("text").asText());
            collect(node.path("annotations"),new ArrayList<>(),urls);collect(node.path("citations"),new ArrayList<>(),urls);return;
        }
        if(Set.of("url_citation","web_search_result_location","url").contains(type)){
            String url=node.path("url").asText(node.path("uri").asText(""));
            if(url.startsWith("https://")||url.startsWith("http://"))urls.add(url);
        }
        for(String field:List.of("output","content","steps","annotations","citations"))collect(node.path(field),texts,urls);
        if(node.has("output_text")&&node.path("output_text").isTextual()&&texts.isEmpty())texts.add(node.path("output_text").asText());
    }
}
