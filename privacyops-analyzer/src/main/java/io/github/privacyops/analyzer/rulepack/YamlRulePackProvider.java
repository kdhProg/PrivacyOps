package io.github.privacyops.analyzer.rulepack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.github.privacyops.model.Severity;
import io.github.privacyops.rulepack.RulePack;
import io.github.privacyops.rulepack.RuleSetting;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class YamlRulePackProvider {

    private final ObjectMapper mapper =
            new ObjectMapper(
                    new YAMLFactory()
            );

    public RulePack load(
            Path path
    ) {

        try {

            JsonNode root =
                    mapper.readTree(
                            path.toFile()
                    );

            String name =
                    root.path("name")
                            .asText(
                                    "custom-rule-pack"
                            );

            Map<String, RuleSetting> rules =
                    new HashMap<>();

            JsonNode rulesNode =
                    root.path("rules");

            if (rulesNode.isObject()) {

                Iterator<Map.Entry<String, JsonNode>> fields =
                        rulesNode.fields();

                while (fields.hasNext()) {

                    Map.Entry<String, JsonNode> entry =
                            fields.next();

                    String ruleId =
                            entry.getKey();

                    JsonNode value =
                            entry.getValue();

                    boolean enabled =
                            value.path("enabled")
                                    .asBoolean(true);

                    Severity severity =
                            parseSeverity(
                                    value.path("severity")
                                            .asText(null)
                            );

                    rules.put(
                            ruleId,
                            new RuleSetting(
                                    enabled,
                                    severity
                            )
                    );
                }
            }

            return new RulePack(
                    name,
                    rules
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to load rule pack: "
                            + path,
                    e
            );
        }
    }

    private Severity parseSeverity(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        try {

            return Severity.valueOf(
                    value.trim()
                            .toUpperCase()
            );

        } catch (IllegalArgumentException e) {

            return null;
        }
    }
}