package com.oreostudios.sarah.agent.internal.infrastructure.web;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CreativeControllerTest {
 @Test void supportedSizes(){assertTrue(CreativeController.allowedSize("1024x1536"));assertFalse(CreativeController.allowedSize("auto"));assertFalse(CreativeController.allowedSize(null));}
 @Test void pngSignature(){assertTrue(CreativeController.isPng(new byte[]{(byte)137,80,78,71,13,10,26,10}));assertFalse(CreativeController.isPng(new byte[]{1,2,3}));}
}
