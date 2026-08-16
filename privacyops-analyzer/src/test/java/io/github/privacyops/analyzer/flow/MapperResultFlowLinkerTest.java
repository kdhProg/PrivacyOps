package io.github.privacyops.analyzer.flow;

import io.github.privacyops.fact.Fact;
import io.github.privacyops.fact.JavaFieldFact;
import io.github.privacyops.fact.MapperColumnFact;

import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.flow.DataFlowRelation;

import io.github.privacyops.model.SourceLocation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MapperResultFlowLinkerTest {

    @Test
    void linksDatabaseColumnToDtoField() {

        MapperColumnFact column =
                new MapperColumnFact(
                        "column:EMAIL_ADDR",
                        "samples.MemberMapper.selectMember",
                        "TB_MEMBER",
                        "EMAIL_ADDR",
                        "samples.MemberDto",
                        new SourceLocation(
                                "MemberMapper.xml",
                                null
                        )
                );

        JavaFieldFact field =
                new JavaFieldFact(
                        "field:MemberDto#emailAddress",
                        "samples.MemberDto",
                        "emailAddress",
                        "String",
                        new SourceLocation(
                                "MemberDto.java",
                                10
                        )
                );

        List<Fact> facts =
                List.of(
                        column,
                        field
                );

        MapperResultFlowLinker linker =
                new MapperResultFlowLinker();

        List<DataFlowEdge> edges =
                linker.link(facts);

        assertEquals(
                1,
                edges.size()
        );

        assertEquals(
                DataFlowRelation.MAPPER_RESULT,
                edges.get(0).relation()
        );

        assertEquals(
                column.id(),
                edges.get(0).sourceFactId()
        );

        assertEquals(
                field.id(),
                edges.get(0).targetFactId()
        );
    }

    @Test
    void linksPhoneNoToPhoneNumber() {

        MapperColumnFact column =
                new MapperColumnFact(
                        "column:PHONE_NO",
                        "samples.MemberMapper.selectMember",
                        "TB_MEMBER",
                        "PHONE_NO",
                        "samples.MemberDto",
                        null
                );

        JavaFieldFact field =
                new JavaFieldFact(
                        "field:MemberDto#phoneNumber",
                        "samples.MemberDto",
                        "phoneNumber",
                        "String",
                        null
                );

        MapperResultFlowLinker linker =
                new MapperResultFlowLinker();

        List<DataFlowEdge> edges =
                linker.link(
                        List.of(
                                column,
                                field
                        )
                );

        assertEquals(
                1,
                edges.size()
        );
    }
}