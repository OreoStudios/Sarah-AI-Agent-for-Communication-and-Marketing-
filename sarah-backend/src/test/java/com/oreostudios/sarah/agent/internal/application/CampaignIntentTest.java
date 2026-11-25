package com.oreostudios.sarah.agent.internal.application;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CampaignIntentTest {
    @Test void explicitCampaignRequests(){
        assertTrue(CampaignIntent.isExplicit("launch a campaign to promote our Spring Boot services on Discord"));
        assertTrue(CampaignIntent.isExplicit("/campaign promote Oreo Studios"));
        assertTrue(CampaignIntent.isExplicit("crée une campagne pour notre site"));
    }
    @Test void generalDiscussionDoesNotSchedule(){
        assertFalse(CampaignIntent.isExplicit("What is marketing?"));
        assertFalse(CampaignIntent.isExplicit("Do not launch a campaign"));
        assertFalse(CampaignIntent.isExplicit("Can you explain our marketing campaign?"));
    }
}
