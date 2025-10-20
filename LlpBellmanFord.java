import java.util.*;
import java.util.function.IntPredicate;

import TestGenerator.*;

public final class LlpBellmanFord extends LlpKernel {
    private static final int INF = Integer.MAX_VALUE / 4;
    private final WeightedDirectedGraphMatrix g;
    private final int source;
    private final BitSet L;
    private final int[] d;
    private final int[] budget; // n-1 down to 0
    private volatile boolean negCycle;

    public LlpBellmanFord(WeightedDirectedGraphMatrix g, int source) {
        super(g.getNumVertices(), g.getNumVertices());
        this.g = g;
        this.source = source;
        this.d = new int[n];
        this.budget = new int[n];
        this.L = new BitSet(n);
        Arrays.fill(d, INF);
        d[this.source] = 0;
        Arrays.fill(budget, n - 1);
    }

    private static int safeAdd(int a, int b) {
        if (a >= INF/2) return INF;
        long s = (long)a + b;
        if (s >= INF) return INF;
        if (s <= -INF) return -INF;
        return (int)s;
    }

    private boolean forbidden(int v) {
        int dv = d[v], best = dv;
        for (int u : g.getParents(v)) {
            int cand = safeAdd(d[u], g.getWeight(u, v));
            if (cand < best) best = cand;
        }
        return best < dv;
    }

    @Override
    protected int numAdvanceSteps() { return 1; }

    @Override
    protected boolean eligible(int v) { return budget[v] > 0; }

    @Override
    protected IntPredicate forbiddens(int forbIdx) {
        IntPredicate pred = v -> forbidden(v);
        return pred;
    }

    @Override
    protected void advanceStep(int stepIdx, int v) {
        // single step: relax v by its best parent
        int dv = d[v], best = dv;
        for (int u : g.getParents(v)) {
            int cand = safeAdd(d[u], g.getWeight(u, v));
            if (cand < best) best = cand;
        }
        d[v] = best;
        if (budget[v] == 0 && best < dv) negCycle = true; // improvement after n-1
        else if (budget[v] > 0) budget[v]--;
    }

    public boolean hasNegativeCycle() { return negCycle; }
    public int[] distances() { return d.clone(); }

    @Override
    public int[] solve() throws Exception {
        boolean hasForbidden = true;
        
        while (hasForbidden) {
            hasForbidden = collectForbidden(0, L);;
            if (hasForbidden) advance(L);
        }

        return d;
    }
}


class LlpBellmanFordTest {
    private static final int INF = Integer.MAX_VALUE / 4;

    private static void test1() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test1.txt");
        LlpBellmanFord bf = new LlpBellmanFord(graph, 0);
        int[] path = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(path, new int[]{0, 10, -1, INF});
        System.out.println("Test1 ---------- OK");
    }

    private static void test2() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test2.txt");
        LlpBellmanFord bf = new LlpBellmanFord(graph, 0);
        int[] path = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(path, new int[]{0});
        System.out.println("Test2 ---------- OK");
    }

    private static void test3() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test3.txt");
        LlpBellmanFord bf = new LlpBellmanFord(graph, 0);
        int[] path = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(path, new int[]{0, 5});
        System.out.println("Test3 ---------- OK");
    }

    private static void test4() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test4.txt");
        LlpBellmanFord bf = new LlpBellmanFord(graph, 0);
        int[] path = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(path, new int[]{0, 2, 1});
        System.out.println("Test4 ---------- OK");
    }

    private static void test5() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test5.txt");
        LlpBellmanFord bf = new LlpBellmanFord(graph, 0);
        int[] path = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(path, new int[]{0, 3, 7, INF});
        System.out.println("Test5 ---------- OK");
    }
    
    private static void test6() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test6.txt");
        LlpBellmanFord bf = new LlpBellmanFord(graph, 0);
        int[] path = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(path, new int[]{0, 0, 0, 0, 0});
        System.out.println("Test6 ---------- OK");
    }

    private static void test7() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test7.txt");
        LlpBellmanFord bf = new LlpBellmanFord(graph, 0);
        int[] path = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(path, new int[]{0, 3, 2, 5});
        System.out.println("Test7 ---------- OK");
    }

    private static void test9() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test9.txt");
        LlpBellmanFord bf = new LlpBellmanFord(graph, 0);
        int[] path = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(path, new int[]{0, 0, 1, 6, 3, 1});
        System.out.println("Test9 ---------- OK");
    }

    public static void main(String[] args) {
        try {
            long startTime = System.nanoTime();
            test1();
            test2();
            test3();
            test4();
            test5();
            test6();
            test7();
            test9();
            long endTime = System.nanoTime();
            long elapsedTimeNanos = endTime - startTime;
            double elapsedTimeMS = (double) elapsedTimeNanos / 1_000_000.0;
            System.out.println("Elapsed time: " + elapsedTimeMS + " milliseconds");
        } catch (Exception e) { e.printStackTrace(); }
    }

    // // Negative Cycle -- Assume there's no negative cycle
    // private static void test9() throws Exception {
    //     WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test8.txt");
    //     LlpBellmanFord bf = new LlpBellmanFord(graph, 0);
    //     int[] path = bf.solve();
    //     bf.close();
    //     SimpleTests.check(bf.hasNegativeCycle(), "Should detect a negative cycle");
    //     System.err.println("path:");
    //     for (int i = 0; i < path.length; i++)
    //         System.out.print(path[i] + " ");
    //     System.out.println("Test8 ---------- OK");
    // }
}
