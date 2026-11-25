package com.oreostudios.sarah.dashboard.internal.infrastructure.web;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class MarketingAnalyticsControllerTest {
    @Test void emptyCohortDoesNotInventSuccessRate(){
        var result=MarketingAnalyticsController.metrics(List.of());
        assertEquals(0,result.published());
        assertEquals(0,result.attempts());
        assertNull(result.deliverySuccessRate());
    }
}
