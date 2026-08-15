package io.github.privacyops.scan;

import java.nio.file.Path;

public interface ProjectScanner {

    ScanResult scan(Path projectRoot);
}