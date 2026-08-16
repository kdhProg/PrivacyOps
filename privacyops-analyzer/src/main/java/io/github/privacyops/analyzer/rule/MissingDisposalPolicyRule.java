package io.github.privacyops.analyzer.rule;

import io.github.privacyops.fact.MapperColumnFact;
import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.flow.DataFlowRelation;
import io.github.privacyops.model.Finding;
import io.github.privacyops.model.Severity;
import io.github.privacyops.policy.ResourcePolicy;
import io.github.privacyops.rule.PrivacyRule;
import io.github.privacyops.rule.RuleContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MissingDisposalPolicyRule
        implements PrivacyRule {

    public static final String RULE_ID =
            "PRIV-DISPOSAL-001";

    @Override
    public String id() {
        return RULE_ID;
    }

    @Override
    public String name() {
        return "Missing disposal policy";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.HIGH;
    }

    @Override
    public List<Finding> evaluate(
            RuleContext context
    ) {

        List<Finding> findings =
                new ArrayList<>();

        Set<String> reportedTables =
                new HashSet<>();

        for (DataFlowEdge edge :
                context.dataFlows()) {

            if (edge.relation()
                    != DataFlowRelation.MAPPER_RESULT) {
                continue;
            }

            boolean privacyField =
                    context.classifiedFacts()
                            .stream()
                            .anyMatch(
                                    item ->
                                            item.fact()
                                                    .id()
                                                    .equals(
                                                            edge.targetFactId()
                                                    )
                            );

            if (!privacyField) {
                continue;
            }

            MapperColumnFact column =
                    context.facts()
                            .stream()
                            .filter(
                                    fact ->
                                            fact.id()
                                                    .equals(
                                                            edge.sourceFactId()
                                                    )
                            )
                            .filter(
                                    MapperColumnFact.class
                                            ::isInstance
                            )
                            .map(
                                    MapperColumnFact.class
                                            ::cast
                            )
                            .findFirst()
                            .orElse(null);

            if (column == null) {
                continue;
            }

            String tableName =
                    column.tableName();

            ResourcePolicy resourcePolicy =
                    context.policy()
                            .resources()
                            .get(tableName);

            if (resourcePolicy == null) {
                continue;
            }

            String disposal =
                    resourcePolicy.disposal();

            if (disposal != null
                    && !disposal.isBlank()) {
                continue;
            }

            if (!reportedTables.add(tableName)) {
                continue;
            }

            findings.add(
                    new Finding(
                            id(),
                            name(),
                            "Privacy resource "
                                    + tableName
                                    + " has no disposal policy.",
                            defaultSeverity(),
                            column.location()
                    )
            );
        }

        return findings;
    }
}