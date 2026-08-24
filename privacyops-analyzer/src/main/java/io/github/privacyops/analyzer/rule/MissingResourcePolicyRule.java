package io.github.privacyops.analyzer.rule;

import io.github.privacyops.fact.MapperColumnFact;
import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.flow.DataFlowRelation;
import io.github.privacyops.model.ClassifiedFact;
import io.github.privacyops.model.Evidence;
import io.github.privacyops.model.EvidenceType;
import io.github.privacyops.model.Finding;
import io.github.privacyops.model.Severity;
import io.github.privacyops.rule.PrivacyRule;
import io.github.privacyops.rule.RuleContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
            Optional<ClassifiedFact> classifiedTarget =
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
            Optional<MapperColumnFact> columnOptional =
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

            if (columnOptional.isEmpty()) {
                continue;
            }

            /*
             * Optional 검사가 끝났으므로
             * 실제 MapperColumnFact를 꺼내서 이후 사용
             */
            MapperColumnFact column =
                    columnOptional.get();

            String tableName =
                    column.tableName();

            // 이미 같은 테이블에 대해 Finding을 생성했다면 중복 방지
            if (!reportedTables.add(
                    tableName
            )) {
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

            List<Evidence> evidence =
                    List.of(
                            new Evidence(
                                    EvidenceType.DATA_FLOW,
                                    "Privacy resource detected",
                                    tableName
                                            + " contains data participating "
                                            + "in a detected privacy flow.",
                                    column.location()
                            ),
                            new Evidence(
                                    EvidenceType.POLICY,
                                    "Management policy",
                                    "No resource policy was found "
                                            + "for "
                                            + tableName
                                            + ".",
                                    column.location()
                            )
                    );

            findings.add(
                    new Finding(
                            id(),
                            name(),
                            "Privacy resource "
                                    + tableName
                                    + " has no management policy.",
                            defaultSeverity(),
                            column.location(),
                            evidence
                    )
            );
        }

        return findings;
    }
}