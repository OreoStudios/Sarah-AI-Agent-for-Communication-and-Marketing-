package com.oreostudios.sarah.prospecting.internal.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Read-only, bounded discovery of public French company records. No contact harvesting. */
@RestController
@RequestMapping("/api/v1/prospects/discovery")
@PreAuthorize("hasRole('OWNER')")
public class CompanyDiscoveryController {
    private final RestClient client;
    public CompanyDiscoveryController() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(12));
        client = RestClient.builder().baseUrl("https://recherche-entreprises.api.gouv.fr")
                .requestFactory(factory).build();
    }
    public record Candidate(String siren, String companyName, String activityCode, String locality, String sourceUrl) {}
    public record Discovery(List<Candidate> candidates, int page, String source) {}
    @GetMapping
    public Discovery search(@RequestParam String q,
                            @RequestParam(required=false) String postalCode,
                            @RequestParam(defaultValue="1") int page) {
        if (q == null || q.strip().length() < 3 || q.length() > 120 || page < 1 || page > 10
                || (postalCode != null && !postalCode.matches("[0-9]{5}")))
            throw new IllegalArgumentException("Invalid company query, postal code or page");
        JsonNode data = client.get().uri(b -> {
            b.path("/search").queryParam("q", q.strip()).queryParam("page", page)
                    .queryParam("per_page", 10).queryParam("etat_administratif", "A");
            if (postalCode != null) b.queryParam("code_postal", postalCode);
            return b.build();
        }).retrieve().body(JsonNode.class);
        List<Candidate> results = new ArrayList<>();
        if (data != null) for (JsonNode item : data.path("results")) {
            String siren = item.path("siren").asText("");
            String name = item.path("nom_raison_sociale").asText("");
            // Skip records without a business name, non-diffusible records and individual entrepreneurs.
            if (!siren.matches("[0-9]{9}") || name.isBlank()
                    || !"A".equals(item.path("etat_administratif").asText())
                    || !"O".equals(item.path("statut_diffusion").asText())
                    || item.path("nature_juridique").asText().startsWith("1")) continue;
            results.add(new Candidate(siren, name, item.path("activite_principale").asText(""),
                    item.path("siege").path("libelle_commune").asText(""),
                    "https://annuaire-entreprises.data.gouv.fr/entreprise/" + siren));
        }
        return new Discovery(List.copyOf(results), page, "API Recherche d'entreprises (data.gouv.fr)");
    }
}
