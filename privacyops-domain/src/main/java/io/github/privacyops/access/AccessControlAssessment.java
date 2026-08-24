package io.github.privacyops.access;

import io.github.privacyops.model.Evidence;

import java.util.List;

public record AccessControlAssessment(
        boolean controlled,
        String providerId,
        String description,
        List<Evidence> evidence
) {

    public AccessControlAssessment {

        evidence =
                evidence == null
                        ? List.of()
                        : List.copyOf(evidence);
    }

    public static AccessControlAssessment controlled(
            String providerId,
            String description,
            List<Evidence> evidence
    ) {

        return new AccessControlAssessment(
                true,
                providerId,
                description,
                evidence
        );
    }

    public static AccessControlAssessment notControlled(
            String providerId,
            String description,
            List<Evidence> evidence
    ) {

        return new AccessControlAssessment(
                false,
                providerId,
                description,
                evidence
        );
    }
}