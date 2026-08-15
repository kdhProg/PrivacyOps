package io.github.privacyops.analyzer.classifier;

import io.github.privacyops.fact.DatabaseColumnFact;
import io.github.privacyops.fact.JavaFieldFact;
import io.github.privacyops.model.Classification;
import io.github.privacyops.model.PrivacyType;
import io.github.privacyops.model.SourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NamePatternPrivacyClassifierTest {

    private final NamePatternPrivacyClassifier classifier =
            new NamePatternPrivacyClassifier();

    @Test
    void classifiesRrnField() {

        JavaFieldFact fact = new JavaFieldFact(
                "field-1",
                "MemberDto",
                "rrn",
                "String",
                new SourceLocation("MemberDto.java", 10)
        );

        Optional<Classification> result = classifier.classify(fact);

        assertTrue(result.isPresent());
        assertEquals(
                PrivacyType.NATIONAL_IDENTIFIER,
                result.get().privacyType()
        );
    }

    @Test
    void classifiesEmailField() {

        JavaFieldFact fact = new JavaFieldFact(
                "field-2",
                "MemberDto",
                "emailAddress",
                "String",
                new SourceLocation("MemberDto.java", 20)
        );

        Optional<Classification> result = classifier.classify(fact);

        assertTrue(result.isPresent());
        assertEquals(
                PrivacyType.EMAIL,
                result.get().privacyType()
        );
    }

    @Test
    void ignoresNonPrivacyField() {

        JavaFieldFact fact = new JavaFieldFact(
                "field-3",
                "MemberDto",
                "createdAt",
                "LocalDateTime",
                new SourceLocation("MemberDto.java", 30)
        );

        Optional<Classification> result = classifier.classify(fact);

        assertTrue(result.isEmpty());
    }

    @Test
    void classifiesDatabaseColumn() {

        DatabaseColumnFact fact = new DatabaseColumnFact(
                "db-1",
                "APP",
                "TB_MEMBER",
                "PHONE_NO",
                "VARCHAR2",
                "휴대전화번호",
                null
        );

        Optional<Classification> result = classifier.classify(fact);

        assertTrue(result.isPresent());
        assertEquals(
                PrivacyType.PHONE_NUMBER,
                result.get().privacyType()
        );
    }

}