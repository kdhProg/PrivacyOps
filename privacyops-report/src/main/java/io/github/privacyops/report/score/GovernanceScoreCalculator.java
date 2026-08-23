package io.github.privacyops.report.score;

import io.github.privacyops.model.AnalysisResult;

public class GovernanceScoreCalculator {

    public GovernanceScore calculate(
            AnalysisResult result
    ) {

        boolean resourcePolicy =
                !hasFinding(
                        result,
                        "PRIV-POLICY-001"
                );

        /*
         * Policy 자체가 없는데 retention/disposal Finding이
         * 발생하지 않는 구조이므로 반드시 resourcePolicy와
         * 함께 판단한다.
         */
        boolean retention =
                resourcePolicy
                        && !hasFinding(
                        result,
                        "PRIV-RETENTION-001"
                );

        boolean disposal =
                resourcePolicy
                        && !hasFinding(
                        result,
                        "PRIV-DISPOSAL-001"
                );

        boolean accessControl =
                !hasFinding(
                        result,
                        "PRIV-ACCESS-001"
                );

        boolean auditControl =
                !hasFinding(
                        result,
                        "PRIV-AUDIT-001"
                );

        int score = 0;

        if (resourcePolicy) {
            score += 20;
        }

        if (retention) {
            score += 20;
        }

        if (disposal) {
            score += 20;
        }

        if (accessControl) {
            score += 20;
        }

        if (auditControl) {
            score += 20;
        }

        return new GovernanceScore(
                score,
                resourcePolicy,
                retention,
                disposal,
                accessControl,
                auditControl
        );
    }

    private boolean hasFinding(
            AnalysisResult result,
            String ruleId
    ) {

        return result.findings()
                .stream()
                .anyMatch(
                        finding ->
                                ruleId.equals(
                                        finding.ruleId()
                                )
                );
    }
}