package io.github.privacyops.scan;

import io.github.privacyops.fact.Fact;

import java.nio.file.Path;
import java.util.List;

public interface ArtifactScanner {

    boolean supports(Path path);

    List<Fact> scan(Path path);
}