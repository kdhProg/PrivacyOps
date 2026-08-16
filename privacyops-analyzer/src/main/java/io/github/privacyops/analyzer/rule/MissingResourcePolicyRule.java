package io.github.privacyops.analyzer.rule;

import io.github.privacyops.fact.Fact;
import io.github.privacyops.fact.MapperColumnFact;

import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.flow.DataFlowRelation;

import io.github.privacyops.model.ClassifiedFact;
import io.github.privacyops.model.Finding;
import io.github.privacyops.model.Severity;

import io.github.privacyops.rule.PrivacyRule;
import io.github.privacyops.rule.RuleContext;

import java.util.*;

public class MissingResourcePolicyRule
        implements PrivacyRule {

    public static final String RULE_ID =
            "PRIV-POLICY-001";

    @Override
    public String id() {
        return RULE_ID;
    }

    @Override
    public String name() {
        return "Missing privacy resource policy";
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

            // MyBatis 컬럼 -> DTO 필드 흐름만 검사
            if (edge.relation()
                    != DataFlowRelation.MAPPER_RESULT) {

                continue;
            }

            // target DTO 필드가 개인정보인지 확인
            Optional<ClassifiedFact>
                    classifiedTarget =
                    context
                            .classifiedFacts()
                            .stream()
                            .filter(
                                    item ->
                                            item.fact()
                                                    .id()
                                                    .equals(
                                                            edge.targetFactId()
                                                    )
                            )
                            .findFirst();

            if (classifiedTarget.isEmpty()) {
                continue;
            }

            // source가 MapperColumnFact인지 확인
            Optional<MapperColumnFact>
                    column =
                    context
                            .facts()
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
                            .findFirst();

            if (column.isEmpty()) {
                continue;
            }

            String tableName =
                    column.get()
                            .tableName();

            // 이미 같은 테이블에 대해 Finding을 생성했다면 중복 방지
            if (!reportedTables.add(tableName)) {
                continue;
            }

            boolean policyExists =
                    context
                            .policy()
                            .resources()
                            .containsKey(
                                    tableName
                            );

            // 정책이 있으면 관리되고 있는 것으로 판단
            if (policyExists) {
                continue;
            }

            findings.add(
                    new Finding(
                            id(),
                            name(),
                            "Privacy resource "
                                    + tableName
                                    + " has no management policy.",
                            defaultSeverity(),
                            column.get()
                                    .location()
                    )
            );
        }

        return findings;
    }
}