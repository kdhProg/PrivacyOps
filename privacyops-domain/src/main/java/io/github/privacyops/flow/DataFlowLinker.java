package io.github.privacyops.flow;

import io.github.privacyops.fact.Fact;

import java.util.List;

public interface DataFlowLinker {

    List<DataFlowEdge> link(List<Fact> facts);
}