package io.github.privacyops.analyzer.classifier;

import io.github.privacyops.classifier.PrivacyClassifier;
import io.github.privacyops.fact.DatabaseColumnFact;
import io.github.privacyops.fact.Fact;
import io.github.privacyops.fact.JavaFieldFact;
import io.github.privacyops.model.Classification;
import io.github.privacyops.model.PrivacyType;

import java.util.Locale;
import java.util.Optional;

public class NamePatternPrivacyClassifier implements PrivacyClassifier {

    @Override
    public Optional<Classification> classify(Fact fact) {

        if (fact instanceof JavaFieldFact fieldFact) {
            return classifyName(fieldFact.fieldName());
        }

        if (fact instanceof DatabaseColumnFact columnFact) {
            return classifyName(columnFact.columnName());
        }

        return Optional.empty();
    }

    private Optional<Classification> classifyName(String name) {

        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        String normalized = normalize(name);

        if (matchesNationalIdentifier(normalized)) {
            return Optional.of(new Classification(
                    PrivacyType.NATIONAL_IDENTIFIER,
                    0.95,
                    "Matched national identifier naming pattern: " + name
            ));
        }

        if (matchesEmail(normalized)) {
            return Optional.of(new Classification(
                    PrivacyType.EMAIL,
                    0.90,
                    "Matched email naming pattern: " + name
            ));
        }

        if (matchesPhone(normalized)) {
            return Optional.of(new Classification(
                    PrivacyType.PHONE_NUMBER,
                    0.90,
                    "Matched phone number naming pattern: " + name
            ));
        }

        if (matchesName(normalized)) {
            return Optional.of(new Classification(
                    PrivacyType.NAME,
                    0.70,
                    "Matched personal name naming pattern: " + name
            ));
        }

        return Optional.empty();
    }

    private String normalize(String value) {
        return value
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "");
    }

    private boolean matchesNationalIdentifier(String value) {
        return value.contains("rrn")
                || value.contains("jumin")
                || value.contains("residentnumber")
                || value.contains("residentno")
                || value.contains("regno");
    }

    private boolean matchesEmail(String value) {
        return value.contains("email")
                || value.contains("mailaddr")
                || value.contains("emailaddr");
    }

    private boolean matchesPhone(String value) {
        return value.contains("phone")
                || value.contains("mobile")
                || value.contains("telno")
                || value.contains("phoneno");
    }

    private boolean matchesName(String value) {
        return value.equals("name")
                || value.equals("username")
                || value.equals("membername")
                || value.equals("custname");
    }
}