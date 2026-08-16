package io.github.privacyops.rule;

import io.github.privacyops.model.Finding;
import io.github.privacyops.model.Severity;

import java.util.List;

public interface PrivacyRule {

    String id();

    String name();

    Severity defaultSeverity();

    List<Finding> evaluate(RuleContext context);
}