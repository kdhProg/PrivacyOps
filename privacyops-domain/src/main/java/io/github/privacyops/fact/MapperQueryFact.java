package io.github.privacyops.fact;

import io.github.privacyops.model.SourceLocation;

import java.util.List;

public record MapperQueryFact(
        String id,
        String mapperId,
        String resultType,
        List<String> tables,
        List<String> columns,
        SourceLocation location
) implements Fact {
}