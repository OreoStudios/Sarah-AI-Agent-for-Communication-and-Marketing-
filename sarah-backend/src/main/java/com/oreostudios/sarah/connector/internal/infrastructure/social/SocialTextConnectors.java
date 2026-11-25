package com.oreostudios.sarah.connector.internal.infrastructure.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oreostudios.sarah.connector.api.*;
import com.oreostudios.sarah.connector.internal.domain.*;
import com.oreostudios.sarah.connector.internal.infrastructure.OutboundHttpGuard;
import com.oreostudios.sarah.shared.Platform;
import com.oreostudios.sarah.shared.PolicyViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Configuration
public class SocialTextConnectors {
    @Bean
    @ConditionalOnProperty(name="sarah.social.linkedin.enabled", havingValue="true")
    SocialConnector linkedinText(ObjectMapper json, OutboundHttpGuard guard,
                                 @Value("${SARAH_LINKEDIN_API_VERSION:}") String version) {
        if (!version.matches("[0-9]{6}")) throw new IllegalStateException("Set SARAH_LINKEDIN_API_VERSION (YYYYMM)");
        return new TextConnector(Platform.LINKEDIN,json,guard,version);
    }
    @Bean
    @ConditionalOnProperty(name="sarah.social.facebook.enabled", havingValue="true")
    SocialConnector facebookText(ObjectMapper json, OutboundHttpGuard guard,
                                 @Value("${SARAH_FACEBOOK_API_VERSION:}") String version) {
        if (!version.matches("v[0-9]+\\.[0-9]+")) throw new IllegalStateException("Set SARAH_FACEBOOK_API_VERSION (vNN.N)");
        return new TextConnector(Platform.FACEBOOK,json,guard,version);
    }
    static final class TextConnector implements SocialConnector {
        private final Platform platform;
        private final ObjectMapper json;
        private final OutboundHttpGuard guard;
        private final String version;
        private final RestClient client;
        TextConnector(Platform platform, ObjectMapper json, OutboundHttpGuard guard, String version) {
            this.platform=platform; this.json=json; this.guard=guard; this.version=version;
            var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
                    .followRedirects(HttpClient.Redirect.NEVER).build());
            factory.setReadTimeout(Duration.ofSeconds(25));
            client=RestClient.builder().requestFactory(factory).build();
        }
        @Override public Platform platform(){return platform;}
        @Override public boolean live(){return true;}
        @Override public Set<Capability> capabilities(){return Set.of(Capability.PREPARE_CONTENT,Capability.PUBLISH,Capability.SCHEDULE);}
        private String target(ConnectorContext ctx) {
            try {
                String id=json.readTree(ctx.config()).path(platform==Platform.LINKEDIN?"author":"pageId").asText("");
                if (platform==Platform.LINKEDIN && !id.matches("urn:li:(person|organization):[a-zA-Z0-9_-]+"))
                    throw new IllegalArgumentException();
                if (platform==Platform.FACEBOOK && !id.matches("[0-9]{1,32}"))
                    throw new IllegalArgumentException();
                return id;
            } catch(Exception e){throw new PolicyViolationException("Invalid connector target configuration");}
        }
        @Override public void validateCredentials(ConnectorContext ctx) {
            if(ctx.secret()==null || ctx.secret().isBlank()) throw new PolicyViolationException("Access token missing");
            target(ctx);
            guard.validate(platform==Platform.LINKEDIN?"https://api.linkedin.com/rest/posts":"https://graph.facebook.com/"+version+"/me");
            // This checks configuration, not the provider's permission to publish. Only a successful post confirms that.
        }
        @Override public String prepareContent(String raw,String language){
            if(raw==null || raw.isBlank() || raw.length()>platform.maxChars())
                throw new PolicyViolationException("Content exceeds platform limits or is empty");
            return raw.strip();
        }
        @Override public PublishResult publish(ConnectorContext ctx,PublishCommand request){
            try {
                validateCredentials(ctx);
                String id=target(ctx), content=prepareContent(request.content(),request.language());
                if(platform==Platform.LINKEDIN){
                    var body=Map.of("author",id,"commentary",content,"visibility","PUBLIC",
                            "distribution",Map.of("feedDistribution","MAIN_FEED","targetEntities",java.util.List.of(),"thirdPartyDistributionChannels",java.util.List.of()),
                            "lifecycleState","PUBLISHED","isReshareDisabledByAuthor",false);
                    var response=client.post().uri(guard.validate("https://api.linkedin.com/rest/posts"))
                            .headers(h->{h.setBearerAuth(ctx.secret());h.set("LinkedIn-Version",version);h.set("X-Restli-Protocol-Version","2.0.0");})
                            .body(body).retrieve().toBodilessEntity();
                    String external=response.getHeaders().getFirst("x-restli-id");
                    return external==null || external.isBlank()?PublishResult.permanentFailure("LinkedIn response lacks a post ID; reconcile manually"):
                            PublishResult.ok(external,null,"LinkedIn accepted post");
                }
                JsonNode response=client.post().uri(guard.validate("https://graph.facebook.com/"+version+"/"+id+"/feed"))
                        .headers(h->h.setBearerAuth(ctx.secret())).body(Map.of("message",content,"published",true))
                        .retrieve().body(JsonNode.class);
                String external=response==null?"":response.path("id").asText("");
                return external.isBlank()?PublishResult.permanentFailure("Facebook response lacks a post ID; reconcile manually"):
                        PublishResult.ok(external,null,"Facebook accepted post");
            } catch(org.springframework.web.client.RestClientResponseException e){
                return PublishResult.permanentFailure(platform.displayName()+" HTTP "+e.getStatusCode().value()+"; verify permissions and reconcile before retry");
            } catch(RuntimeException e){
                return PublishResult.permanentFailure(platform.displayName()+" publish not confirmed; reconcile manually before retry");
            }
        }
        @Override public PublicationStatusInfo getPublicationStatus(ConnectorContext ctx,String id){return new PublicationStatusInfo(id,"UNKNOWN","Status lookup not implemented");}
        @Override public AnalyticsSnapshot fetchAnalytics(ConnectorContext ctx,String id){return AnalyticsSnapshot.unavailable("Analytics not implemented");}
    }
}
