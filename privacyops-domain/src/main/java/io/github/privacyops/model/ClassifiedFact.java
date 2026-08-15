package io.github.privacyops.model;

import io.github.privacyops.fact.Fact;

public record ClassifiedFact(
        Fact fact,
        Classification classification
) {
}