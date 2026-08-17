package io.github.privacyops.analyzer.java;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import io.github.privacyops.fact.Fact;
import io.github.privacyops.fact.JavaFieldFact;
import io.github.privacyops.fact.PrivacyDataAnnotationFact;
import io.github.privacyops.model.SourceLocation;
import io.github.privacyops.scan.ArtifactScanner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class JavaSourceScanner
        implements ArtifactScanner {

    @Override
    public boolean supports(Path path) {

        return path != null
                && path.toString()
                .toLowerCase(Locale.ROOT)
                .endsWith(".java");
    }

    @Override
    public List<Fact> scan(Path path) {

        List<Fact> facts =
                new ArrayList<>();

        if (!supports(path)) {
            return facts;
        }

        try {

            CompilationUnit compilationUnit =
                    StaticJavaParser.parse(path);

            compilationUnit
                    .findAll(
                            ClassOrInterfaceDeclaration.class
                    )
                    .forEach(
                            clazz ->
                                    extractFields(
                                            path,
                                            compilationUnit,
                                            clazz,
                                            facts
                                    )
                    );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to read Java source: "
                            + path,
                    e
            );
        }

        return facts;
    }

    private void extractFields(
            Path path,
            CompilationUnit compilationUnit,
            ClassOrInterfaceDeclaration clazz,
            List<Fact> facts
    ) {

        String className =
                resolveClassName(
                        compilationUnit,
                        clazz
                );

        for (FieldDeclaration field :
                clazz.getFields()) {

            for (VariableDeclarator variable :
                    field.getVariables()) {

                String fieldName =
                        variable.getNameAsString();

                String fieldType =
                        variable.getTypeAsString();

                Integer line =
                        variable
                                .getBegin()
                                .map(
                                        position ->
                                                position.line
                                )
                                .orElse(null);

                String id =
                        "java-field:"
                                + className
                                + "#"
                                + fieldName;

                JavaFieldFact fieldFact =
                        new JavaFieldFact(
                                id,
                                className,
                                fieldName,
                                fieldType,
                                new SourceLocation(
                                        path.toString(),
                                        line
                                )
                        );

                /*
                 * 1. Java 필드 자체를 Fact로 등록
                 */
                facts.add(fieldFact);

                /*
                 * 2. 해당 필드에 @PrivacyData가 있으면
                 *    별도의 명시적 개인정보 선언 Fact 생성
                 */
                extractPrivacyDataAnnotation(
                        field,
                        fieldFact
                ).ifPresent(
                        facts::add
                );
            }
        }
    }

    private String resolveClassName(
            CompilationUnit compilationUnit,
            ClassOrInterfaceDeclaration clazz
    ) {

        String packageName =
                compilationUnit
                        .getPackageDeclaration()
                        .map(
                                declaration ->
                                        declaration
                                                .getNameAsString()
                        )
                        .orElse("");

        if (packageName.isBlank()) {

            return clazz.getNameAsString();
        }

        return packageName
                + "."
                + clazz.getNameAsString();
    }

    private Optional<PrivacyDataAnnotationFact>
    extractPrivacyDataAnnotation(
            FieldDeclaration field,
            JavaFieldFact fieldFact
    ) {

        for (AnnotationExpr annotation :
                field.getAnnotations()) {

            String annotationName =
                    annotation
                            .getName()
                            .getIdentifier();

            if (!"PrivacyData".equals(
                    annotationName
            )) {
                continue;
            }

            String declaredType =
                    extractPrivacyDataType(
                            annotation
                    );

            if (declaredType == null
                    || declaredType.isBlank()) {

                continue;
            }

            PrivacyDataAnnotationFact annotationFact =
                    new PrivacyDataAnnotationFact(
                            "privacy-data:"
                                    + fieldFact.id(),
                            fieldFact.id(),
                            declaredType,
                            fieldFact.location()
                    );

            return Optional.of(
                    annotationFact
            );
        }

        return Optional.empty();
    }

    private String extractPrivacyDataType(
            AnnotationExpr annotation
    ) {

        /*
         * 현재 권장 문법:
         *
         * @PrivacyData(
         *     type = "NATIONAL_IDENTIFIER"
         * )
         *
         * → NormalAnnotationExpr
         */
        if (annotation
                instanceof NormalAnnotationExpr normal) {

            return normal
                    .getPairs()
                    .stream()
                    .filter(
                            pair ->
                                    "type".equals(
                                            pair.getNameAsString()
                                    )
                    )
                    .map(
                            pair ->
                                    extractAnnotationValue(
                                            pair.getValue()
                                                    .toString()
                                    )
                    )
                    .findFirst()
                    .orElse(null);
        }

        /*
         * 향후 @PrivacyData("EMAIL") 같은 문법을
         * 허용하게 될 경우를 대비한 처리.
         *
         * 현재 PrivacyData Annotation 정의상
         * 반드시 필요한 것은 아니지만
         * Scanner 자체는 대응 가능하게 둔다.
         */
        if (annotation
                instanceof SingleMemberAnnotationExpr singleMember) {

            return extractAnnotationValue(
                    singleMember
                            .getMemberValue()
                            .toString()
            );
        }

        return null;
    }

    private String extractAnnotationValue(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String result =
                value.trim();

        /*
         * "NATIONAL_IDENTIFIER"
         * →
         * NATIONAL_IDENTIFIER
         */
        if (result.length() >= 2
                && result.startsWith("\"")
                && result.endsWith("\"")) {

            return result.substring(
                    1,
                    result.length() - 1
            );
        }

        return result;
    }
}