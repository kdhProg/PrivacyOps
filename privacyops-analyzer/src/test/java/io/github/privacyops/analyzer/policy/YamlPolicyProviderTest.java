package io.github.privacyops.analyzer.policy;

import io.github.privacyops.policy.PrivacyPolicy;
import io.github.privacyops.policy.ResourcePolicy;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class YamlPolicyProviderTest {

    @Test
    void loadsPrivacyPolicy()
            throws Exception {

        Path path =
                Path.of(
                        getClass()
                                .getClassLoader()
                                .getResource(
                                        "project-sample/"
                                                + "privacyops-policy.yml"
                                )
                                .toURI()
                );

        YamlPolicyProvider provider =
                new YamlPolicyProvider();

        PrivacyPolicy policy =
                provider.load(path);

        assertNotNull(policy);

        assertTrue(
                policy.resources()
                        .containsKey(
                                "TB_MEMBER"
                        )
        );

        ResourcePolicy resource =
                policy.resources()
                        .get("TB_MEMBER");

        assertEquals(
                "member-management",
                resource.purpose()
        );

        assertEquals(
                "3y",
                resource.retention()
        );

        assertEquals(
                "scheduled-delete",
                resource.disposal()
        );
    }
}