package com.oreostudios.sarah.seo.internal;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Consumer;

/** Bounded, workspace-owned manual jobs. Reports themselves are persisted by the controller. */
@Component
final class SeoJobs {
    private static final Logger log = LoggerFactory.getLogger(SeoJobs.class);
    record Progress(String phase, int discovered, int analyzed, int issues) {}
    record View(UUID id, String kind, String status, Progress progress, Long reportId, String error) {}
    private static final class Job {
        final UUID owner, id = UUID.randomUUID();
        final String kind;
        volatile View view;
        volatile Instant finished;
        Job(UUID owner, String kind) {
            this.owner = owner; this.kind = kind;
            this.view = new View(id, kind, "QUEUED", new Progress("Queued", 0, 0, 0), null, null);
        }
    }
    private final Map<UUID, Job> jobs = new HashMap<>();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(16), new ThreadPoolExecutor.AbortPolicy());

    synchronized View start(UUID owner, String kind, Function<Consumer<Progress>, Long> work) {
        jobs.values().removeIf(j -> j.finished != null && j.finished.isBefore(Instant.now().minusSeconds(3600)));
        for (Job j : jobs.values()) {
            if (j.owner.equals(owner) && j.finished == null)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An SEO job is already running in this company");
        }
        if (jobs.size() >= 256) {
            jobs.values().stream().filter(j -> j.finished != null)
                .min(Comparator.comparing(j -> j.finished)).ifPresent(j -> jobs.remove(j.id));
        }
        Job job = new Job(owner, kind);
        jobs.put(job.id, job);
        try {
            executor.execute(() -> {
                job.view = new View(job.id, kind, "RUNNING", new Progress("Starting", 0, 0, 0), null, null);
                try {
                    Long report = work.apply(progress -> job.view =
                        new View(job.id, kind, "RUNNING", progress, null, null));
                    job.view = new View(job.id, kind, "SUCCEEDED", job.view.progress(), report, null);
                } catch (RuntimeException ex) {
                    log.warn("SEO job {} ({}) failed: {}", job.id, kind, ex.getClass().getSimpleName());
                    job.view = new View(job.id, kind, "FAILED", job.view.progress(), null, safeError(kind, ex));
                } finally { job.finished = Instant.now(); }
            });
        } catch (RejectedExecutionException ex) {
            jobs.remove(job.id);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SEO queue is full; try again later");
        }
        return job.view;
    }

    synchronized View get(UUID owner, UUID id) {
        Job job = jobs.get(id);
        if (job == null || !job.owner.equals(owner))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SEO job not found; check saved report history");
        return job.view;
    }

    static String safeError(String kind, RuntimeException ex) {
        if (ex instanceof RestClientResponseException http) {
            int status = http.getStatusCode().value();
            if (status == 401 || status == 403) return "Provider rejected credentials or access (HTTP " + status + ")";
            if (status == 429) return "Provider rate limit or quota exceeded (HTTP 429)";
            return "Provider request failed (HTTP " + status + "); check model and provider configuration";
        }
        if (ex instanceof ResourceAccessException) return "Provider connection or response timed out";
        if (ex instanceof IllegalStateException && ex.getMessage() != null &&
            ex.getMessage().matches("Configure [A-Z_0-9]+")) return ex.getMessage();
        return kind.equals("crawl") ? "Website audit or report storage failed; check server logs" :
            "AI visibility or report storage failed; check provider settings and server logs";
    }

    @PreDestroy void stop() { executor.shutdownNow(); }
}
