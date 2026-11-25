package com.oreostudios.sarah.connector.internal.infrastructure.website;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oreostudios.sarah.connector.api.*;
import com.oreostudios.sarah.connector.internal.domain.*;
import com.oreostudios.sarah.connector.internal.infrastructure.OutboundHttpGuard;
import com.oreostudios.sarah.shared.Platform;
import com.oreostudios.sarah.shared.PolicyViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@ConditionalOnProperty(name="sarah.website.wordpress.enabled",havingValue="true")
public class WordPressConnector implements SocialConnector {
    private final RestClient client;
    private final OutboundHttpGuard guard;
    private final ObjectMapper json;
    private final String base;
    public WordPressConnector(OutboundHttpGuard guard,ObjectMapper json,@Value("${SARAH_WORDPRESS_URL:}") String url){
        this.guard=guard;this.json=json;
        URI uri=guard.validate(url);
        if(uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null || (uri.getPort()!=-1 && uri.getPort()!=443))
            throw new IllegalStateException("Configure an HTTPS WordPress site URL without credentials or query");
        base=uri.toString().replaceAll("/+$","")+"/wp-json/wp/v2";
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(25));
        client=RestClient.builder().requestFactory(factory).build();
    }
    @Override public Platform platform(){return Platform.WEBSITE;}
    @Override public boolean live(){return true;}
    @Override public Set<Capability> capabilities(){return Set.of(Capability.VALIDATE_CREDENTIALS,Capability.PREPARE_CONTENT,Capability.PUBLISH,Capability.SCHEDULE);}
    private String user(ConnectorContext ctx){
        try {
            String user=json.readTree(ctx.config()).path("username").asText("");
            if(user.isBlank() || user.contains(":") || ctx.secret()==null || ctx.secret().isBlank()) throw new IllegalArgumentException();
            return user;
        } catch(Exception e){throw new PolicyViolationException("WordPress username and application password are required");}
    }
    @Override public void validateCredentials(ConnectorContext ctx){
        JsonNode me=client.get().uri(guard.validate(base+"/users/me?context=edit"))
                .headers(h->h.setBasicAuth(user(ctx),ctx.secret())).retrieve().body(JsonNode.class);
        if(me==null || !me.path("capabilities").path("publish_posts").asBoolean(false))
            throw new PolicyViolationException("WordPress account lacks publish_posts capability");
    }
    @Override public String prepareContent(String raw,String language){
        if(raw==null || raw.isBlank() || raw.length()>100000) throw new PolicyViolationException("Invalid blog content");
        return raw.strip();
    }
    @Override public PublishResult publish(ConnectorContext ctx,PublishCommand request){
        try{
            String content=prepareContent(request.content(),request.language()), username=user(ctx);
            if(request.idempotencyKey()==null || request.idempotencyKey().isBlank()) return PublishResult.permanentFailure("Missing idempotency key");
            String slug="sarah-"+UUID.nameUUIDFromBytes(request.idempotencyKey().getBytes(StandardCharsets.UTF_8));
            JsonNode existing=client.get().uri(guard.validate(base+"/posts?slug="+slug+"&status=publish"))
                    .headers(h->h.setBasicAuth(username,ctx.secret())).retrieve().body(JsonNode.class);
            if(existing!=null && existing.isArray() && !existing.isEmpty()) return result(existing.get(0));
            String title=request.title()==null?content.lines().findFirst().orElse("Oreo Studios update"):request.title();
            if(title.length()>100) title=title.substring(0,100);
            String html="<p>"+HtmlUtils.htmlEscape(content).replace("\n\n","</p><p>").replace("\n","<br>")+"</p>";
            JsonNode post=client.post().uri(guard.validate(base+"/posts")).headers(h->h.setBasicAuth(username,ctx.secret()))
                    .body(Map.of("title",title,"content",html,"slug",slug,"status","publish")).retrieve().body(JsonNode.class);
            return result(post);
        }catch(RuntimeException e){return PublishResult.permanentFailure("WordPress publication not confirmed; reconcile existing slug before retry");}
    }
    private PublishResult result(JsonNode post){
        if(post==null || !post.path("id").canConvertToLong() || !"publish".equals(post.path("status").asText()))
            return PublishResult.permanentFailure("WordPress did not confirm published status");
        return PublishResult.ok(post.path("id").asText(),post.path("link").asText(null),"WordPress publication confirmed");
    }
    @Override public PublicationStatusInfo getPublicationStatus(ConnectorContext ctx,String id){return new PublicationStatusInfo(id,"UNKNOWN","Status lookup not implemented");}
    @Override public AnalyticsSnapshot fetchAnalytics(ConnectorContext ctx,String id){return AnalyticsSnapshot.unavailable("Analytics not implemented");}
}
