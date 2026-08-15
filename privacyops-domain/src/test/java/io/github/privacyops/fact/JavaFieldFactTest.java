package io.github.privacyops.fact;

import io.github.privacyops.model.SourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaFieldFactTest {

    @Test
    void createsJavaFieldFact() {

        JavaFieldFact fact = new JavaFieldFact(
                "field-1",
                "MemberDto",
                "rrn",
                "String",
                new SourceLocation(
                        "MemberDto.java",
                        10
                )
        );

        assertEquals("rrn", fact.fieldName());
        assertEquals("MemberDto", fact.className());
    }
}