package io.github.privacyops.report.score;

public record GovernanceScore(
        int score,
        boolean resourcePolicy,
        boolean retention,
        boolean disposal,
        boolean accessControl,
        boolean auditControl
) {
}