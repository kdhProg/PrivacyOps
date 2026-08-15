package io.github.privacyops.fact;

import io.github.privacyops.model.SourceLocation;

public record JavaFieldFact(
        String id,
        String className,
        String fieldName,
        String fieldType,
        SourceLocation location
) implements Fact {
}