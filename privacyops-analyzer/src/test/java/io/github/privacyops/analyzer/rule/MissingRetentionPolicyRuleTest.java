package io.github.privacyops.analyzer.rule;

import io.github.privacyops.fact.JavaFieldFact;
import io.github.privacyops.fact.MapperColumnFact;
import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.flow.DataFlowRelation;
import io.github.privacyops.model.Classification;
import io.github.privacyops.model.ClassifiedFact;
import io.github.privacyops.model.PrivacyType;
import io.github.privacyops.policy.PrivacyPolicy;
import io.github.privacyops.policy.ResourcePolicy;
import io.github.privacyops.rule.RuleContext;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MissingRetentionPolicyRuleTest {

    @Test
    void createsFindingWhenRetentionIsMissing() {

        MapperColumnFact column =
                new MapperColumnFact(
                        "column:RRN",
                        "samples.MemberMapper.selectMember",
                        "TB_MEMBER",
                        "RRN",
                        "samples.MemberDto",
                        null
                );

        JavaFieldFact field =
                new JavaFieldFact(
                        "field:MemberDto#rrn",
                        "samples.MemberDto",
                        "rrn",
                        "String",
                        null
                );

        ClassifiedFact classified =
                new ClassifiedFact(
                        field,
                        new Classification(
                                PrivacyType.NATIONAL_IDENTIFIER,
                                0.95,
                                "test"
                        )
                );

        DataFlowEdge edge =
                new DataFlowEdge(
                        column.id(),
                        field.id(),
                        DataFlowRelation.MAPPER_RESULT
                );

        PrivacyPolicy policy =
                new PrivacyPolicy(
                        Map.of(
                                "TB_MEMBER",
                                new ResourcePolicy(
                                        "member-management",
                                        null,
                                        "scheduled-delete"
                                )
                        )
                );

        RuleContext context =
                new RuleContext(
                        List.of(
                                column,
                                field
                        ),
                        List.of(
                                classified
                        ),
                        List.of(
                                edge
                        ),
                        policy
                );

        MissingRetentionPolicyRule rule =
                new MissingRetentionPolicyRule();

        assertEquals(
                1,
                rule.evaluate(context)
                        .size()
        );
    }

    @Test
    void ignoresResourceWithRetentionPolicy() {

        MapperColumnFact column =
                new MapperColumnFact(
                        "column:RRN",
                        "samples.MemberMapper.selectMember",
                        "TB_MEMBER",
                        "RRN",
                        "samples.MemberDto",
                        null
                );

        JavaFieldFact field =
                new JavaFieldFact(
                        "field:MemberDto#rrn",
                        "samples.MemberDto",
                        "rrn",
                        "String",
                        null
                );

        ClassifiedFact classified =
                new ClassifiedFact(
                        field,
                        new Classification(
                                PrivacyType.NATIONAL_IDENTIFIER,
                                0.95,
                                "test"
                        )
                );

        DataFlowEdge edge =
                new DataFlowEdge(
                        column.id(),
                        field.id(),
                        DataFlowRelation.MAPPER_RESULT
                );

        PrivacyPolicy policy =
                new PrivacyPolicy(
                        Map.of(
                                "TB_MEMBER",
                                new ResourcePolicy(
                                        "member-management",
                                        "3y",
                                        "scheduled-delete"
                                )
                        )
                );

        RuleContext context =
                new RuleContext(
                        List.of(
                                column,
                                field
                        ),
                        List.of(
                                classified
                        ),
                        List.of(
                                edge
                        ),
                        policy
                );

        MissingRetentionPolicyRule rule =
                new MissingRetentionPolicyRule();

        assertTrue(
                rule.evaluate(context)
                        .isEmpty()
        );
    }
}