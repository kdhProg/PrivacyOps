package io.github.privacyops.analyzer.project;

import io.github.privacyops.analyzer.PrivacyAnalysisService;
import io.github.privacyops.analyzer.classifier.NamePatternPrivacyClassifier;
import io.github.privacyops.analyzer.java.JavaSourceScanner;
import io.github.privacyops.fact.JavaFieldFact;
import io.github.privacyops.model.ClassifiedFact;
import io.github.privacyops.model.PrivacyType;
import io.github.privacyops.scan.ScanResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultProjectScannerTest {

    @Test
    void scansAllJavaFilesInProject() throws Exception {

        Path projectRoot = Path.of(
                getClass()
                        .getClassLoader()
                        .getResource("project-sample")
                        .toURI()
        );

        DefaultProjectScanner scanner =
                new DefaultProjectScanner(
                        List.of(
                                new JavaSourceScanner()
                        )
                );

        ScanResult result =
                scanner.scan(projectRoot);

        List<JavaFieldFact> fields =
                result.facts().stream()
                        .filter(JavaFieldFact.class::isInstance)
                        .map(JavaFieldFact.class::cast)
                        .toList();

        assertTrue(
                fields.stream()
                        .anyMatch(
                                fact -> fact.fieldName().equals("rrn")
                        )
        );

        assertTrue(
                fields.stream()
                        .anyMatch(
                                fact -> fact.fieldName().equals("phoneNumber")
                        )
        );

        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void scansAndClassifiesWholeProject() throws Exception {

        Path root = Path.of(
                getClass()
                        .getClassLoader()
                        .getResource("project-sample")
                        .toURI()
        );

        DefaultProjectScanner projectScanner =
                new DefaultProjectScanner(
                        List.of(
                                new JavaSourceScanner()
                        )
                );

        PrivacyAnalysisService service =
                new PrivacyAnalysisService(
                        projectScanner,
                        List.of(
                                new NamePatternPrivacyClassifier()
                        )
                );

        ScanResult scanResult =
                service.scan(root);

        List<ClassifiedFact> classified =
                service.classify(
                        scanResult.facts()
                );

        classified.forEach(System.out::println);

        assertTrue(
                classified.stream()
                        .anyMatch(
                                item ->
                                        item.classification()
                                                .privacyType()
                                                == PrivacyType.NATIONAL_IDENTIFIER
                        )
        );
    }

}