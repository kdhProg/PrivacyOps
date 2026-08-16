package io.github.privacyops.policy;

import java.util.Map;

public record PrivacyPolicy(
        Map<String, ResourcePolicy> resources
) {

    public static PrivacyPolicy empty() {
        return new PrivacyPolicy(Map.of());
    }
}