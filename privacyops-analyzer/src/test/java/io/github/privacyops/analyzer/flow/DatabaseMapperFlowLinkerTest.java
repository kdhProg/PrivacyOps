package io.github.privacyops.analyzer.flow;

import io.github.privacyops.fact.DatabaseColumnFact;
import io.github.privacyops.fact.Fact;
import io.github.privacyops.fact.MapperColumnFact;
import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.flow.DataFlowRelation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMapperFlowLinkerTest {

    @Test
    void linksDatabaseColumnToMapperColumn() {

        DatabaseColumnFact databaseColumn =
                new DatabaseColumnFact(
                        "db-column:APP.TB_MEMBER.RRN",
                        "APP",
                        "TB_MEMBER",
                        "RRN",
                        "VARCHAR",
                        null,
                        null
                );

        MapperColumnFact mapperColumn =
                new MapperColumnFact(
                        "mapper-column:TB_MEMBER.RRN",
                        "samples.MemberMapper.selectMember",
                        "TB_MEMBER",
                        "RRN",
                        "samples.MemberDto",
                        null
                );

        List<Fact> facts =
                List.of(
                        databaseColumn,
                        mapperColumn
                );

        DatabaseMapperFlowLinker linker =
                new DatabaseMapperFlowLinker();

        List<DataFlowEdge> edges =
                linker.link(
                        facts
                );

        assertEquals(
                1,
                edges.size()
        );

        DataFlowEdge edge =
                edges.get(0);

        assertEquals(
                databaseColumn.id(),
                edge.sourceFactId()
        );

        assertEquals(
                mapperColumn.id(),
                edge.targetFactId()
        );

        assertEquals(
                DataFlowRelation.DATABASE_MAPPER,
                edge.relation()
        );
    }

    @Test
    void ignoresDifferentColumns() {

        DatabaseColumnFact databaseColumn =
                new DatabaseColumnFact(
                        "db-column:APP.TB_MEMBER.RRN",
                        "APP",
                        "TB_MEMBER",
                        "RRN",
                        "VARCHAR",
                        null,
                        null
                );

        MapperColumnFact mapperColumn =
                new MapperColumnFact(
                        "mapper-column:TB_MEMBER.NAME",
                        "samples.MemberMapper.selectMember",
                        "TB_MEMBER",
                        "NAME",
                        "samples.MemberDto",
                        null
                );

        DatabaseMapperFlowLinker linker =
                new DatabaseMapperFlowLinker();

        List<DataFlowEdge> edges =
                linker.link(
                        List.of(
                                databaseColumn,
                                mapperColumn
                        )
                );

        assertTrue(
                edges.isEmpty()
        );
    }

    @Test
    void linksColumnsIgnoringCase() {

        DatabaseColumnFact databaseColumn =
                new DatabaseColumnFact(
                        "db-column:APP.TB_MEMBER.RRN",
                        "APP",
                        "TB_MEMBER",
                        "RRN",
                        "VARCHAR",
                        null,
                        null
                );

        MapperColumnFact mapperColumn =
                new MapperColumnFact(
                        "mapper-column:tb_member.rrn",
                        "samples.MemberMapper.selectMember",
                        "tb_member",
                        "rrn",
                        "samples.MemberDto",
                        null
                );

        DatabaseMapperFlowLinker linker =
                new DatabaseMapperFlowLinker();

        List<DataFlowEdge> edges =
                linker.link(
                        List.of(
                                databaseColumn,
                                mapperColumn
                        )
                );

        assertEquals(
                1,
                edges.size()
        );
    }
}