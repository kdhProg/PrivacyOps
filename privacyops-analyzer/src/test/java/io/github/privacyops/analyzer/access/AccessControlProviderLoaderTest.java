package io.github.privacyops.analyzer.access;

import io.github.privacyops.access.AccessControlProvider;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessControlProviderLoaderTest {

    @Test
    void loadsBuiltInAndSpiProviders() {

        AccessControlProviderLoader loader =
                new AccessControlProviderLoader();

        List<AccessControlProvider> providers =
                loader.load();

        assertTrue(
                providers.stream()
                        .anyMatch(
                                provider ->
                                        provider.id()
                                                .equals(
                                                        "spring-security"
                                                )
                        )
        );

        assertTrue(
                providers.stream()
                        .anyMatch(
                                provider ->
                                        provider.id()
                                                .equals(
                                                        "example-iam"
                                                )
                        )
        );
    }
}