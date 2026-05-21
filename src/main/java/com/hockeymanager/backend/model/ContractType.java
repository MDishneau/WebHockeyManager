package com.hockeymanager.backend.model;

public enum ContractType {
    ENTRY_LEVEL ("Entry Level Contract (ELC)"),
    RFA         ("Restricted Free Agent"),
    UFA         ("Unrestricted Free Agent"),
    MAX         ("Maximum Contract");

    private final String displayName;
    ContractType(String displayName) { this.displayName = displayName; }
    public String getDisplayName()   { return displayName; }
}
