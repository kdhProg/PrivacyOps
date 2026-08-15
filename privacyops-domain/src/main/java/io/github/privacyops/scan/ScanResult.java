package io.github.privacyops.scan;

import io.github.privacyops.fact.Fact;

import java.util.List;

public record ScanResult(
        List<Fact> facts,
        List<String> warnings
) {
}