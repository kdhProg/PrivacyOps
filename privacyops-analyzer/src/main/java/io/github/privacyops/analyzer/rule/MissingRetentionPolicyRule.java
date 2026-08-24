package io.github.privacyops.analyzer.rule;

import io.github.privacyops.fact.MapperColumnFact;
import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.flow.DataFlowRelation;
import io.github.privacyops.model.Evidence;
import io.github.privacyops.model.EvidenceType;
import io.github.privacyops.model.Finding;
import io.github.privacyops.model.Severity;
import io.github.privacyops.policy.ResourcePolicy;
import io.github.privacyops.rule.PrivacyRule;
import io.github.privacyops.rule.RuleContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MissingRetentionPolicyRule
        implements PrivacyRule {

    public static final String RULE_ID =
            "PRIV-RETENTION-001";

    @Override
    public String id() {
        return RULE_ID;
    }

    @Override
    public String name() {
        return "Missing retention policy";
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

            // 리소스 정책 자체가 없으면
            // MissingResourcePolicyRule에서 처리
            if (resourcePolicy == null) {
                continue;
            }

            String retention =
                    resourcePolicy.retention();

            if (retention != null
                    && !retention.isBlank()) {
                continue;
            }

            if (!reportedTables.add(tableName)) {
                continue;
            }

            List<Evidence> evidence =
                    List.of(
                            new Evidence(
                                    EvidenceType.POLICY,
                                    "Privacy resource",
                                    tableName
                                            + " is registered as "
                                            + "a managed privacy resource.",
                                    column.location()
                            ),
                            new Evidence(
                                    EvidenceType.POLICY,
                                    "Retention policy",
                                    tableName
                                            + ".retention is not defined.",
                                    column.location()
                            )
                    );

            findings.add(
                    new Finding(
                            id(),
                            name(),
                            "Privacy resource "
                                    + tableName
                                    + " has no retention policy.",
                            defaultSeverity(),
                            column.location(),
                            evidence
                    )
            );
        }

        return findings;
    }
}