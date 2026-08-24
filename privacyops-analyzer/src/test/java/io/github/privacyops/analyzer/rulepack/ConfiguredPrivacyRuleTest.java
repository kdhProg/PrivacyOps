package io.github.privacyops.analyzer.rulepack;

import io.github.privacyops.model.Finding;
import io.github.privacyops.model.Severity;
import io.github.privacyops.rule.PrivacyRule;
import io.github.privacyops.rule.RuleContext;
import io.github.privacyops.rulepack.RuleSetting;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguredPrivacyRuleTest {

    @Test
    void overridesSeverity() {

        PrivacyRule baseRule =
                new TestRule();

        ConfiguredPrivacyRule rule =
                new ConfiguredPrivacyRule(
                        baseRule,
                        new RuleSetting(
                                true,
                                Severity.CRITICAL
                        )
                );

        List<Finding> findings =
                rule.evaluate(null);

        assertEquals(
                1,
                findings.size()
        );

        assertEquals(
                Severity.CRITICAL,
                findings.get(0)
                        .severity()
        );
    }

    @Test
    void disablesRule() {

        ConfiguredPrivacyRule rule =
                new ConfiguredPrivacyRule(
                        new TestRule(),
                        new RuleSetting(
                                false,
                                null
                        )
                );

        assertTrue(
                rule.evaluate(null)
                        .isEmpty()
        );
    }

    private static class TestRule
            implements PrivacyRule {

        @Override
        public String id() {
            return "TEST-001";
        }

        @Override
        public String name() {
            return "Test Rule";
        }

        @Override
        public Severity defaultSeverity() {
            return Severity.LOW;
        }

        @Override
        public List<Finding> evaluate(
                RuleContext context
        ) {

            return List.of(
                    new Finding(
                            id(),
                            name(),
                            "test",
                            Severity.LOW,
                            null
                    )
            );
        }
    }
}