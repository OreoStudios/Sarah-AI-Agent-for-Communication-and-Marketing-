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
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Set;

@Configuration
public class ReviewedSocialConnectors {
    @Bean
    @ConditionalOnProperty(name="sarah.social.instagram.enabled",havingValue="true")
    SocialConnector instagramPhoto(ObjectMapper json, OutboundHttpGuard guard,
            @Value("${SARAH_INSTAGRAM_API_VERSION:}") String version) {
        if (!version.matches("v[0-9]+\\.[0-9]+")) throw new IllegalStateException("Set SARAH_INSTAGRAM_API_VERSION");
        return new Instagram(json,guard,http(),version);
    }
    @Bean
    @ConditionalOnProperty(name="sarah.social.reddit.enabled",havingValue="true")
    SocialConnector redditText(ObjectMapper json, OutboundHttpGuard guard,
            @Value("${SARAH_REDDIT_USER_AGENT:}") String userAgent) {
        if (userAgent.isBlank() || userAgent.length()>200 || userAgent.contains("\n") || userAgent.contains("\r"))
            throw new IllegalStateException("Set a descriptive SARAH_REDDIT_USER_AGENT including the app and responsible account");
        return new Reddit(json,guard,http(),userAgent);
    }
    private static RestClient http() {
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        return RestClient.builder().requestFactory(factory).build();
    }
    abstract static class Base implements SocialConnector {
        final ObjectMapper json; final OutboundHttpGuard guard; final RestClient client;
        Base(ObjectMapper json,OutboundHttpGuard guard,RestClient client){this.json=json;this.guard=guard;this.client=client;}
        @Override public boolean live(){return true;}
        @Override public Set<Capability> capabilities(){return Set.of(Capability.VALIDATE_CREDENTIALS,Capability.PREPARE_CONTENT,Capability.PUBLISH,Capability.SCHEDULE);}
        JsonNode config(ConnectorContext ctx){
            try{
                if(ctx.secret()==null || ctx.secret().isBlank())throw new IllegalArgumentException();
                JsonNode node=json.readTree(ctx.config());
                if(node==null || !node.isObject())throw new IllegalArgumentException();
                return node;
            }catch(Exception e){throw new PolicyViolationException("Channel configuration or access token missing");}
        }
        @Override public String prepareContent(String raw,String language){
            if(raw==null || raw.isBlank() || raw.codePointCount(0,raw.length())>platform().maxChars())
                throw new PolicyViolationException("Empty content or platform length limit exceeded");
            return raw.strip();
        }
        void reviewed(PublishCommand request){
            if(request.idempotencyKey()==null || !request.idempotencyKey().startsWith("reviewed-social:"))
                throw new PolicyViolationException("Owner review required before publication");
        }
        @Override public PublicationStatusInfo getPublicationStatus(ConnectorContext ctx,String id){
            return new PublicationStatusInfo(id,"UNKNOWN","Status lookup not implemented");
        }
        @Override public AnalyticsSnapshot fetchAnalytics(ConnectorContext ctx,String id){
            return AnalyticsSnapshot.unavailable("Engagement analytics not implemented");
        }
        PublishResult failure(){return PublishResult.permanentFailure(platform().displayName()+" publication not confirmed; check provider and reconcile manually before any retry");}
    }
    static final class Instagram extends Base {
        final String version;
        Instagram(ObjectMapper json,OutboundHttpGuard guard,RestClient client,String version){super(json,guard,client);this.version=version;}
        @Override public Platform platform(){return Platform.INSTAGRAM;}
        String account(JsonNode config){
            String id=config.path("accountId").asText("");
            if(!id.matches("[0-9]{1,32}"))throw new PolicyViolationException("Instagram professional account ID required");
            return id;
        }
        String image(JsonNode config){
            String url=config.path("imageUrl").asText("");
            if(url.length()>2000)throw new PolicyViolationException("Image URL too long");
            URI u=guard.validate(url);
            if(u.getUserInfo()!=null||u.getFragment()!=null||u.getQuery()!=null||(u.getPort()!=-1&&u.getPort()!=443))
                throw new PolicyViolationException("Use an approved, public HTTPS image URL without credentials or query");
            return u.toASCIIString();
        }
        URI endpoint(String path){return guard.validate("https://graph.facebook.com/"+version+"/"+path);}
        @Override public void validateCredentials(ConnectorContext ctx){
            try{
                JsonNode cfg=config(ctx);String id=account(cfg);image(cfg);
                JsonNode me=client.get().uri(endpoint(id+"?fields=id,username")).headers(h->h.setBearerAuth(ctx.secret()))
                        .retrieve().body(JsonNode.class);
                if(me==null||!id.equals(me.path("id").asText()))throw new IllegalStateException();
            }catch(RuntimeException e){throw new PolicyViolationException("Instagram account, media URL or access token could not be validated");}
        }
        @Override public PublishResult publish(ConnectorContext ctx,PublishCommand request){
            try{
                reviewed(request);
                JsonNode cfg=config(ctx);String id=account(cfg),url=image(cfg);
                var form=new LinkedMultiValueMap<String,String>();
                form.add("image_url",url);form.add("caption",prepareContent(request.content(),request.language()));
                JsonNode container=client.post().uri(endpoint(id+"/media")).headers(h->h.setBearerAuth(ctx.secret()))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(JsonNode.class);
                String creation=container==null?"":container.path("id").asText("");
                if(!creation.matches("[0-9]{1,32}"))return failure();
                JsonNode status=client.get().uri(endpoint(creation+"?fields=status_code"))
                        .headers(h->h.setBearerAuth(ctx.secret())).retrieve().body(JsonNode.class);
                if(status==null||!"FINISHED".equals(status.path("status_code").asText()))
                    return PublishResult.permanentFailure("Instagram media container not ready; no publish requested. Review media and create a new approved publication.");
                var publishForm=new LinkedMultiValueMap<String,String>();publishForm.add("creation_id",creation);
                JsonNode post=client.post().uri(endpoint(id+"/media_publish")).headers(h->h.setBearerAuth(ctx.secret()))
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(publishForm).retrieve().body(JsonNode.class);
                String external=post==null?"":post.path("id").asText("");
                if(!external.matches("[0-9]{1,32}"))return failure();
                return PublishResult.ok(external,null,"Instagram confirmed media publication");
            }catch(RuntimeException e){return failure();}
        }
    }
    static final class Reddit extends Base {
        final String userAgent;
        Reddit(ObjectMapper json,OutboundHttpGuard guard,RestClient client,String userAgent){super(json,guard,client);this.userAgent=userAgent;}
        @Override public Platform platform(){return Platform.REDDIT;}
        String subreddit(JsonNode cfg){
            String sr=cfg.path("subreddit").asText("");
            if(!sr.matches("[A-Za-z0-9_]{2,21}"))throw new PolicyViolationException("Configure one authorized subreddit");
            return sr;
        }
        String title(JsonNode cfg){
            String title=cfg.path("title").asText("").strip();
            if(title.isBlank()||title.length()>300||title.contains("\n")||title.contains("\r"))
                throw new PolicyViolationException("Provide an approved Reddit title (1-300 characters)");
            return title;
        }
        URI endpoint(String path){return guard.validate("https://oauth.reddit.com/"+path);}
        @Override public void validateCredentials(ConnectorContext ctx){
            try{
                JsonNode cfg=config(ctx);subreddit(cfg);title(cfg);
                JsonNode me=client.get().uri(endpoint("api/v1/me")).headers(h->{h.setBearerAuth(ctx.secret());h.set("User-Agent",userAgent);})
                        .retrieve().body(JsonNode.class);
                if(me==null||me.path("name").asText("").isBlank())throw new IllegalStateException();
            }catch(RuntimeException e){throw new PolicyViolationException("Reddit user token or channel configuration could not be validated");}
        }
        @Override public PublishResult publish(ConnectorContext ctx,PublishCommand request){
            try{
                reviewed(request);JsonNode cfg=config(ctx);String sr=subreddit(cfg);
                var form=new LinkedMultiValueMap<String,String>();
                form.add("api_type","json");form.add("kind","self");form.add("sr",sr);form.add("title",title(cfg));
                form.add("text",prepareContent(request.content(),request.language())+"\n\nDisclosure: posted by SARAH, Oreo Studios' automated assistant.");
                form.add("sendreplies","false");
                JsonNode result=client.post().uri(endpoint("api/submit")).headers(h->{h.setBearerAuth(ctx.secret());h.set("User-Agent",userAgent);})
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(JsonNode.class);
                if(result==null)return failure();
                JsonNode envelope=result.path("json"),errors=envelope.path("errors"),data=envelope.path("data");
                if(!errors.isArray()||!errors.isEmpty())return PublishResult.permanentFailure("Reddit rejected the post or returned an unconfirmed response; check subreddit rules and permissions");
                String id=data.path("name").asText("");
                if(id.isBlank()&&data.path("id").asText("").matches("[a-z0-9]+"))id="t3_"+data.path("id").asText();
                if(!id.matches("t3_[a-z0-9]+"))return failure();
                return PublishResult.ok(id,"https://www.reddit.com/comments/"+id.substring(3),"Reddit confirmed text post");
            }catch(RuntimeException e){return failure();}
        }
    }
}
