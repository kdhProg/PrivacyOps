package io.github.privacyops.model;

public record Evidence(
        EvidenceType type,
        String title,
        String description,
        SourceLocation location
) {
}