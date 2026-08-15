package io.github.privacyops.model;

public record Finding(
        String ruleId,
        String title,
        String description,
        Severity severity,
        SourceLocation location
) {
}