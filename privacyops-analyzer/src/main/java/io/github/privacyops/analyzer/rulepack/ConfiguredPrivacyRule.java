package io.github.privacyops.analyzer.rulepack;

import io.github.privacyops.model.Finding;
import io.github.privacyops.model.Severity;
import io.github.privacyops.rule.PrivacyRule;
import io.github.privacyops.rule.RuleContext;
import io.github.privacyops.rulepack.RuleSetting;

import java.util.List;

public class ConfiguredPrivacyRule
        implements PrivacyRule {

    private final PrivacyRule delegate;
    private final RuleSetting setting;

    public ConfiguredPrivacyRule(
            PrivacyRule delegate,
            RuleSetting setting
    ) {

        this.delegate =
                delegate;

        this.setting =
                setting;
    }

    @Override
    public String id() {

        return delegate.id();
    }

    @Override
    public String name() {

        return delegate.name();
    }

    @Override
    public Severity defaultSeverity() {

        if (setting.severity() != null) {

            return setting.severity();
        }

        return delegate.defaultSeverity();
    }

    @Override
    public List<Finding> evaluate(
            RuleContext context
    ) {

        if (!setting.enabled()) {

            return List.of();
        }

        List<Finding> findings =
                delegate.evaluate(
                        context
                );

        if (setting.severity() == null) {

            return findings;
        }

        Severity overrideSeverity =
                setting.severity();

        return findings.stream()
                .map(
                        finding ->
                                new Finding(
                                        finding.ruleId(),
                                        finding.title(),
                                        finding.description(),
                                        overrideSeverity,
                                        finding.location(),
                                        finding.evidence()
                                )
                )
                .toList();
    }
}