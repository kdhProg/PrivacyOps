package io.github.privacyops.analyzer.risk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.github.privacyops.model.PrivacyType;
import io.github.privacyops.risk.KeywordRiskRule;
import io.github.privacyops.risk.RiskProfile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class YamlRiskProfileProvider {

    private final ObjectMapper mapper =
            new ObjectMapper(
                    new YAMLFactory()
            );

    public RiskProfile load(
            Path path
    ) {

        try {

            JsonNode root =
                    mapper.readTree(
                            path.toFile()
                    );

            Map<PrivacyType, Integer> typeWeights =
                    new EnumMap<>(
                            PrivacyType.class
                    );

            Map<String, KeywordRiskRule> keywordRules =
                    new HashMap<>();

            JsonNode privacyTypes =
                    root.path(
                            "privacy-types"
                    );

            if (privacyTypes.isObject()) {

                Iterator<Map.Entry<String, JsonNode>> fields =
                        privacyTypes.fields();

                while (fields.hasNext()) {

                    Map.Entry<String, JsonNode> entry =
                            fields.next();

                    try {

                        PrivacyType type =
                                PrivacyType.valueOf(
                                        entry.getKey()
                                                .trim()
                                                .toUpperCase()
                                );

                        int weight =
                                entry.getValue()
                                        .path("weight")
                                        .asInt(
                                                RiskProfile
                                                        .defaults()
                                                        .weightOf(type)
                                        );

                        typeWeights.put(
                                type,
                                weight
                        );

                    } catch (IllegalArgumentException ignored) {
                        // Unknown privacy type:
                        // ignore in MVP.
                    }
                }
            }

            /*
             * 기본값 위에 사용자 설정 override
             */
            Map<PrivacyType, Integer> merged =
                    new EnumMap<>(
                            PrivacyType.class
                    );

            merged.putAll(
                    RiskProfile.defaults()
                            .typeWeights()
            );

            merged.putAll(
                    typeWeights
            );

            JsonNode keywords =
                    root.path(
                            "keywords"
                    );

            if (keywords.isObject()) {

                Iterator<Map.Entry<String, JsonNode>> fields =
                        keywords.fields();

                while (fields.hasNext()) {

                    Map.Entry<String, JsonNode> entry =
                            fields.next();

                    String keyword =
                            entry.getKey()
                                    .trim()
                                    .toLowerCase();

                    String typeValue =
                            entry.getValue()
                                    .path("type")
                                    .asText();

                    if (typeValue.isBlank()) {
                        continue;
                    }

                    try {

                        PrivacyType type =
                                PrivacyType.valueOf(
                                        typeValue
                                                .trim()
                                                .toUpperCase()
                                );

                        int weight =
                                entry.getValue()
                                        .path("weight")
                                        .asInt(
                                                merged.getOrDefault(
                                                        type,
                                                        1
                                                )
                                        );

                        keywordRules.put(
                                keyword,
                                new KeywordRiskRule(
                                        type,
                                        weight
                                )
                        );

                    } catch (IllegalArgumentException ignored) {
                        // Unknown type:
                        // ignore in MVP.
                    }
                }
            }

            return new RiskProfile(
                    merged,
                    keywordRules
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to load risk profile: "
                            + path,
                    e
            );
        }
    }
}