package io.github.privacyops.policy;

import java.nio.file.Path;

public interface PolicyProvider {

    PrivacyPolicy load(Path path);
}