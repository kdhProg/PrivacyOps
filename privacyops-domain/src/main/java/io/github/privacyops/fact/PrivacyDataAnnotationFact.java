package io.github.privacyops.fact;

import io.github.privacyops.model.SourceLocation;

public record PrivacyDataAnnotationFact(
        String id,
        String fieldFactId,
        String declaredType,
        SourceLocation location
) implements Fact {
}