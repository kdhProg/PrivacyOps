package io.github.privacyops.fact;

import io.github.privacyops.model.SourceLocation;

public record ApiEndpointFact(
        String id,
        String httpMethod,
        String path,
        String controllerClass,
        String controllerMethod,
        String responseType,
        SourceLocation location
) implements Fact {
}