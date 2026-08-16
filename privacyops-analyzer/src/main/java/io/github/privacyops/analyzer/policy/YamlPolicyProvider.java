package io.github.privacyops.analyzer.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.github.privacyops.policy.PolicyProvider;
import io.github.privacyops.policy.PrivacyPolicy;

import java.io.IOException;
import java.nio.file.Path;

public class YamlPolicyProvider
        implements PolicyProvider {

    private final ObjectMapper objectMapper;

    public YamlPolicyProvider() {
        this.objectMapper =
                new ObjectMapper(
                        new YAMLFactory()
                );
    }

    @Override
    public PrivacyPolicy load(Path path) {

        try {

            return objectMapper.readValue(
                    path.toFile(),
                    PrivacyPolicy.class
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to load privacy policy: "
                            + path,
                    e
            );
        }
    }
}