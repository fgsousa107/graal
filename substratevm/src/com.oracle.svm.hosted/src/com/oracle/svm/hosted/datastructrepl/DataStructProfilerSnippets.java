package com.oracle.svm.hosted.datastructrepl;

import java.util.HashMap;
import java.util.Map;

import com.oracle.svm.core.snippets.SnippetRuntime;
import com.oracle.svm.core.snippets.SubstrateForeignCallTarget;

public class DataStructProfilerSnippets {

    public static final SnippetRuntime.SubstrateForeignCallDescriptor NEW_LOOP_ITERATION = SnippetRuntime.findForeignCall(DataStructProfilerSnippets.class, "newLoopIteration", false);
    public static final SnippetRuntime.SubstrateForeignCallDescriptor NORMAL_RETURN = SnippetRuntime.findForeignCall(DataStructProfilerSnippets.class, "normalReturn", false);
    public static final SnippetRuntime.SubstrateForeignCallDescriptor LAUNCH_START = SnippetRuntime.findForeignCall(DataStructProfilerSnippets.class, "launchStart", false);
    public static final SnippetRuntime.SubstrateForeignCallDescriptor LAUNCH_RETURN = SnippetRuntime.findForeignCall(DataStructProfilerSnippets.class, "launchReturn", false);
    public static final SnippetRuntime.SubstrateForeignCallDescriptor[] FOREIGN_CALLS = new SnippetRuntime.SubstrateForeignCallDescriptor[]{NEW_LOOP_ITERATION, NORMAL_RETURN, LAUNCH_START, LAUNCH_RETURN};

    private static long start = System.currentTimeMillis();
    private static long count = 0;
    private static Map<Integer, Map<Integer, Integer>> loops = new HashMap<>();

    @SubstrateForeignCallTarget(stubCallingConvention = false)
    public static void newLoopIteration(int size, int methodId, int loopId) {
        count += size;
        //System.out.println("Size: " + size + ", Count : " + count);
        if (!loops.containsKey(methodId)) {
            loops.put(methodId, new HashMap<>());
        }
        if (!loops.get(methodId).containsKey(loopId)) {
            loops.get(methodId).put(loopId, size);
        }
    }

    @SubstrateForeignCallTarget(stubCallingConvention = false)
    public static void normalReturn(int size) {
        count += size;
    }

    @SubstrateForeignCallTarget(stubCallingConvention = false)
    public static void launchStart() {
        System.out.println("launchStart");
        start = System.currentTimeMillis();
    }

    @SubstrateForeignCallTarget(stubCallingConvention = false)
    public static void launchReturn(int size) {
        count += size;
        int oneIterationEveryLoop = 0;
        for (Integer k1 : loops.keySet()) {
            for (Integer k2 : loops.get(k1).keySet()) {
                oneIterationEveryLoop += loops.get(k1).get(k2);
            }
        }
        count -= oneIterationEveryLoop;
        long end = System.currentTimeMillis();
        double throughput = (double) count / (end - start);
        System.out.println("Count : " + count + ", Start : " + start + ", End : " + end + ", Duration : " + (end - start) + " ms.");
        System.out.println("Throughput of thread " + Thread.currentThread().getId() + " : " + throughput + " bytecode per ms.");
    }

    public static Runnable dumpProfileResults() {
        return () -> {
            System.out.println("Finished profiling thread. |||");
        };
    }
}
