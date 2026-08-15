package io.github.privacyops.rule;

import io.github.privacyops.model.Finding;

import java.util.List;

public interface PrivacyRule {

    String id();

    String name();

    List<Finding> evaluate();
}