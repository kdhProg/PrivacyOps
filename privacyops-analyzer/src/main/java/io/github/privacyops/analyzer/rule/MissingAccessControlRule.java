package io.github.privacyops.analyzer.rule;

import io.github.privacyops.fact.ApiAccessControlFact;
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

public class MissingAccessControlRule
        implements PrivacyRule {

    public static final String RULE_ID =
            "PRIV-ACCESS-001";

    @Override
    public String id() {
        return RULE_ID;
    }

    @Override
    public String name() {
        return "Missing access control";
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

            // Source가 실제 개인정보로 분류됐는지 확인
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

            boolean accessControlExists =
                    context.facts()
                            .stream()
                            .filter(
                                    ApiAccessControlFact.class
                                            ::isInstance
                            )
                            .map(
                                    ApiAccessControlFact.class
                                            ::cast
                            )
                            .anyMatch(
                                    access ->
                                            access.endpointId()
                                                    .equals(
                                                            endpointId
                                                    )
                            );

            if (accessControlExists) {
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
                                    "Access control",
                                    "No recognized access control "
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
                                    + "access control.",
                            defaultSeverity(),
                            endpoint.location(),
                            evidence
                    )
            );
        }

        return findings;
    }
}