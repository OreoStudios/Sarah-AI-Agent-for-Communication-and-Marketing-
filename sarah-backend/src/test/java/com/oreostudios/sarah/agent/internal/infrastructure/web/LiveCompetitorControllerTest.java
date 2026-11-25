package com.oreostudios.sarah.agent.internal.infrastructure.web;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LiveCompetitorControllerTest {
 @Test void validatesEvidenceLinks(){assertTrue(LiveCompetitorController.publicWebLink("https://example.org/services"));assertFalse(LiveCompetitorController.publicWebLink("javascript:alert(1)"));assertFalse(LiveCompetitorController.publicWebLink("https://user:secret@example.org"));}
 @Test void rejectsUnboundedQueries(){assertThrows(RuntimeException.class,()->LiveCompetitorController.bounded("x".repeat(161),160));}
}
