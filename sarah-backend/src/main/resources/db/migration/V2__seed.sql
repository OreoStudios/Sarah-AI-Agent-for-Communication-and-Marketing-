-- Seed data: brand profile for Oreo Studios + autonomy defaults.
-- The owner (admin) account is seeded in application code (needs BCrypt hashing).

INSERT INTO brand_profile (id, company_name, description, services, tone, languages, website, active)
VALUES (
    gen_random_uuid(),
    'Oreo Studios',
    'Oreo Studios is a software development studio that builds reliable backend systems, web applications and custom business software for SMBs, entrepreneurs and software teams.',
    E'Java / Spring Boot backend development\nWeb application development\nCustom business software\nMinecraft server plugin development\nTechnical consulting\nAPI development and integrations\nSoftware maintenance and optimization',
    'Professional, clear, helpful, and honest. Adapts register to the audience.',
    'en,fr',
    'https://oreostudios.com',
    TRUE
);

INSERT INTO autonomy_settings (id, enabled, daily_post_limit, per_platform_cooldown_minutes, max_llm_calls_per_run)
VALUES (gen_random_uuid(), TRUE, 10, 30, 25);
