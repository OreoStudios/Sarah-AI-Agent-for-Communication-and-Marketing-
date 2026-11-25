package com.oreostudios.sarah.agent.internal.infrastructure.web;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EmailMarketingControllerTest {
 @Test void rejectsMultipleRecipientsAndHeaderInjection(){assertThrows(RuntimeException.class,()->EmailMarketingController.address("a@example.com,b@example.com"));assertThrows(RuntimeException.class,()->EmailMarketingController.subject("Hello\r\nBcc: hidden@example.org"));assertEquals("a@example.com",EmailMarketingController.address("A@Example.com"));}
}
