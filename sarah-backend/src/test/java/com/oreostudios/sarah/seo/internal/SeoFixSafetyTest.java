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
    @Test void auditUrlsResolveToRealWebsiteFilesNotSarahSourceFiles(){
        assertEquals("index.html",
            SeoFixService.websitePath("https://oreostudios.fr/","https://oreostudios.fr"));
        assertEquals("services/creation-site-vitrine/index.html",
            SeoFixService.websitePath("https://oreostudios.fr/services/creation-site-vitrine/","https://oreostudios.fr"));
        assertEquals("hosting.html",
            SeoFixService.websitePath("https://oreostudios.fr/hosting.html","https://oreostudios.fr"));
        assertNull(SeoFixService.websitePath("https://another-site.fr/hosting.html","https://oreostudios.fr"));
        assertNull(SeoFixService.websitePath("https://oreostudios.fr/%2e%2e/config","https://oreostudios.fr"));
        String audit = """
            {"results":[
              {"url":"https://oreostudios.fr/hosting.html","issues":["BROKEN_LINK: https://oreostudios.fr/missing.html"]},
              {"url":"https://oreostudios.fr/services/creation-site-vitrine/","issues":["LONG_DESCRIPTION"]},
              {"url":"https://oreostudios.fr/index.html","issues":["EXACT_DUPLICATE_TEXT"]}
            ]}
            """;
        var targets=SeoFixService.auditedWebsitePaths(audit,"crawl","https://oreostudios.fr");
        assertTrue(SeoFixService.sourceMatchScore("frontend/hosting.html",targets,"crawl")>900);
        assertTrue(SeoFixService.sourceMatchScore("frontend/services/creation-site-vitrine/index.html",targets,"crawl")>800);
        assertEquals(0,SeoFixService.sourceMatchScore("sarah-frontend/src/pages/Seo.tsx",targets,"crawl"));
        assertEquals(0,SeoFixService.sourceMatchScore("sarah-frontend/index.html",targets,"crawl"));
    }

    @Test void geoFallbackOnlyUsesWebsiteHomeAndServicePages(){
        var targets=SeoFixService.auditedWebsitePaths("{}","geo","https://oreostudios.fr");
        assertTrue(SeoFixService.sourceMatchScore("frontend/services/creation-site-ecommerce/index.html",targets,"geo")>0);
        assertTrue(SeoFixService.sourceMatchScore("frontend/index.html",targets,"geo")>0);
        assertEquals(0,SeoFixService.sourceMatchScore("sarah-frontend/src/pages/Seo.tsx",targets,"geo"));
    }

    @Test void checksTheWorkspaceCompanyNotAHardcodedBusiness(){
        assertTrue(SeoProviders.mentionsCompany("Visit **Oreo Studios** for software services.","OreoStudios"));
        assertTrue(SeoProviders.mentionsCompany("We recommend Alsacréations.","Alsacréations"));
        assertFalse(SeoProviders.mentionsCompany("We recommend Alsacréations.","OreoStudios"));
        assertFalse(SeoProviders.mentionsCompany("No specific agency.",""));
    }
}
