/**
 * LLM module: the narrow boundary to the language model. The LLM produces text only; it never
 * controls credentials, authorization, or outbound HTTP. An offline mock is the default; a live
 * Anthropic/Claude adapter activates via configuration.
 */
@org.springframework.modulith.ApplicationModule(displayName = "LLM") // Bounded module; only OPEN dependencies.
package com.oreostudios.sarah.llm; // Root package of the llm module.
