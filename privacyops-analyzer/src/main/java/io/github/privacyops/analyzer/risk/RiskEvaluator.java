package io.github.privacyops.analyzer.risk;

import io.github.privacyops.model.ClassifiedFact;
import io.github.privacyops.risk.RiskAssessment;
import io.github.privacyops.risk.RiskProfile;

import java.util.List;

public class RiskEvaluator {

    public List<RiskAssessment> evaluate(
            List<ClassifiedFact> classifiedFacts,
            RiskProfile profile
    ) {

        return classifiedFacts
                .stream()
                .map(
                        classified -> {

                            int weight =
                                    profile.weightOf(
                                            classified
                                                    .classification()
                                                    .privacyType()
                                    );

                            return new RiskAssessment(
                                    classified.fact().id(),
                                    classified
                                            .classification()
                                            .privacyType(),
                                    weight,
                                    levelOf(weight),
                                    "Privacy type weight"
                            );
                        }
                )
                .toList();
    }

    private String levelOf(
            int weight
    ) {

        if (weight >= 8) {
            return "CRITICAL";
        }

        if (weight >= 5) {
            return "HIGH";
        }

        if (weight >= 3) {
            return "MEDIUM";
        }

        return "LOW";
    }
}