package io.github.privacyops.analyzer.flow;

import io.github.privacyops.fact.DatabaseColumnFact;
import io.github.privacyops.fact.Fact;
import io.github.privacyops.fact.MapperColumnFact;
import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.flow.DataFlowRelation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DatabaseMapperFlowLinker {

    public List<DataFlowEdge> link(
            List<Fact> facts
    ) {

        List<DataFlowEdge> edges =
                new ArrayList<>();

        List<DatabaseColumnFact> databaseColumns =
                facts.stream()
                        .filter(
                                DatabaseColumnFact.class
                                        ::isInstance
                        )
                        .map(
                                DatabaseColumnFact.class
                                        ::cast
                        )
                        .toList();

        List<MapperColumnFact> mapperColumns =
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

        for (DatabaseColumnFact databaseColumn :
                databaseColumns) {

            for (MapperColumnFact mapperColumn :
                    mapperColumns) {

                if (!sameTable(
                        databaseColumn,
                        mapperColumn
                )) {
                    continue;
                }

                if (!sameColumn(
                        databaseColumn,
                        mapperColumn
                )) {
                    continue;
                }

                edges.add(
                        new DataFlowEdge(
                                databaseColumn.id(),
                                mapperColumn.id(),
                                DataFlowRelation.DATABASE_MAPPER
                        )
                );
            }
        }

        return edges;
    }

    private boolean sameTable(
            DatabaseColumnFact databaseColumn,
            MapperColumnFact mapperColumn
    ) {

        String databaseTable =
                normalize(
                        databaseColumn.tableName()
                );

        String mapperTable =
                normalize(
                        mapperColumn.tableName()
                );

        return databaseTable != null
                && databaseTable.equals(
                mapperTable
        );
    }

    private boolean sameColumn(
            DatabaseColumnFact databaseColumn,
            MapperColumnFact mapperColumn
    ) {

        String databaseColumnName =
                normalize(
                        databaseColumn.columnName()
                );

        String mapperColumnName =
                normalize(
                        mapperColumn.columnName()
                );

        return databaseColumnName != null
                && databaseColumnName.equals(
                mapperColumnName
        );
    }

    private String normalize(
            String value
    ) {

        if (value == null) {
            return null;
        }

        return value
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}