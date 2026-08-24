package io.github.privacyops.analyzer.rule;

import io.github.privacyops.fact.ApiAuditControlFact;
import io.github.privacyops.fact.ApiEndpointFact;
import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.flow.DataFlowRelation;
import io.github.privacyops.model.Evidence;
import io.github.privacyops.model.EvidenceType;
import io.github.privacyops.model.Finding;
import io.github.privacyops.model.Severity;
import io.github.privacyops.rule.PrivacyRule;
import io.github.privacyops.rule.RuleContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MissingAuditControlRule
        implements PrivacyRule {

    public static final String RULE_ID =
            "PRIV-AUDIT-001";

    @Override
    public String id() {
        return RULE_ID;
    }

    @Override
    public String name() {
        return "Missing audit control";
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

        Set<String> reportedEndpoints =
                new HashSet<>();

        for (DataFlowEdge edge :
                context.dataFlows()) {

            if (edge.relation()
                    != DataFlowRelation.API_RESPONSE) {
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
                                                            edge.sourceFactId()
                                                    )
                            );

            if (!privacyField) {
                continue;
            }

            String endpointId =
                    edge.targetFactId();

            if (!reportedEndpoints.add(
                    endpointId
            )) {
                continue;
            }

            boolean auditControlExists =
                    context.facts()
                            .stream()
                            .filter(
                                    ApiAuditControlFact.class
                                            ::isInstance
                            )
                            .map(
                                    ApiAuditControlFact.class
                                            ::cast
                            )
                            .anyMatch(
                                    audit ->
                                            audit.endpointId()
                                                    .equals(
                                                            endpointId
                                                    )
                            );

            if (auditControlExists) {
                continue;
            }

            ApiEndpointFact endpoint =
                    context.facts()
                            .stream()
                            .filter(
                                    fact ->
                                            fact.id()
                                                    .equals(
                                                            endpointId
                                                    )
                            )
                            .filter(
                                    ApiEndpointFact.class
                                            ::isInstance
                            )
                            .map(
                                    ApiEndpointFact.class
                                            ::cast
                            )
                            .findFirst()
                            .orElse(null);

            if (endpoint == null) {
                continue;
            }

            List<Evidence> evidence =
                    List.of(
                            new Evidence(
                                    EvidenceType.DATA_FLOW,
                                    "Privacy API detected",
                                    endpoint.httpMethod()
                                            + " "
                                            + endpoint.path(),
                                    endpoint.location()
                            ),
                            new Evidence(
                                    EvidenceType.SOURCE_CODE,
                                    "Audit control",
                                    "No recognized audit control "
                                            + "was found for this endpoint.",
                                    endpoint.location()
                            )
                    );

            findings.add(
                    new Finding(
                            id(),
                            name(),
                            "Privacy API "
                                    + endpoint.httpMethod()
                                    + " "
                                    + endpoint.path()
                                    + " has no recognized "
                                    + "audit control.",
                            defaultSeverity(),
                            endpoint.location(),
                            evidence
                    )
            );
        }

        return findings;
    }
}