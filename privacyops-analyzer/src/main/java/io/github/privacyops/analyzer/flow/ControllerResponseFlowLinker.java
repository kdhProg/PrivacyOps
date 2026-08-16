package io.github.privacyops.analyzer.flow;

import io.github.privacyops.fact.ApiEndpointFact;
import io.github.privacyops.fact.Fact;
import io.github.privacyops.fact.JavaFieldFact;

import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.flow.DataFlowLinker;
import io.github.privacyops.flow.DataFlowRelation;

import java.util.ArrayList;
import java.util.List;

public class ControllerResponseFlowLinker
        implements DataFlowLinker {

    @Override
    public List<DataFlowEdge> link(
            List<Fact> facts
    ) {

        List<DataFlowEdge> edges =
                new ArrayList<>();

        List<ApiEndpointFact> endpoints =
                facts.stream()
                        .filter(
                                ApiEndpointFact.class
                                        ::isInstance
                        )
                        .map(
                                ApiEndpointFact.class
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

        for (ApiEndpointFact endpoint :
                endpoints) {

            String responseType =
                    normalizeResponseType(
                            endpoint.responseType()
                    );

            for (JavaFieldFact field :
                    fields) {

                if (matchesClass(
                        responseType,
                        field.className()
                )) {

                    edges.add(
                            new DataFlowEdge(
                                    field.id(),
                                    endpoint.id(),
                                    DataFlowRelation.API_RESPONSE
                            )
                    );
                }
            }
        }

        return edges;
    }

    private boolean matchesClass(
            String responseType,
            String className
    ) {

        if (responseType == null
                || className == null) {
            return false;
        }

        if (responseType.equals(className)) {
            return true;
        }

        String simpleClassName =
                className.substring(
                        className.lastIndexOf('.') + 1
                );

        return responseType.equals(
                simpleClassName
        );
    }

    private String normalizeResponseType(
            String responseType
    ) {

        if (responseType == null) {
            return "";
        }

        String value =
                responseType.trim();

        // ResponseEntity<MemberDto> 정도의 단순 Generic 지원
        int genericStart =
                value.indexOf('<');

        int genericEnd =
                value.lastIndexOf('>');

        if (genericStart >= 0
                && genericEnd > genericStart) {

            value =
                    value.substring(
                            genericStart + 1,
                            genericEnd
                    );
        }

        return value.trim();
    }
}