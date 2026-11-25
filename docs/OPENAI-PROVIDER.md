# OpenAI provider

Set `SARAH_LLM_PROVIDER=openai`, `OPENAI_API_KEY` and optionally `SARAH_OPENAI_MODEL` (default `gpt-4.1-mini`) in the backend environment. Existing Anthropic and mock modes are unchanged. In Docker/Coolify pass the environment variables to the backend service. The adapter produces text only; it cannot authorize or directly execute tools. API failures are surfaced without response bodies or credentials.
