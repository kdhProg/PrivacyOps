package io.github.privacyops.analyzer.risk;

import io.github.privacyops.model.PrivacyType;
import io.github.privacyops.risk.RiskProfile;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlRiskProfileProviderTest {

    @Test
    void loadsCustomRiskProfile()
            throws Exception {

        Path path =
                Path.of(
                        "../samples",
                        "legacy-member-system",
                        "risk-profile.yml"
                );

        YamlRiskProfileProvider provider =
                new YamlRiskProfileProvider();

        RiskProfile profile =
                provider.load(path);

        assertEquals(
                10,
                profile.weightOf(
                        PrivacyType.NATIONAL_IDENTIFIER
                )
        );

        assertEquals(
                3,
                profile.weightOf(
                        PrivacyType.EMAIL
                )
        );

        assertEquals(
                8,
                profile.keywords()
                        .get("employee_no")
                        .weight()
        );
    }
}