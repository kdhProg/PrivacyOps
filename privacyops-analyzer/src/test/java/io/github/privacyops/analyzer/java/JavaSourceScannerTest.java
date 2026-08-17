package io.github.privacyops.analyzer.java;

import io.github.privacyops.analyzer.classifier.NamePatternPrivacyClassifier;
import io.github.privacyops.fact.Fact;
import io.github.privacyops.fact.JavaFieldFact;

import io.github.privacyops.fact.PrivacyDataAnnotationFact;
import io.github.privacyops.model.Classification;
import io.github.privacyops.model.PrivacyType;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JavaSourceScannerTest {

    private final JavaSourceScanner scanner =
            new JavaSourceScanner();

    @Test
    void extractsFieldsFromJavaSource() throws Exception {

        Path path = Path.of(
                getClass()
                        .getClassLoader()
                        .getResource("samples/MemberDto.java")
                        .toURI()
        );

        List<Fact> facts =
                scanner.scan(path);

        assertEquals(5, facts.size());

        List<JavaFieldFact> fields =
                facts.stream()
                        .filter(JavaFieldFact.class::isInstance)
                        .map(JavaFieldFact.class::cast)
                        .toList();

        assertTrue(
                fields.stream()
                        .anyMatch(
                                fact ->
                                        fact.fieldName()
                                                .equals("rrn")
                        )
        );

        assertTrue(
                fields.stream()
                        .anyMatch(
                                fact ->
                                        fact.fieldName()
                                                .equals("emailAddress")
                        )
        );
    }

    @Test
    void scansAndClassifiesPrivacyFields() throws Exception {

        Path path = Path.of(
                getClass()
                        .getClassLoader()
                        .getResource("samples/MemberDto.java")
                        .toURI()
        );

        JavaSourceScanner scanner =
                new JavaSourceScanner();

        NamePatternPrivacyClassifier classifier =
                new NamePatternPrivacyClassifier();

        List<Fact> facts =
                scanner.scan(path);

        List<Classification> classifications =
                facts.stream()
                        .map(classifier::classify)
                        .flatMap(Optional::stream)
                        .toList();

        assertTrue(
                classifications.stream()
                        .anyMatch(
                                result ->
                                        result.privacyType()
                                                == PrivacyType.NATIONAL_IDENTIFIER
                        )
        );

        assertTrue(
                classifications.stream()
                        .anyMatch(
                                result ->
                                        result.privacyType()
                                                == PrivacyType.EMAIL
                        )
        );

        assertTrue(
                classifications.stream()
                        .anyMatch(
                                result ->
                                        result.privacyType()
                                                == PrivacyType.PHONE_NUMBER
                        )
        );
    }

    @Test
    void supportsJavaFilesOnly() {

        assertTrue(
                scanner.supports(
                        Path.of("MemberDto.java")
                )
        );

        assertFalse(
                scanner.supports(
                        Path.of("MemberMapper.xml")
                )
        );
    }

    @Test
    void extractsPrivacyDataAnnotation()
            throws Exception {

        Path path =
                Path.of(
                        getClass()
                                .getClassLoader()
                                .getResource(
                                        "java/AnnotatedPrivacyDto.java"
                                )
                                .toURI()
                );

        List<Fact> facts =
                scanner.scan(path);

        PrivacyDataAnnotationFact annotation =
                facts.stream()
                        .filter(
                                PrivacyDataAnnotationFact.class
                                        ::isInstance
                        )
                        .map(
                                PrivacyDataAnnotationFact.class
                                        ::cast
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                "NATIONAL_IDENTIFIER",
                annotation.declaredType()
        );

        JavaFieldFact secretField =
                facts.stream()
                        .filter(
                                JavaFieldFact.class
                                        ::isInstance
                        )
                        .map(
                                JavaFieldFact.class
                                        ::cast
                        )
                        .filter(
                                field ->
                                        field.fieldName()
                                                .equals(
                                                        "secretValue"
                                                )
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                secretField.id(),
                annotation.fieldFactId()
        );
    }

    @Test
    void extractsAnnotatedPrivacyFieldFromProjectSample()
            throws Exception {

        Path path =
                Path.of(
                        getClass()
                                .getClassLoader()
                                .getResource(
                                        "project-sample/"
                                                + "src/main/java/"
                                                + "com/example/"
                                                + "MemberDto.java"
                                )
                                .toURI()
                );

        List<Fact> facts =
                scanner.scan(path);

        facts.forEach(
                fact ->
                        System.out.println(
                                fact.getClass()
                                        .getSimpleName()
                                        + " -> "
                                        + fact
                        )
        );

        JavaFieldFact secretValue =
                facts.stream()
                        .filter(
                                JavaFieldFact.class
                                        ::isInstance
                        )
                        .map(
                                JavaFieldFact.class
                                        ::cast
                        )
                        .filter(
                                field ->
                                        "secretValue".equals(
                                                field.fieldName()
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                "secretValue",
                secretValue.fieldName()
        );

        PrivacyDataAnnotationFact annotation =
                facts.stream()
                        .filter(
                                PrivacyDataAnnotationFact.class
                                        ::isInstance
                        )
                        .map(
                                PrivacyDataAnnotationFact.class
                                        ::cast
                        )
                        .filter(
                                fact ->
                                        fact.fieldFactId()
                                                .equals(
                                                        secretValue.id()
                                                )
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                "NATIONAL_IDENTIFIER",
                annotation.declaredType()
        );
    }


}