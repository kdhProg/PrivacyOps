package io.github.privacyops.analyzer.flow;

import io.github.privacyops.fact.ApiEndpointFact;
import io.github.privacyops.fact.Fact;
import io.github.privacyops.fact.JavaFieldFact;

import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.flow.DataFlowRelation;

import io.github.privacyops.model.SourceLocation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ControllerResponseFlowLinkerTest {

    @Test
    void linksDtoFieldsToApiResponse() {

        JavaFieldFact rrn =
                new JavaFieldFact(
                        "field:MemberDto#rrn",
                        "com.example.MemberDto",
                        "rrn",
                        "String",
                        new SourceLocation(
                                "MemberDto.java",
                                10
                        )
                );

        ApiEndpointFact endpoint =
                new ApiEndpointFact(
                        "api:MemberController#getMember",
                        "GET",
                        "/members/{id}",
                        "com.example.MemberController",
                        "getMember",
                        "MemberDto",
                        new SourceLocation(
                                "MemberController.java",
                                10
                        )
                );

        List<Fact> facts =
                List.of(
                        rrn,
                        endpoint
                );

        ControllerResponseFlowLinker linker =
                new ControllerResponseFlowLinker();

        List<DataFlowEdge> edges =
                linker.link(facts);

        assertEquals(
                1,
                edges.size()
        );

        assertEquals(
                DataFlowRelation.API_RESPONSE,
                edges.get(0).relation()
        );
    }
}