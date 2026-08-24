package io.github.privacyops.model;

import java.util.List;

public record Finding(
        String ruleId,
        String title,
        String description,
        Severity severity,
        SourceLocation location,
        List<Evidence> evidence
) {

    public Finding {

        evidence =
                evidence == null
                        ? List.of()
                        : List.copyOf(evidence);
    }

    public Finding(
            String ruleId,
            String title,
            String description,
            Severity severity,
            SourceLocation location
    ) {

        this(
                ruleId,
                title,
                description,
                severity,
                location,
                List.of()
        );
    }
}