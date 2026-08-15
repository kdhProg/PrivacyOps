package io.github.privacyops.analyzer.project;

import io.github.privacyops.fact.Fact;
import io.github.privacyops.scan.ArtifactScanner;
import io.github.privacyops.scan.ProjectScanner;
import io.github.privacyops.scan.ScanResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DefaultProjectScanner implements ProjectScanner {

    private final List<ArtifactScanner> scanners;

    public DefaultProjectScanner(List<ArtifactScanner> scanners) {
        this.scanners = scanners;
    }

    @Override
    public ScanResult scan(Path projectRoot) {

        List<Fact> facts = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (projectRoot == null || !Files.exists(projectRoot)) {
            throw new IllegalArgumentException(
                    "Project root does not exist: " + projectRoot
            );
        }

        try (var paths = Files.walk(projectRoot)) {

            paths
                    .filter(Files::isRegularFile)
                    .forEach(path ->
                            scanFile(path, facts, warnings)
                    );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to scan project: " + projectRoot,
                    e
            );
        }

        return new ScanResult(
                List.copyOf(facts),
                List.copyOf(warnings)
        );
    }

    private void scanFile(
            Path path,
            List<Fact> facts,
            List<String> warnings
    ) {

        scanners.stream()
                .filter(scanner -> scanner.supports(path))
                .forEach(scanner -> {
                    try {
                        facts.addAll(
                                scanner.scan(path)
                        );
                    } catch (RuntimeException e) {
                        warnings.add(
                                path + ": " + e.getMessage()
                        );
                    }
                });
    }
}