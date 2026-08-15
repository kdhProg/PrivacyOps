package io.github.privacyops.model;

public record Classification(
        PrivacyType privacyType,
        double confidence,
        String reason
) {
}