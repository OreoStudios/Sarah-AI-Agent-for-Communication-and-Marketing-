package com.oreostudios.sarah.seo.internal;
import org.junit.jupiter.api.Test;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
class SiteCrawlerTest {
    @Test void robotsSpecificAndLongestMatch(){
        String r="User-agent: *\nDisallow: /\nUser-agent: SarahSEO\nDisallow: /private\nAllow: /private/public\n";
        assertTrue(SiteCrawler.allowed(r,"SarahSEO","/"));
        assertFalse(SiteCrawler.allowed(r,"SarahSEO","/private/a"));
        assertTrue(SiteCrawler.allowed(r,"SarahSEO","/private/public/a"));
        assertFalse(SiteCrawler.allowed(r,"Googlebot","/"));
    }
    @Test void scope(){
        var c=new SiteCrawler("https://oreostudios.fr",50);
        var base=URI.create("https://oreostudios.fr/");
        assertNull(c.normalize("https://evil.example/",base));
        assertNull(c.normalize("/admin/users",base));
        assertNull(c.normalize("/page?token=secret",base));
        assertEquals("https://oreostudios.fr/page",c.normalize("/page#top",base).toString());
    }
    @Test void inspection() throws Exception {
        var c=new SiteCrawler("https://oreostudios.fr",50);
        var p=SiteCrawler.inspect(URI.create("https://oreostudios.fr/"),200,
          "<title>Test</title><h1>A</h1><img src=x><a href='/test'>link</a>","","",c);
        assertEquals(1,p.h1());assertEquals(1,p.missingAlt());
        assertTrue(p.issues().contains("MISSING_DESCRIPTION"));
        assertTrue(p.links().contains("https://oreostudios.fr/test"));
    }
}
