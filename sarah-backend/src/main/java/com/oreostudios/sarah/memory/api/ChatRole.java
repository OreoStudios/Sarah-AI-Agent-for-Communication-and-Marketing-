package com.oreostudios.sarah.memory.api; // Public API package of the memory module.

/** The author role of a conversation turn. */
public enum ChatRole {
    USER, // The owner.
    ASSISTANT, // Sarah.
    SYSTEM // System/context messages.
}
