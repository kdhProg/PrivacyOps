package io.github.privacyops.risk;

import io.github.privacyops.model.PrivacyType;

public record KeywordRiskRule(
        PrivacyType type,
        int weight
) {
}