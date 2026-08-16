package io.github.privacyops.policy;

public record ResourcePolicy(
        String purpose,
        String retention,
        String disposal
) {
}