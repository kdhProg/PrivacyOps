package io.github.privacyops.risk;

import io.github.privacyops.model.PrivacyType;

import java.util.Map;

public record RiskProfile(
        Map<PrivacyType, Integer> typeWeights,
        Map<String, KeywordRiskRule> keywords
) {

    public RiskProfile {

        typeWeights =
                typeWeights == null
                        ? Map.of()
                        : Map.copyOf(typeWeights);

        keywords =
                keywords == null
                        ? Map.of()
                        : Map.copyOf(keywords);
    }

    public static RiskProfile defaults() {

        return new RiskProfile(
                Map.of(
                        PrivacyType.NAME,
                        1,
                        PrivacyType.EMAIL,
                        2,
                        PrivacyType.PHONE_NUMBER,
                        2,
                        PrivacyType.NATIONAL_IDENTIFIER,
                        5
                ),
                Map.of()
        );
    }

    public int weightOf(
            PrivacyType type
    ) {

        return typeWeights.getOrDefault(
                type,
                1
        );
    }
}