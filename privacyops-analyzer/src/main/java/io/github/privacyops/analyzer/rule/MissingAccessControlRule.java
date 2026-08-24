package io.github.privacyops.analyzer.rule;

import io.github.privacyops.access.AccessControlAssessment;
import io.github.privacyops.access.AccessControlProvider;
import io.github.privacyops.analyzer.access.SpringSecurityAccessControlProvider;
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

    private final List<AccessControlProvider>
            accessControlProviders;

    /**
     * 기존 코드 및 테스트와의 호환성을 위한
     * 기본 생성자.
     *
     * 기본적으로 Spring Security Provider를 사용한다.
     */
    public MissingAccessControlRule() {

        this(
                List.of(
                        new SpringSecurityAccessControlProvider()
                )
        );
    }

    /**
     * 외부 Provider를 주입할 수 있는 생성자.
     */
    public MissingAccessControlRule(
            List<AccessControlProvider>
                    accessControlProviders
    ) {

        this.accessControlProviders =
                accessControlProviders == null
                        ? List.of()
                        : List.copyOf(
                        accessControlProviders
                );
    }

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

            /*
             * 개인정보가 API Response로
             * 이동하는 Flow만 평가한다.
             */
            if (edge.relation()
                    != DataFlowRelation.API_RESPONSE) {

                continue;
            }

            /*
             * Source가 실제 개인정보로
             * 분류되어 있는지 확인한다.
             */
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

            /*
             * 개인정보 필드가 여러 개여도
             * 동일 endpoint에 Finding을
             * 한 번만 생성한다.
             */
            if (!reportedEndpoints.add(
                    endpointId
            )) {

                continue;
            }

            ApiEndpointFact endpoint =
                    findEndpoint(
                            context,
                            endpointId
                    );

            if (endpoint == null) {
                continue;
            }

            List<AccessControlAssessment>
                    assessments =
                    inspectAccessControls(
                            endpoint,
                            context
                    );

            /*
             * Provider 중 하나라도
             * 접근통제를 확인하면 통과한다.
             */
            boolean controlled =
                    assessments
                            .stream()
                            .anyMatch(
                                    AccessControlAssessment
                                            ::controlled
                            );

            if (controlled) {
                continue;
            }

            List<Evidence> evidence =
                    new ArrayList<>();

            /*
             * 개인정보 API 자체가 존재한다는 Evidence
             */
            evidence.add(
                    new Evidence(
                            EvidenceType.DATA_FLOW,
                            "Privacy API detected",
                            endpoint.httpMethod()
                                    + " "
                                    + endpoint.path(),
                            endpoint.location()
                    )
            );

            /*
             * 각 Provider의 판정 근거를
             * Finding Evidence에 결합한다.
             */
            for (AccessControlAssessment assessment :
                    assessments) {

                evidence.addAll(
                        assessment.evidence()
                );
            }

            /*
             * Provider 자체가 하나도 평가하지 못한 경우
             */
            if (assessments.isEmpty()) {

                evidence.add(
                        new Evidence(
                                EvidenceType.EXTERNAL_PROVIDER,
                                "Access control provider",
                                "No access control provider "
                                        + "could evaluate this endpoint.",
                                endpoint.location()
                        )
                );
            }

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

    private ApiEndpointFact findEndpoint(
            RuleContext context,
            String endpointId
    ) {

        return context.facts()
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
    }

    private List<AccessControlAssessment>
    inspectAccessControls(
            ApiEndpointFact endpoint,
            RuleContext context
    ) {

        List<AccessControlAssessment>
                assessments =
                new ArrayList<>();

        for (AccessControlProvider provider :
                accessControlProviders) {

            if (!provider.supports(
                    endpoint,
                    context
            )) {

                continue;
            }

            AccessControlAssessment assessment =
                    provider.inspect(
                            endpoint,
                            context
                    );

            if (assessment != null) {

                assessments.add(
                        assessment
                );
            }
        }

        return assessments;
    }
}