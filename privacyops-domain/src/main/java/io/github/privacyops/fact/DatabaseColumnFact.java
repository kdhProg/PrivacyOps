package io.github.privacyops.fact;

import io.github.privacyops.model.SourceLocation;

public record DatabaseColumnFact(
        String id,
        String schemaName,
        String tableName,
        String columnName,
        String columnType,
        String comment,
        SourceLocation location
) implements Fact {
}