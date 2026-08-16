package io.github.privacyops.model;

import io.github.privacyops.fact.Fact;
import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.policy.PrivacyPolicy;

import java.util.List;

public record AnalysisResult(
        List<Fact> facts,
        List<ClassifiedFact> classifiedFacts,
        List<DataFlowEdge> dataFlows,
        List<Finding> findings,
        List<String> warnings,
        PrivacyPolicy policy
) {
}