package io.github.privacyops.analyzer;

import io.github.privacyops.classifier.PrivacyClassifier;
import io.github.privacyops.fact.Fact;
import io.github.privacyops.model.ClassifiedFact;
import io.github.privacyops.scan.ProjectScanner;
import io.github.privacyops.scan.ScanResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PrivacyAnalysisService {

    private final ProjectScanner projectScanner;
    private final List<PrivacyClassifier> classifiers;

    public PrivacyAnalysisService(
            ProjectScanner projectScanner,
            List<PrivacyClassifier> classifiers
    ) {
        this.projectScanner = projectScanner;
        this.classifiers = classifiers;
    }

    public ScanResult scan(Path root) {
        return projectScanner.scan(root);
    }

    public List<ClassifiedFact> classify(
            List<Fact> facts
    ) {

        List<ClassifiedFact> results =
                new ArrayList<>();

        for (Fact fact : facts) {

            for (PrivacyClassifier classifier : classifiers) {

                classifier.classify(fact)
                        .ifPresent(
                                classification ->
                                        results.add(
                                                new ClassifiedFact(
                                                        fact,
                                                        classification
                                                )
                                        )
                        );
            }
        }

        return results;
    }
}