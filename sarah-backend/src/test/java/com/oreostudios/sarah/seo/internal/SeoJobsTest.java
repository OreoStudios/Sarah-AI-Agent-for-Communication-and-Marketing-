package com.oreostudios.sarah.seo.internal;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class SeoJobsTest {
    @Test void jobsAreScopedAndReturnTheExactSavedReport() throws Exception {
        SeoJobs jobs = new SeoJobs();
        UUID company = UUID.randomUUID(), other = UUID.randomUUID();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        try {
            var job = jobs.start(company, "crawl", progress -> {
                progress.accept(new SeoJobs.Progress("Analyzing pages", 7, 2, 3));
                entered.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Test timed out");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
                return 42L;
            });
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            assertEquals(2, jobs.get(company, job.id()).progress().analyzed());
            assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> jobs.get(other, job.id())).getStatusCode().value());
            assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> jobs.start(company, "geo", p -> 99L)).getStatusCode().value());
            release.countDown();
            var result = awaitTerminal(jobs, company, job.id());
            assertEquals("SUCCEEDED", result.status());
            assertEquals(Long.valueOf(42), result.reportId());
        } finally { release.countDown(); jobs.stop(); }
    }

    @Test void errorsDoNotLeakProviderSecrets() throws Exception {
        SeoJobs jobs = new SeoJobs();
        UUID company = UUID.randomUUID();
        try {
            var job = jobs.start(company, "geo", progress -> {
                throw new IllegalStateException("secret-token-must-not-be-returned");
            });
            var result = awaitTerminal(jobs, company, job.id());
            assertEquals("FAILED", result.status());
            assertNull(result.reportId());
            assertFalse(result.error().contains("secret-token"));
            assertEquals("Configure OPENAI_API_KEY",
                SeoJobs.safeError("geo", new IllegalStateException("Configure OPENAI_API_KEY")));
        } finally { jobs.stop(); }
    }

    private static SeoJobs.View awaitTerminal(SeoJobs jobs, UUID company, UUID id) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < end) {
            var result = jobs.get(company, id);
            if ("SUCCEEDED".equals(result.status()) || "FAILED".equals(result.status())) return result;
            Thread.sleep(10);
        }
        throw new AssertionError("Job did not finish");
    }
}
