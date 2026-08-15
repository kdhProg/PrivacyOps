package io.github.privacyops.cli;

import io.github.privacyops.analyzer.PrivacyAnalysisService;
import io.github.privacyops.analyzer.classifier.NamePatternPrivacyClassifier;
import io.github.privacyops.analyzer.java.JavaSourceScanner;
import io.github.privacyops.analyzer.project.DefaultProjectScanner;

import io.github.privacyops.model.ClassifiedFact;
import io.github.privacyops.model.PrivacyType;
import io.github.privacyops.scan.ScanResult;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(
        name = "scan",
        description = "Scan a project for privacy-related data.",
        mixinStandardHelpOptions = true
)
public class ScanCommand implements Callable<Integer> {

    @Parameters(
            index = "0",
            paramLabel = "<project-path>",
            description = "Project directory to scan."
    )
    private Path projectPath;

    @Override
    public Integer call() {

        if (!Files.isDirectory(projectPath)) {

            System.err.println(
                    "Project path does not exist or is not a directory: "
                            + projectPath
            );

            return 2;
        }

        DefaultProjectScanner projectScanner =
                new DefaultProjectScanner(
                        List.of(
                                new JavaSourceScanner()
                        )
                );

        PrivacyAnalysisService analysisService =
                new PrivacyAnalysisService(
                        projectScanner,
                        List.of(
                                new NamePatternPrivacyClassifier()
                        )
                );

        ScanResult scanResult =
                analysisService.scan(projectPath);

        List<ClassifiedFact> classifiedFacts =
                analysisService.classify(
                        scanResult.facts()
                );

        printSummary(
                projectPath,
                scanResult,
                classifiedFacts
        );

        return 0;
    }

    private void printSummary(
            Path projectPath,
            ScanResult scanResult,
            List<ClassifiedFact> classifiedFacts
    ) {

        System.out.println();
        System.out.println("PrivacyOps 0.1.0");
        System.out.println();
        System.out.println(
                "Project: "
                        + projectPath.toAbsolutePath()
        );

        System.out.println();
        System.out.println("Scan Summary");
        System.out.println(
                "--------------------------------"
        );

        System.out.printf(
                "%-22s : %d%n",
                "Facts",
                scanResult.facts().size()
        );

        System.out.printf(
                "%-22s : %d%n",
                "Privacy Candidates",
                classifiedFacts.size()
        );

        System.out.printf(
                "%-22s : %d%n",
                "Warnings",
                scanResult.warnings().size()
        );

        printPrivacyTypes(classifiedFacts);

        printWarnings(scanResult);
    }

    private void printPrivacyTypes(
            List<ClassifiedFact> classifiedFacts
    ) {

        Map<PrivacyType, Long> counts =
                new EnumMap<>(PrivacyType.class);

        for (ClassifiedFact classified : classifiedFacts) {

            PrivacyType type =
                    classified
                            .classification()
                            .privacyType();

            counts.merge(type, 1L, Long::sum);
        }

        System.out.println();
        System.out.println("Privacy Types");
        System.out.println(
                "--------------------------------"
        );

        counts.forEach(
                (type, count) ->
                        System.out.printf(
                                "%-22s : %d%n",
                                type,
                                count
                        )
        );
    }

    private void printWarnings(
            ScanResult scanResult
    ) {

        if (scanResult.warnings().isEmpty()) {
            return;
        }

        System.out.println();
        System.out.println("Warnings");
        System.out.println(
                "--------------------------------"
        );

        scanResult.warnings()
                .forEach(
                        warning ->
                                System.out.println(
                                        "- " + warning
                                )
                );
    }
}