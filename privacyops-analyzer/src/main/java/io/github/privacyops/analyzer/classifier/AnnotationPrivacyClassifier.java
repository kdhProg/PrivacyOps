package io.github.privacyops.analyzer.classifier;

import io.github.privacyops.fact.Fact;
import io.github.privacyops.fact.JavaFieldFact;
import io.github.privacyops.fact.PrivacyDataAnnotationFact;
import io.github.privacyops.model.Classification;
import io.github.privacyops.model.ClassifiedFact;
import io.github.privacyops.model.PrivacyType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnnotationPrivacyClassifier {

    public List<ClassifiedFact> classify(
            List<Fact> facts
    ) {

        List<ClassifiedFact> results =
                new ArrayList<>();

        List<PrivacyDataAnnotationFact> annotations =
                facts.stream()
                        .filter(
                                PrivacyDataAnnotationFact.class
                                        ::isInstance
                        )
                        .map(
                                PrivacyDataAnnotationFact.class
                                        ::cast
                        )
                        .toList();

        for (PrivacyDataAnnotationFact annotation :
                annotations) {

            JavaFieldFact field =
                    findTargetField(
                            facts,
                            annotation
                    );

            if (field == null) {
                continue;
            }

            PrivacyType privacyType =
                    resolvePrivacyType(
                            annotation.declaredType()
                    );

            if (privacyType == null) {
                continue;
            }

            Classification classification =
                    new Classification(
                            privacyType,
                            1.0,
                            "Explicit @PrivacyData declaration"
                    );

            results.add(
                    new ClassifiedFact(
                            field,
                            classification
                    )
            );
        }

        return results;
    }

    private JavaFieldFact findTargetField(
            List<Fact> facts,
            PrivacyDataAnnotationFact annotation
    ) {

        return facts.stream()
                .filter(
                        fact ->
                                fact.id()
                                        .equals(
                                                annotation.fieldFactId()
                                        )
                )
                .filter(
                        JavaFieldFact.class
                                ::isInstance
                )
                .map(
                        JavaFieldFact.class
                                ::cast
                )
                .findFirst()
                .orElse(null);
    }

    private PrivacyType resolvePrivacyType(
            String declaredType
    ) {

        if (declaredType == null
                || declaredType.isBlank()) {

            return null;
        }

        try {

            return PrivacyType.valueOf(
                    declaredType
                            .trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (IllegalArgumentException e) {

            return null;
        }
    }
}