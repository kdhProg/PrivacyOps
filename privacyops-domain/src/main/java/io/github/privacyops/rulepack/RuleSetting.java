package io.github.privacyops.rulepack;

import io.github.privacyops.model.Severity;

public record RuleSetting(
        boolean enabled,
        Severity severity
) {

    public static RuleSetting defaults() {

        return new RuleSetting(
                true,
                null
        );
    }
}