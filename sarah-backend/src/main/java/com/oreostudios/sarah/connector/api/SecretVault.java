package com.oreostudios.sarah.connector.api;

/** Encrypts external credentials at rest; never expose credentials to model prompts or HTTP responses. */
public interface SecretVault {
    String seal(String secret);
    String open(String sealedSecret);
}
