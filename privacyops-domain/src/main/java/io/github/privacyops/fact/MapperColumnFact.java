package io.github.privacyops.fact;

import io.github.privacyops.model.SourceLocation;

public record MapperColumnFact(
        String id,
        String mapperId,
        String tableName,
        String columnName,
        String resultType,
        SourceLocation location
) implements Fact {
}