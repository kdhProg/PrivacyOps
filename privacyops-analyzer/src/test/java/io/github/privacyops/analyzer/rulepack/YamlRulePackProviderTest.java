package io.github.privacyops.analyzer.rulepack;

import io.github.privacyops.model.Severity;
import io.github.privacyops.rulepack.RulePack;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlRulePackProviderTest {

    @Test
    void loadsRulePack()
            throws Exception {

        Path path =
                Path.of(
                        getClass()
                                .getClassLoader()
                                .getResource(
                                        "rulepack/strict.yml"
                                )
                                .toURI()
                );

        YamlRulePackProvider provider =
                new YamlRulePackProvider();

        RulePack rulePack =
                provider.load(path);

        assertEquals(
                "strict-test",
                rulePack.name()
        );

        assertTrue(
                rulePack.isEnabled(
                        "PRIV-ACCESS-001"
                )
        );

        assertEquals(
                Severity.CRITICAL,
                rulePack.settingFor(
                        "PRIV-ACCESS-001"
                ).severity()
        );

        assertFalse(
                rulePack.isEnabled(
                        "PRIV-AUDIT-001"
                )
        );

        /*
         * Rule Pack에 없는 Rule은
         * 기본적으로 활성화
         */
        assertTrue(
                rulePack.isEnabled(
                        "PRIV-API-001"
                )
        );
    }
}