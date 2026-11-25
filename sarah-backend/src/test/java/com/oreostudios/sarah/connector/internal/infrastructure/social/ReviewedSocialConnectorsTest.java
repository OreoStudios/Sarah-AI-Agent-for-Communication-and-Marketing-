package com.oreostudios.sarah.connector.internal.infrastructure.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oreostudios.sarah.connector.api.PublishCommand;
import com.oreostudios.sarah.connector.internal.domain.ConnectorContext;
import com.oreostudios.sarah.connector.internal.infrastructure.OutboundHttpGuard;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ReviewedSocialConnectorsTest {
    private final ObjectMapper json=new ObjectMapper();
    private final OutboundHttpGuard guard=mock(OutboundHttpGuard.class);
    private final RestClient.Builder builder=RestClient.builder();
    private final MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
    private final ConnectorContext ig=new ConnectorContext("ig","{\"accountId\":\"123\",\"imageUrl\":\"https://example.com/photo.jpg\"}","token");
    private final ConnectorContext rd=new ConnectorContext("rd","{\"subreddit\":\"OreoStudios\",\"title\":\"Studio news\"}","token");
    private final PublishCommand approved=new PublishCommand("Our studio update",null,"reviewed-social:123",null);
    ReviewedSocialConnectorsTest(){when(guard.validate(anyString())).thenAnswer(i->URI.create(i.getArgument(0)));}
    @Test void instagramPublishesOnlyAfterFinishedContainer(){
        var connector=new ReviewedSocialConnectors.Instagram(json,guard,builder.build(),"v27.0");
        server.expect(requestTo("https://graph.facebook.com/v27.0/123/media"))
            .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization","Bearer token"))
            .andRespond(withSuccess("{\"id\":\"456\"}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://graph.facebook.com/v27.0/456?fields=status_code"))
            .andRespond(withSuccess("{\"status_code\":\"FINISHED\"}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://graph.facebook.com/v27.0/123/media_publish"))
            .andExpect(method(HttpMethod.POST)).andRespond(withSuccess("{\"id\":\"789\"}",MediaType.APPLICATION_JSON));
        var result=connector.publish(ig,approved);
        assertTrue(result.success());assertEquals("789",result.externalId());server.verify();
    }
    @Test void instagramDoesNotPublishUnreadyContainer(){
        var connector=new ReviewedSocialConnectors.Instagram(json,guard,builder.build(),"v27.0");
        server.expect(requestTo("https://graph.facebook.com/v27.0/123/media")).andRespond(withSuccess("{\"id\":\"456\"}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://graph.facebook.com/v27.0/456?fields=status_code"))
            .andRespond(withSuccess("{\"status_code\":\"IN_PROGRESS\"}",MediaType.APPLICATION_JSON));
        assertFalse(connector.publish(ig,approved).success());server.verify();
    }
    @Test void redditRequiresReviewBeforeAnySubmission(){
        var connector=new ReviewedSocialConnectors.Reddit(json,guard,builder.build(),"Sarah-test/1");
        assertFalse(connector.publish(rd,new PublishCommand("draft",null,"unreviewed",null)).success());
        server.verify();
    }
    @Test void redditTreatsApplicationErrorsAsFailure(){
        var connector=new ReviewedSocialConnectors.Reddit(json,guard,builder.build(),"Sarah-test/1");
        server.expect(requestTo("https://oauth.reddit.com/api/submit")).andExpect(method(HttpMethod.POST))
            .andExpect(header("User-Agent","Sarah-test/1"))
            .andRespond(withSuccess("{\"json\":{\"errors\":[[\"RULE\",\"Not allowed\",\"sr\"]]}}",MediaType.APPLICATION_JSON));
        assertFalse(connector.publish(rd,approved).success());server.verify();
    }
    @Test void redditConfirmsRealPostId(){
        var connector=new ReviewedSocialConnectors.Reddit(json,guard,builder.build(),"Sarah-test/1");
        server.expect(requestTo("https://oauth.reddit.com/api/submit"))
            .andRespond(withSuccess("{\"json\":{\"errors\":[],\"data\":{\"name\":\"t3_abcd\"}}}",MediaType.APPLICATION_JSON));
        var result=connector.publish(rd,approved);
        assertTrue(result.success());assertEquals("t3_abcd",result.externalId());server.verify();
    }
}
