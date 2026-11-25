package com.oreostudios.sarah.agent.internal.infrastructure.web;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class PublicUnsubscribeControllerTest {
 @Test void getDoesNotMutateAndPostSuppresses(){var db=mock(JdbcTemplate.class);var c=new PublicUnsubscribeController(db);var id=UUID.randomUUID();assertEquals(200,c.confirm(id).getStatusCode().value());verifyNoInteractions(db);assertEquals(200,c.unsubscribe(id).getStatusCode().value());verify(db).update("UPDATE marketing_subscriber SET suppressed=true WHERE unsubscribe_token=?",id);}
 @Test void validatesOrigin(){assertTrue(EmailMarketingController.validPublicUrl("https://sarah.example.com"));assertFalse(EmailMarketingController.validPublicUrl("http://example.com"));assertFalse(EmailMarketingController.validPublicUrl("https://example.com/path"));}
}
