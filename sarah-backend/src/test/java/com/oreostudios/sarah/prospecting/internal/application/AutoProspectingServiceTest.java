package com.oreostudios.sarah.prospecting.internal.application;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AutoProspectingServiceTest {
 @Test void scoreUsesEvidenceNotPersonalData(){assertEquals(75,AutoProspectingService.score("62.01Z","62","Strasbourg"));assertEquals(60,AutoProspectingService.score("62.01Z","62",""));assertEquals(0,AutoProspectingService.score("43.22B","62","Strasbourg"));}
}
