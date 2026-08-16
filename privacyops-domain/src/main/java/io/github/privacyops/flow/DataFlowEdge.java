package io.github.privacyops.flow;

public record DataFlowEdge(
        String sourceFactId,
        String targetFactId,
        DataFlowRelation relation
) {
}