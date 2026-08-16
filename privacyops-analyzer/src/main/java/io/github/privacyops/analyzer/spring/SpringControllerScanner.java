package io.github.privacyops.analyzer.spring;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import io.github.privacyops.fact.ApiEndpointFact;
import io.github.privacyops.fact.Fact;
import io.github.privacyops.model.SourceLocation;
import io.github.privacyops.scan.ArtifactScanner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpringControllerScanner implements ArtifactScanner {

    @Override
    public boolean supports(Path path) {
        return path != null
                && path.toString()
                .toLowerCase()
                .endsWith(".java");
    }

    @Override
    public List<Fact> scan(Path path) {

        List<Fact> facts = new ArrayList<>();

        if (!supports(path)) {
            return facts;
        }

        try {

            CompilationUnit compilationUnit =
                    StaticJavaParser.parse(path);

            compilationUnit
                    .findAll(ClassOrInterfaceDeclaration.class)
                    .stream()
                    .filter(this::isController)
                    .forEach(clazz ->
                            extractEndpoints(
                                    path,
                                    compilationUnit,
                                    clazz,
                                    facts
                            )
                    );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to read Spring source: " + path,
                    e
            );
        }

        return facts;
    }

    private boolean isController(
            ClassOrInterfaceDeclaration clazz
    ) {

        return clazz.getAnnotations()
                .stream()
                .map(annotation ->
                        annotation
                                .getName()
                                .getIdentifier()
                )
                .anyMatch(name ->
                        name.equals("Controller")
                                || name.equals("RestController")
                );
    }

    private void extractEndpoints(
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

        String classPath =
                extractClassPath(clazz);

        for (MethodDeclaration method :
                clazz.getMethods()) {

            Optional<HttpMapping> mapping =
                    extractMapping(method);

            if (mapping.isEmpty()) {
                continue;
            }

            String methodPath =
                    mapping.get().path();

            String fullPath =
                    combinePaths(
                            classPath,
                            methodPath
                    );

            Integer line =
                    method.getBegin()
                            .map(position ->
                                    position.line
                            )
                            .orElse(null);

            String id =
                    "api:"
                            + className
                            + "#"
                            + method.getNameAsString();

            ApiEndpointFact fact =
                    new ApiEndpointFact(
                            id,
                            mapping.get().httpMethod(),
                            fullPath,
                            className,
                            method.getNameAsString(),
                            method.getTypeAsString(),
                            new SourceLocation(
                                    path.toString(),
                                    line
                            )
                    );

            facts.add(fact);
        }
    }

    private Optional<HttpMapping> extractMapping(
            MethodDeclaration method
    ) {

        for (AnnotationExpr annotation :
                method.getAnnotations()) {

            String name =
                    annotation
                            .getName()
                            .getIdentifier();

            String httpMethod =
                    switch (name) {

                        case "GetMapping" -> "GET";
                        case "PostMapping" -> "POST";
                        case "PutMapping" -> "PUT";
                        case "DeleteMapping" -> "DELETE";
                        case "PatchMapping" -> "PATCH";

                        default -> null;
                    };

            if (httpMethod != null) {

                return Optional.of(
                        new HttpMapping(
                                httpMethod,
                                extractPath(annotation)
                        )
                );
            }
        }

        return Optional.empty();
    }

    private String extractClassPath(
            ClassOrInterfaceDeclaration clazz
    ) {

        return clazz
                .getAnnotations()
                .stream()
                .filter(annotation ->
                        annotation
                                .getName()
                                .getIdentifier()
                                .equals("RequestMapping")
                )
                .findFirst()
                .map(this::extractPath)
                .orElse("");
    }

    private String extractPath(
            AnnotationExpr annotation
    ) {

        if (annotation instanceof
                SingleMemberAnnotationExpr singleMember) {

            if (singleMember.getMemberValue()
                    instanceof StringLiteralExpr stringLiteral) {

                return stringLiteral.asString();
            }
        }

        return "";
    }

    private String resolveClassName(
            CompilationUnit compilationUnit,
            ClassOrInterfaceDeclaration clazz
    ) {

        String packageName =
                compilationUnit
                        .getPackageDeclaration()
                        .map(pd ->
                                pd.getNameAsString()
                        )
                        .orElse("");

        if (packageName.isBlank()) {
            return clazz.getNameAsString();
        }

        return packageName
                + "."
                + clazz.getNameAsString();
    }

    private String combinePaths(
            String classPath,
            String methodPath
    ) {

        String result =
                ("/"
                        + classPath
                        + "/"
                        + methodPath)
                        .replaceAll("/+", "/");

        if (!result.startsWith("/")) {
            result = "/" + result;
        }

        return result;
    }

    private record HttpMapping(
            String httpMethod,
            String path
    ) {
    }
}