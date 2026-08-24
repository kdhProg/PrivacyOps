package io.github.privacyops.analyzer.classifier;

import io.github.privacyops.classifier.PrivacyClassifier;
import io.github.privacyops.fact.DatabaseColumnFact;
import io.github.privacyops.fact.Fact;
import io.github.privacyops.fact.JavaFieldFact;
import io.github.privacyops.fact.MapperColumnFact;
import io.github.privacyops.model.Classification;
import io.github.privacyops.risk.KeywordRiskRule;
import io.github.privacyops.risk.RiskProfile;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class RiskProfilePrivacyClassifier
        implements PrivacyClassifier {

    private final RiskProfile riskProfile;

    public RiskProfilePrivacyClassifier(
            RiskProfile riskProfile
    ) {

        if (riskProfile == null) {

            throw new IllegalArgumentException(
                    "Risk profile must not be null."
            );
        }

        this.riskProfile =
                riskProfile;
    }

    @Override
    public Optional<Classification> classify(
            Fact fact
    ) {

        if (fact == null) {
            return Optional.empty();
        }

        /*
         * Java DTO / Entity 등의 필드
         */
        if (fact instanceof JavaFieldFact field) {

            return classifyName(
                    field.fieldName()
            );
        }

        /*
         * 실제 JDBC Database Metadata 컬럼
         */
        if (fact instanceof DatabaseColumnFact column) {

            return classifyName(
                    column.columnName()
            );
        }

        /*
         * MyBatis SQL에서 추출된 컬럼
         */
        if (fact instanceof MapperColumnFact column) {

            return classifyName(
                    column.columnName()
            );
        }

        return Optional.empty();
    }

    private Optional<Classification> classifyName(
            String name
    ) {

        if (name == null
                || name.isBlank()) {

            return Optional.empty();
        }

        for (Map.Entry<String, KeywordRiskRule> entry :
                riskProfile
                        .keywords()
                        .entrySet()) {

            String configuredKeyword =
                    entry.getKey();

            KeywordRiskRule rule =
                    entry.getValue();

            if (!matchesKeyword(
                    name,
                    configuredKeyword
            )) {
                continue;
            }

            return Optional.of(
                    new Classification(
                            rule.type(),
                            1.0,
                            "custom risk profile keyword: "
                                    + configuredKeyword
                                    + " (weight="
                                    + rule.weight()
                                    + ")"
                    )
            );
        }

        return Optional.empty();
    }

    private boolean matchesKeyword(
            String candidate,
            String configuredKeyword
    ) {

        if (candidate == null
                || configuredKeyword == null) {

            return false;
        }

        String candidateNormalized =
                normalize(candidate);

        String keywordNormalized =
                normalize(configuredKeyword);

        /*
         * 완전 일치
         *
         * employee_no
         * ↔ employee_no
         *
         * personalId
         * ↔ personal_id
         */
        if (candidateNormalized.equals(
                keywordNormalized
        )) {

            return true;
        }

        /*
         * camelCase / snake_case / kebab-case 등을
         * 단어 단위로 분리해서 비교한다.
         *
         * 예:
         *
         * mobileTel
         * → mobile / tel
         *
         * keyword = tel
         * → match
         */
        String[] candidateTokens =
                tokenize(candidate);

        String[] keywordTokens =
                tokenize(configuredKeyword);

        /*
         * keyword가 단일 단어인 경우
         * token 단위 일치 허용
         *
         * tel
         * → mobileTel의 tel과 일치
         *
         * 단순 contains()를 쓰지 않으므로
         * hotel 등에 tel이 들어있다고
         * 잘못 탐지하는 문제를 줄인다.
         */
        if (keywordTokens.length == 1) {

            String keywordToken =
                    keywordTokens[0];

            return Arrays.stream(
                            candidateTokens
                    )
                    .anyMatch(
                            token ->
                                    token.equals(
                                            keywordToken
                                    )
                    );
        }

        /*
         * 여러 단어로 구성된 keyword는
         * normalize 결과의 완전 일치를 기본으로 한다.
         *
         * 예:
         *
         * personal_id
         * personalId
         *
         * → 둘 다 personalid
         */
        return false;
    }

    private String normalize(
            String value
    ) {

        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll(
                        "[^a-z0-9]",
                        ""
                );
    }

    private String[] tokenize(
            String value
    ) {

        /*
         * camelCase를 먼저 분리
         *
         * mobileTel
         * → mobile Tel
         */
        String camelSeparated =
                value.replaceAll(
                        "([a-z0-9])([A-Z])",
                        "$1 $2"
                );

        /*
         * snake_case, kebab-case 등의 구분자를
         * 공백으로 변경
         */
        String cleaned =
                camelSeparated
                        .toLowerCase(Locale.ROOT)
                        .replaceAll(
                                "[^a-z0-9]+",
                                " "
                        )
                        .trim();

        if (cleaned.isBlank()) {
            return new String[0];
        }

        return cleaned.split(
                "\\s+"
        );
    }
}