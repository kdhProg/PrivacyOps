package io.github.privacyops.fact;

import io.github.privacyops.model.SourceLocation;

public record ApiAccessControlFact(
        String id,
        String endpointId,
        String annotationType,
        String expression,
        SourceLocation location
) implements Fact {
}