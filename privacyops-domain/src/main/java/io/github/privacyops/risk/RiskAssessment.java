package io.github.privacyops.risk;

import io.github.privacyops.model.PrivacyType;

public record RiskAssessment(
        String factId,
        PrivacyType privacyType,
        int weight,
        String level,
        String reason
) {
}