package io.github.privacyops.analyzer.rule;

import io.github.privacyops.fact.ApiEndpointFact;
import io.github.privacyops.fact.Fact;
import io.github.privacyops.fact.JavaFieldFact;

import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.flow.DataFlowRelation;

import io.github.privacyops.model.Classification;
import io.github.privacyops.model.ClassifiedFact;
import io.github.privacyops.model.Finding;
import io.github.privacyops.model.PrivacyType;
import io.github.privacyops.model.Severity;
import io.github.privacyops.model.SourceLocation;

import io.github.privacyops.rule.RuleContext;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiPrivacyExposureRuleTest {

    @Test
    void createsCriticalFindingForNationalIdentifier()
    {

        JavaFieldFact rrn =
                new JavaFieldFact(
                        "field:MemberDto#rrn",
                        "com.example.MemberDto",
                        "rrn",
                        "String",
                        new SourceLocation(
                                "MemberDto.java",
                                10
                        )
                );

        ApiEndpointFact endpoint =
                new ApiEndpointFact(
                        "api:MemberController#getMember",
                        "GET",
                        "/members/{id}",
                        "com.example.MemberController",
                        "getMember",
                        "MemberDto",
                        new SourceLocation(
                                "MemberController.java",
                                20
                        )
                );

        ClassifiedFact classified =
                new ClassifiedFact(
                        rrn,
                        new Classification(
                                PrivacyType.NATIONAL_IDENTIFIER,
                                0.95,
                                "Matched rrn pattern"
                        )
                );

        DataFlowEdge edge =
                new DataFlowEdge(
                        rrn.id(),
                        endpoint.id(),
                        DataFlowRelation.API_RESPONSE
                );

        RuleContext context =
                new RuleContext(
                        List.of(
                                rrn,
                                endpoint
                        ),
                        List.of(
                                classified
                        ),
                        List.of(
                                edge
                        )
                );

        ApiPrivacyExposureRule rule =
                new ApiPrivacyExposureRule();

        List<Finding> findings =
                rule.evaluate(context);

        assertEquals(
                1,
                findings.size()
        );

        Finding finding =
                findings.get(0);

        assertEquals(
                "PRIV-API-001",
                finding.ruleId()
        );

        assertEquals(
                Severity.CRITICAL,
                finding.severity()
        );

        assertTrue(
                finding.description()
                        .contains(
                                "/members/{id}"
                        )
        );
    }

    @Test
    void ignoresUnclassifiedField() {

        JavaFieldFact createdAt =
                new JavaFieldFact(
                        "field:MemberDto#createdAt",
                        "com.example.MemberDto",
                        "createdAt",
                        "LocalDateTime",
                        new SourceLocation(
                                "MemberDto.java",
                                15
                        )
                );

        ApiEndpointFact endpoint =
                new ApiEndpointFact(
                        "api:MemberController#getMember",
                        "GET",
                        "/members/{id}",
                        "com.example.MemberController",
                        "getMember",
                        "MemberDto",
                        new SourceLocation(
                                "MemberController.java",
                                20
                        )
                );

        DataFlowEdge edge =
                new DataFlowEdge(
                        createdAt.id(),
                        endpoint.id(),
                        DataFlowRelation.API_RESPONSE
                );

        RuleContext context =
                new RuleContext(
                        List.of(
                                createdAt,
                                endpoint
                        ),
                        List.of(),
                        List.of(
                                edge
                        )
                );

        ApiPrivacyExposureRule rule =
                new ApiPrivacyExposureRule();

        List<Finding> findings =
                rule.evaluate(context);

        assertTrue(
                findings.isEmpty()
        );
    }

}