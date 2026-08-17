package io.github.privacyops.fact;

import io.github.privacyops.model.SourceLocation;

public record ApiAuditControlFact(
        String id,
        String endpointId,
        String auditEvent,
        String sourceType,
        SourceLocation location
) implements Fact {
}