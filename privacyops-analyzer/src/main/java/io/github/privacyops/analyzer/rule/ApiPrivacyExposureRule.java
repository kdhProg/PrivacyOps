package io.github.privacyops.analyzer.rule;

import io.github.privacyops.fact.ApiEndpointFact;
import io.github.privacyops.fact.Fact;
import io.github.privacyops.fact.JavaFieldFact;

import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.flow.DataFlowRelation;

import io.github.privacyops.model.*;

import io.github.privacyops.rule.PrivacyRule;
import io.github.privacyops.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ApiPrivacyExposureRule
        implements PrivacyRule {

    public static final String RULE_ID =
            "PRIV-API-001";

    @Override
    public String id() {
        return RULE_ID;
    }

    @Override
    public String name() {
        return "Personal data in API response";
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

        for (DataFlowEdge edge :
                context.dataFlows()) {

            if (edge.relation()
                    != DataFlowRelation.API_RESPONSE) {

                continue;
            }

            Optional<ClassifiedFact> classified =
                    findClassification(
                            context,
                            edge.sourceFactId()
                    );

            if (classified.isEmpty()) {
                continue;
            }

            Optional<JavaFieldFact> field =
                    findField(
                            context,
                            edge.sourceFactId()
                    );

            Optional<ApiEndpointFact> endpoint =
                    findEndpoint(
                            context,
                            edge.targetFactId()
                    );

            if (field.isEmpty()
                    || endpoint.isEmpty()) {

                continue;
            }

            ClassifiedFact classifiedFact =
                    classified.get();

            JavaFieldFact fieldFact =
                    field.get();

            ApiEndpointFact endpointFact =
                    endpoint.get();

            Severity severity =
                    resolveSeverity(
                            classifiedFact
                                    .classification()
                                    .privacyType()
                    );

            String description =
                    buildDescription(
                            fieldFact,
                            endpointFact,
                            classifiedFact
                    );

            /*
             * Evidence 1:
             * 개인정보로 분류된 Java 필드 자체
             */
            Evidence privacyFieldEvidence =
                    new Evidence(
                            EvidenceType.DATA_FLOW,
                            "Privacy field detected",
                            fieldFact.className()
                                    + "."
                                    + fieldFact.fieldName()
                                    + " ["
                                    + classifiedFact
                                    .classification()
                                    .privacyType()
                                    + "]",
                            fieldFact.location()
                    );

            /*
             * Evidence 2:
             * 해당 개인정보가 연결된 API Endpoint
             */
            Evidence apiEndpointEvidence =
                    new Evidence(
                            EvidenceType.SOURCE_CODE,
                            "API endpoint",
                            endpointFact.httpMethod()
                                    + " "
                                    + endpointFact.path(),
                            endpointFact.location()
                    );

            /*
             * Evidence가 포함된 Finding 생성
             */
            findings.add(
                    new Finding(
                            id(),
                            name(),
                            description,
                            severity,
                            endpointFact.location(),
                            List.of(
                                    privacyFieldEvidence,
                                    apiEndpointEvidence
                            )
                    )
            );
        }

        return findings;
    }

    private Optional<ClassifiedFact>
    findClassification(
            RuleContext context,
            String factId
    ) {

        return context
                .classifiedFacts()
                .stream()
                .filter(
                        item ->
                                item.fact()
                                        .id()
                                        .equals(factId)
                )
                .findFirst();
    }

    private Optional<JavaFieldFact>
    findField(
            RuleContext context,
            String factId
    ) {

        return context
                .facts()
                .stream()
                .filter(
                        fact ->
                                fact.id()
                                        .equals(factId)
                )
                .filter(
                        JavaFieldFact.class
                                ::isInstance
                )
                .map(
                        JavaFieldFact.class
                                ::cast
                )
                .findFirst();
    }

    private Optional<ApiEndpointFact>
    findEndpoint(
            RuleContext context,
            String factId
    ) {

        return context
                .facts()
                .stream()
                .filter(
                        fact ->
                                fact.id()
                                        .equals(factId)
                )
                .filter(
                        ApiEndpointFact.class
                                ::isInstance
                )
                .map(
                        ApiEndpointFact.class
                                ::cast
                )
                .findFirst();
    }

    private Severity resolveSeverity(
            PrivacyType privacyType
    ) {

        return switch (privacyType) {

            case NATIONAL_IDENTIFIER,
                 FINANCIAL_ACCOUNT ->
                    Severity.CRITICAL;

            case EMAIL,
                 PHONE_NUMBER,
                 ADDRESS,
                 BIRTH_DATE ->
                    Severity.HIGH;

            case NAME ->
                    Severity.MEDIUM;

            default ->
                    defaultSeverity();
        };
    }

    private String buildDescription(
            JavaFieldFact field,
            ApiEndpointFact endpoint,
            ClassifiedFact classified
    ) {

        return String.format(
                "Privacy field %s.%s (%s) "
                        + "may be exposed through "
                        + "%s %s.",
                field.className(),
                field.fieldName(),
                classified
                        .classification()
                        .privacyType(),
                endpoint.httpMethod(),
                endpoint.path()
        );
    }
}