package io.github.privacyops.analyzer.rule;

import io.github.privacyops.fact.ApiAuditControlFact;
import io.github.privacyops.fact.ApiEndpointFact;
import io.github.privacyops.fact.JavaFieldFact;
import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.flow.DataFlowRelation;
import io.github.privacyops.model.Classification;
import io.github.privacyops.model.ClassifiedFact;
import io.github.privacyops.model.PrivacyType;
import io.github.privacyops.policy.PrivacyPolicy;
import io.github.privacyops.rule.RuleContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MissingAuditControlRuleTest{
    @Test
    void createsFindingWhenPrivacyApiHasNoAuditControl() {

        JavaFieldFact rrn =
                new JavaFieldFact(
                        "field:MemberDto#rrn",
                        "samples.MemberDto",
                        "rrn",
                        "String",
                        null
                );

        ApiEndpointFact endpoint =
                new ApiEndpointFact(
                        "api:MemberController#getMember",
                        "GET",
                        "/members/{id}",
                        "samples.MemberController",
                        "getMember",
                        "MemberDto",
                        null
                );

        ClassifiedFact classified =
                new ClassifiedFact(
                        rrn,
                        new Classification(
                                PrivacyType.NATIONAL_IDENTIFIER,
                                0.95,
                                "test"
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
                        ),
                        PrivacyPolicy.empty()
                );

        MissingAuditControlRule rule =
                new MissingAuditControlRule();

        assertEquals(
                1,
                rule.evaluate(context)
                        .size()
        );


    }

    @Test
    void ignoresPrivacyApiWithAuditControl() {

        JavaFieldFact rrn =
                new JavaFieldFact(
                        "field:MemberDto#rrn",
                        "samples.MemberDto",
                        "rrn",
                        "String",
                        null
                );

        ApiEndpointFact endpoint =
                new ApiEndpointFact(
                        "api:MemberController#getMember",
                        "GET",
                        "/members/{id}",
                        "samples.MemberController",
                        "getMember",
                        "MemberDto",
                        null
                );

        ApiAuditControlFact audit =
                new ApiAuditControlFact(
                        "audit:"
                                + endpoint.id(),
                        endpoint.id(),
                        "PERSONAL_INFO_VIEW",
                        "PrivacyAudit",
                        null
                );

        ClassifiedFact classified =
                new ClassifiedFact(
                        rrn,
                        new Classification(
                                PrivacyType.NATIONAL_IDENTIFIER,
                                0.95,
                                "test"
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
                                endpoint,
                                audit
                        ),
                        List.of(
                                classified
                        ),
                        List.of(
                                edge
                        ),
                        PrivacyPolicy.empty()
                );

        MissingAuditControlRule rule =
                new MissingAuditControlRule();

        assertTrue(
                rule.evaluate(context)
                        .isEmpty()
        );
    }
}

