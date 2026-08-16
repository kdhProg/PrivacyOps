package io.github.privacyops.analyzer.flow;

import io.github.privacyops.fact.Fact;
import io.github.privacyops.fact.JavaFieldFact;
import io.github.privacyops.fact.MapperColumnFact;

import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.flow.DataFlowLinker;
import io.github.privacyops.flow.DataFlowRelation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapperResultFlowLinker
        implements DataFlowLinker {

    @Override
    public List<DataFlowEdge> link(
            List<Fact> facts
    ) {

        List<DataFlowEdge> edges =
                new ArrayList<>();

        List<MapperColumnFact> columns =
                facts.stream()
                        .filter(
                                MapperColumnFact.class
                                        ::isInstance
                        )
                        .map(
                                MapperColumnFact.class
                                        ::cast
                        )
                        .toList();

        List<JavaFieldFact> fields =
                facts.stream()
                        .filter(
                                JavaFieldFact.class
                                        ::isInstance
                        )
                        .map(
                                JavaFieldFact.class
                                        ::cast
                        )
                        .toList();

        for (MapperColumnFact column :
                columns) {

            for (JavaFieldFact field :
                    fields) {

                if (!matchesResultType(
                        column.resultType(),
                        field.className()
                )) {
                    continue;
                }

                if (!matchesName(
                        column.columnName(),
                        field.fieldName()
                )) {
                    continue;
                }

                edges.add(
                        new DataFlowEdge(
                                column.id(),
                                field.id(),
                                DataFlowRelation.MAPPER_RESULT
                        )
                );
            }
        }

        return edges;
    }

    private boolean matchesResultType(
            String resultType,
            String className
    ) {

        if (resultType == null
                || resultType.isBlank()
                || className == null
                || className.isBlank()) {

            return false;
        }

        if (resultType.equals(className)) {
            return true;
        }

        String resultSimple =
                simpleName(resultType);

        String classSimple =
                simpleName(className);

        return resultSimple.equals(
                classSimple
        );
    }

    private String simpleName(
            String value
    ) {

        int index =
                value.lastIndexOf('.');

        if (index < 0) {
            return value;
        }

        return value.substring(
                index + 1
        );
    }

    private boolean matchesName(
            String columnName,
            String fieldName
    ) {

        return normalizeDatabaseName(
                columnName
        ).equals(
                normalizeJavaName(
                        fieldName
                )
        );
    }

    private String normalizeDatabaseName(
            String value
    ) {

        if (value == null) {
            return "";
        }

        String[] tokens =
                value.toLowerCase(
                                Locale.ROOT
                        )
                        .split("[_\\-]");

        StringBuilder normalized =
                new StringBuilder();

        for (String token : tokens) {

            normalized.append(
                    normalizeToken(token)
            );
        }

        return normalized.toString();
    }

    private String normalizeJavaName(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .toLowerCase(
                        Locale.ROOT
                )
                .replace("_", "")
                .replace("-", "");
    }

    private String normalizeToken(
            String token
    ) {

        return switch (token) {

            case "addr" ->
                    "address";

            case "no" ->
                    "number";

            default ->
                    token;
        };
    }
}