package io.github.privacyops.fact;

import io.github.privacyops.model.SourceLocation;

public interface Fact {

    String id();

    SourceLocation location();
}