package io.github.privacyops.analyzer.java;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;

import io.github.privacyops.fact.Fact;
import io.github.privacyops.fact.JavaFieldFact;
import io.github.privacyops.model.SourceLocation;
import io.github.privacyops.scan.ArtifactScanner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JavaSourceScanner implements ArtifactScanner {

    @Override
    public boolean supports(Path path) {
        return path != null
                && path.toString().toLowerCase().endsWith(".java");
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

//            compilationUnit
//                    .findAll(ClassOrInterfaceDeclaration.class)
//                    .forEach(clazz ->
//                            extractFields(path, clazz, facts)
//                    );

            compilationUnit
                    .findAll(ClassOrInterfaceDeclaration.class)
                    .forEach(clazz ->
                            extractFields(
                                    path,
                                    compilationUnit,
                                    clazz,
                                    facts
                            )
                    );



        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read Java source: " + path,
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

        for (FieldDeclaration field : clazz.getFields()) {

            for (VariableDeclarator variable : field.getVariables()) {

                String fieldName =
                        variable.getNameAsString();

                String fieldType =
                        variable.getTypeAsString();

//                String className =
//                        clazz.getNameAsString();

                String className =
                        resolveClassName(
                                compilationUnit,
                                clazz
                        );

                Integer line = variable
                        .getBegin()
                        .map(position -> position.line)
                        .orElse(null);

                String id =
                        "java-field:"
                                + className
                                + "#"
                                + fieldName;

                JavaFieldFact fact =
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

                facts.add(fact);
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
                        .map(pd -> pd.getNameAsString())
                        .orElse("");

        if (packageName.isBlank()) {
            return clazz.getNameAsString();
        }

        return packageName
                + "."
                + clazz.getNameAsString();
    }

}