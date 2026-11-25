package com.oreostudios.sarah.seo.internal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SeoFixSafetyTest {
    @Test void acceptsOnlyGithubRepositoriesAndWebSourceFiles(){
        assertEquals("https://github.com/OreoStudios/OreoStudios",
            SeoFixService.parseRepo("https://github.com/OreoStudios/OreoStudios").url());
        assertTrue(SeoFixService.allowedPath("src/app/page.tsx"));
        assertTrue(SeoFixService.allowedPath("src/main/resources/templates/index.html"));
        assertFalse(SeoFixService.allowedPath("src/.env"));
        assertFalse(SeoFixService.allowedPath("src/app/../../.env"));
        assertFalse(SeoFixService.allowedPath(".github/workflows/ci.yml"));
        assertFalse(SeoFixService.allowedPath("src/main/resources/application.yml"));
        assertThrows(Exception.class,()->SeoFixService.parseRepo("https://github.com.evil.test/x/y"));
        assertThrows(Exception.class,()->SeoFixService.parseRepo("http://github.com/x/y"));
        assertThrows(Exception.class,()->SeoFixService.parseRepo("https://github.com/x/y?token=secret"));
    }
    @Test void checksTheWorkspaceCompanyNotAHardcodedBusiness(){
        assertTrue(SeoProviders.mentionsCompany("Visit **Oreo Studios** for software services.","OreoStudios"));
        assertTrue(SeoProviders.mentionsCompany("We recommend Alsacréations.","Alsacréations"));
        assertFalse(SeoProviders.mentionsCompany("We recommend Alsacréations.","OreoStudios"));
        assertFalse(SeoProviders.mentionsCompany("No specific agency.",""));
    }
}
