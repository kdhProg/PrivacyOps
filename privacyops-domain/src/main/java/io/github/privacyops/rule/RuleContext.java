package io.github.privacyops.rule;

import io.github.privacyops.fact.Fact;
import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.model.ClassifiedFact;

import java.util.List;

public record RuleContext(
        List<Fact> facts,
        List<ClassifiedFact> classifiedFacts,
        List<DataFlowEdge> dataFlows
) {
}