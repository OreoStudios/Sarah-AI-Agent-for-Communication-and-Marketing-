package com.oreostudios.sarah.agent.internal.infrastructure.web;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/public/marketing/unsubscribe")
public class PublicUnsubscribeController {
    private final JdbcTemplate db;
    public PublicUnsubscribeController(JdbcTemplate db){this.db=db;}
    @GetMapping(value="/{token}",produces=MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirm(@PathVariable UUID token){
        // GET never mutates: link scanners cannot unsubscribe. No recipient identity is exposed.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .header("Referrer-Policy","no-referrer")
            .header("Content-Security-Policy","default-src 'none'; form-action 'self'; frame-ancestors 'none'")
            .body("<!doctype html><html lang=\"en\"><meta charset=\"utf-8\"><title>Unsubscribe</title><h1>Stop marketing emails?</h1><form method=\"post\"><button type=\"submit\">Unsubscribe</button></form></html>");
    }
    @PostMapping(value="/{token}",produces=MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> unsubscribe(@PathVariable UUID token){
        db.update("UPDATE marketing_subscriber SET suppressed=true WHERE unsubscribe_token=?",token);
        // Deliberately identical response for unknown or already suppressed tokens.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Referrer-Policy","no-referrer")
            .body("<!doctype html><html lang=\"en\"><meta charset=\"utf-8\"><title>Unsubscribed</title><p>Your unsubscribe request has been processed.</p></html>");
    }
}
