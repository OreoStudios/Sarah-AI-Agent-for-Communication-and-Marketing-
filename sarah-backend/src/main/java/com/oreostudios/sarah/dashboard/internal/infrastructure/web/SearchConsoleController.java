package com.oreostudios.sarah.dashboard.internal.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriUtils;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/dashboard/search-console")
@PreAuthorize("hasRole('OWNER')")
@ConditionalOnProperty(name="sarah.search-console.enabled",havingValue="true")
public class SearchConsoleController {
    private final RestClient client;
    private final String property,clientId,clientSecret,refreshToken;
    private String token;
    private Instant expires=Instant.EPOCH;
    public SearchConsoleController(@Value("${SARAH_GSC_PROPERTY:}") String property,
            @Value("${SARAH_GSC_CLIENT_ID:}") String clientId,
            @Value("${SARAH_GSC_CLIENT_SECRET:}") String clientSecret,
            @Value("${SARAH_GSC_REFRESH_TOKEN:}") String refreshToken) {
        if(property.isBlank() || clientId.isBlank() || clientSecret.isBlank() || refreshToken.isBlank())
            throw new IllegalStateException("Configure Search Console property and OAuth credentials");
        this.property=property;this.clientId=clientId;this.clientSecret=clientSecret;this.refreshToken=refreshToken;
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(25));
        client=RestClient.builder().requestFactory(factory).build();
    }
    private synchronized String accessToken(){
        if(token!=null && expires.isAfter(Instant.now().plusSeconds(60))) return token;
        var form=new LinkedMultiValueMap<String,String>();
        form.add("client_id",clientId);form.add("client_secret",clientSecret);
        form.add("refresh_token",refreshToken);form.add("grant_type","refresh_token");
        JsonNode data=client.post().uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(JsonNode.class);
        if(data==null || data.path("access_token").asText("").isBlank())
            throw new IllegalStateException("Search Console OAuth returned no token");
        token=data.path("access_token").asText();
        expires=Instant.now().plusSeconds(Math.max(0,data.path("expires_in").asLong(0)));
        return token;
    }
    public record Row(String key,double clicks,double impressions,double ctr,double position){}
    public record Report(String property,LocalDate start,LocalDate end,String dimension,List<Row> rows,String note){}
    @GetMapping
    public Report query(@RequestParam(defaultValue="28") int days,@RequestParam(defaultValue="query") String dimension){
        if(days<1||days>90||!Set.of("query","page","country","device").contains(dimension))
            throw new IllegalArgumentException("Invalid reporting window or dimension");
        LocalDate end=LocalDate.now(ZoneId.of("America/Los_Angeles")).minusDays(3),start=end.minusDays(days-1);
        try{
            URI endpoint=URI.create("https://www.googleapis.com/webmasters/v3/sites/"+
                    UriUtils.encodePathSegment(property,StandardCharsets.UTF_8)+"/searchAnalytics/query");
            JsonNode data=client.post().uri(endpoint).headers(h->h.setBearerAuth(accessToken()))
                    .body(Map.of("startDate",start.toString(),"endDate",end.toString(),
                            "dimensions",List.of(dimension),"rowLimit",100,"type","web","dataState","final"))
                    .retrieve().body(JsonNode.class);
            if(data==null) throw new IllegalStateException("No Search Console response");
            List<Row> rows=new ArrayList<>();
            for(JsonNode item:data.path("rows")) rows.add(new Row(item.path("keys").path(0).asText(),
                    item.path("clicks").asDouble(),item.path("impressions").asDouble(),
                    item.path("ctr").asDouble(),item.path("position").asDouble()));
            return new Report(property,start,end,dimension,List.copyOf(rows),"Top 100 final rows; Search Console may omit some queries");
        }catch(RuntimeException e){
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Search Console request failed; check OAuth access and property");
        }
    }
}
