package io.github.privacyops.classifier;

import io.github.privacyops.fact.Fact;
import io.github.privacyops.model.Classification;

import java.util.Optional;

public interface PrivacyClassifier {

    Optional<Classification> classify(Fact fact);
}