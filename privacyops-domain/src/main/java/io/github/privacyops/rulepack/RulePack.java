package io.github.privacyops.rulepack;

import java.util.Map;

public record RulePack(
        String name,
        Map<String, RuleSetting> rules
) {

    public RulePack {

        name =
                name == null
                        || name.isBlank()
                        ? "default"
                        : name;

        rules =
                rules == null
                        ? Map.of()
                        : Map.copyOf(rules);
    }

    public static RulePack defaults() {

        return new RulePack(
                "privacyops-default",
                Map.of()
        );
    }

    public RuleSetting settingFor(
            String ruleId
    ) {

        return rules.getOrDefault(
                ruleId,
                RuleSetting.defaults()
        );
    }

    public boolean isEnabled(
            String ruleId
    ) {

        return settingFor(
                ruleId
        ).enabled();
    }
}