package com.oracle.svm.hosted.datastructrepl;

import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.nodes.ConstantNode;
import org.graalvm.compiler.nodes.LoopBeginNode;
import org.graalvm.compiler.nodes.LoopEndNode;
import org.graalvm.compiler.nodes.ReturnNode;
import org.graalvm.compiler.nodes.StartNode;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.extended.ForeignCallNode;
import org.graalvm.compiler.phases.Phase;

// && !methodClass.startsWith("Ljava.lang") && !methodClass.startsWith("Ljava.io")

public class DataStructProfilerPhase extends Phase {

    @Override
    protected void run(StructuredGraph graph) {
        String method = graph.asJavaMethod().getName();
        String[] c = graph.asJavaMethod().getDeclaringClass().getName().split("[/;]+");
        String methodClass = c[0];
        for (int i = 1; i < c.length; i++) {
            methodClass += "." + c[i];
        }
        if (methodClass.startsWith("Lcom.oracle") || methodClass.startsWith("Lorg.graalvm") || methodClass.startsWith("Ljava.util.HashMap")
                || methodClass.startsWith("Ljava.text") || methodClass.startsWith("Lsun.security")) {
            return;
        }

        /*System.out.println(methodClass + " - " + method + " - " + graph.graphId() + " - " + graph.getBytecodeSize() + " bytecode size.");
        System.out.println("\tBCI - " + graph.getEntryBCI());*/

        for (Node n : graph.getNodes()) {
            if (n instanceof ReturnNode && method.equals("launch")) {
                ReturnNode rn = (ReturnNode) n;
                //System.out.println("\tReturn BCI - " + rn.getNodeSourcePosition().getBCI());
                // size
                ValueNode[] args = new ValueNode[]{ConstantNode.forInt(rn.getNodeSourcePosition().getBCI(), graph)};
                ForeignCallNode fcn = graph.add(new ForeignCallNode(DataStructProfilerSnippets.LAUNCH_RETURN, args));
                graph.addBeforeFixed(rn, fcn);
            } else if (n instanceof StartNode && method.equals("launch")) {
                StartNode sn = (StartNode) n;
                //System.out.println("\tStart BCI - " + sn.getNodeSourcePosition().getBCI());
                ForeignCallNode fcn = graph.add(new ForeignCallNode(DataStructProfilerSnippets.LAUNCH_START));
                graph.addAfterFixed(fcn, sn);
            } else if (n instanceof LoopEndNode) {
                LoopEndNode ln = (LoopEndNode) n;
                LoopBeginNode lbn = ln.loopBegin();
                //System.out.println("\tLoop Begin BCI - " + lbn.getNodeSourcePosition().getBCI());
                //System.out.println("\tLoop End BCI - " + ln.getNodeSourcePosition().getBCI());
                int size = ln.getNodeSourcePosition().getBCI() - lbn.getNodeSourcePosition().getBCI() + 1;
                // size, methodId, loopId
                ValueNode[] args = new ValueNode[]{ConstantNode.forInt(size, graph), ConstantNode.forInt((int) graph.graphId(), graph), ConstantNode.forInt(ln.getNodeSourcePosition().getBCI(), graph)};
                ForeignCallNode fcn = graph.add(new ForeignCallNode(DataStructProfilerSnippets.NEW_LOOP_ITERATION, args));
                graph.addBeforeFixed(ln, fcn);
            } else if (n instanceof ReturnNode) {
                ReturnNode rn = (ReturnNode) n;
                //System.out.println("\tReturn BCI - " + rn.getNodeSourcePosition().getBCI());
                // size
                ValueNode[] args = new ValueNode[]{ConstantNode.forInt(rn.getNodeSourcePosition().getBCI(), graph)};
                ForeignCallNode fcn = graph.add(new ForeignCallNode(DataStructProfilerSnippets.NORMAL_RETURN, args));
                graph.addBeforeFixed(rn, fcn);
            }
        }
    }
}
