import java.util.function.IntPredicate;
import java.util.BitSet;

import TestGenerator.*;

public class LlpFastComp extends LlpKernel {
    private final WeightedDirectedGraphMatrix g;
    private final BitSet placeholder, L;
    private final int[] parent;
    private final int[] vmax;

    public LlpFastComp(WeightedDirectedGraphMatrix graph){
        super(graph.getNumVertices(), graph.getNumVertices());
        this.g = graph;
        this.parent = new int[g.getNumVertices()];
        this.vmax = new int[g.getNumVertices()];
        this.placeholder = new BitSet();
        this.L = new BitSet();
        L.set(0, g.getNumVertices());

        for (int i = 0; i < graph.getNumVertices(); i++) parent[i] = i;
    }

    private boolean checkSameParentInSameNeighborhood(int v) {
        boolean hasDifferentParents = false;
        for (int p: g.getParents(v)) hasDifferentParents |= parent[v] < parent[p];
        return hasDifferentParents;
    }

    private boolean checkParentNotEqGrandParent(int v) {
        return parent[v] != parent[parent[v]];
    }

    @Override
    protected boolean eligible(int v) { return true; }
    @Override
    protected int numAdvanceSteps() { return 3; }

    @Override
    protected IntPredicate forbiddens(int forbIdx) {
        if (forbIdx == 0) return v -> checkSameParentInSameNeighborhood(v);
        else return v -> checkParentNotEqGrandParent(v);
    }

    @Override
    protected void advanceStep(int stepIdx, int v) {
        // Step 1: Compute vmax
        if (stepIdx == 0) {
            int maxVmax = parent[v];
            for (int n: g.getParents(v)) maxVmax = Math.max(maxVmax, parent[n]);
            vmax[v] = maxVmax;
        }

        // Step 2: Direct parent of v to the largest vmax in the neighborhood
        else if (stepIdx == 1) {
            if (v == parent[v]) {
                int maxParent = vmax[v];
                for (int u = 0; u < parent.length; u++)
                    if (parent[v] == parent[u]) maxParent = Math.max(maxParent, vmax[u]);
                parent[v] = maxParent;
            }
        }
    }

    // Need a little tweak for Fast Component to be efficient
    @Override
    public void advance(BitSet L) {
        // Executes Step 1
        parallelForEach(L, v -> advanceStep(0, v));

        // Execute Step 2
        parallelForEach(L, v -> advanceStep(1, v));

        // Step 3: Convert rooted tree to rooted star
        boolean hasForb = true;
        while (hasForb) {
            hasForb = collectForbidden(1, placeholder);
            if (hasForb) parallelForEach(L, v -> parent[v] = parent[parent[v]]);
        }
    }

    @Override
    public int[] solve() throws Exception {
        boolean hasForbidden = true;

        while (hasForbidden) {
            hasForbidden = collectForbidden(0, placeholder);;
            if (hasForbidden) advance(L);
        }

        return parent;
    }
}


class LlpFastCompTest {
    private static String testDir = "./TestGenerator/Tests/FastComp/";

    private static void test1() throws Exception {
        WeightedDirectedGraphMatrix graph = UUGLoader.loadFromFile(testDir + "test1.txt");
        LlpFastComp bf = new LlpFastComp(graph);
        int[] parents = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(parents, new int[]{0});
        System.out.println("Test1 ---------- OK");
    }

    private static void test2() throws Exception {
        WeightedDirectedGraphMatrix graph = UUGLoader.loadFromFile(testDir + "test2.txt");
        LlpFastComp bf = new LlpFastComp(graph);
        int[] parents = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(parents, new int[]{1, 1});
        System.out.println("Test2 ---------- OK");
    }

    private static void test3() throws Exception {
        WeightedDirectedGraphMatrix graph = UUGLoader.loadFromFile(testDir + "test3.txt");
        LlpFastComp bf = new LlpFastComp(graph);
        int[] parents = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(parents, new int[]{0, 1});
        System.out.println("Test3 ---------- OK");
    }

    private static void test4() throws Exception {
        WeightedDirectedGraphMatrix graph = UUGLoader.loadFromFile(testDir + "test4.txt");
        LlpFastComp bf = new LlpFastComp(graph);
        int[] parents = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(parents, new int[]{2, 2, 2});
        System.out.println("Test4 ---------- OK");
    }

    private static void test5() throws Exception {
        WeightedDirectedGraphMatrix graph = UUGLoader.loadFromFile(testDir + "test5.txt");
        LlpFastComp bf = new LlpFastComp(graph);
        int[] parents = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(parents, new int[]{1, 1, 3, 3});
        System.out.println("Test5 ---------- OK");
    }

    private static void test6() throws Exception {
        WeightedDirectedGraphMatrix graph = UUGLoader.loadFromFile(testDir + "test6.txt");
        LlpFastComp bf = new LlpFastComp(graph);
        int[] parents = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(parents, new int[]{4, 4, 4, 4, 4, 5});
        System.out.println("Test6 ---------- OK");
    }

    private static void test7() throws Exception {
        WeightedDirectedGraphMatrix graph = UUGLoader.loadFromFile(testDir + "test7.txt");
        LlpFastComp bf = new LlpFastComp(graph);
        int[] parents = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(parents, new int[]{3, 3, 3, 3});
        System.out.println("Test7 ---------- OK");
    }

    private static void test8() throws Exception {
        WeightedDirectedGraphMatrix graph = UUGLoader.loadFromFile(testDir + "test8.txt");
        LlpFastComp bf = new LlpFastComp(graph);
        int[] parents = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(parents, new int[]{3, 3, 3, 3, 6, 6, 6});
        System.out.println("Test8 ---------- OK");
    }

    private static void test9() throws Exception {
        WeightedDirectedGraphMatrix graph = UUGLoader.loadFromFile(testDir + "test9.txt");
        LlpFastComp bf = new LlpFastComp(graph);
        int[] parents = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(parents, new int[]{4, 4, 4, 4, 4});
        System.out.println("Test9 ---------- OK");
    }

    private static void test10() throws Exception {
        WeightedDirectedGraphMatrix graph = UUGLoader.loadFromFile(testDir + "test10.txt");
        LlpFastComp bf = new LlpFastComp(graph);
        int[] parents = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(parents, new int[]{2, 2, 2, 3, 7, 7, 7, 7});
        System.out.println("Test10 ---------- OK");
    }

    private static void test11() throws Exception {
        // Undirected Graphs are just Weighted Directed Graphs with all weights = 1, and (v->u) implies (u->v)
        WeightedDirectedGraphMatrix graph = UUGLoader.loadFromFile(testDir + "test11.txt");
        LlpFastComp bf = new LlpFastComp(graph);
        int[] parents = bf.solve();
        bf.close();
        SimpleTests.checkArrEq(parents, new int[]{1, 1, 9, 9, 9, 9, 9, 9, 9, 9});
        System.out.println("Test11 ---------- OK");
    }

    public static void main(String[] args) {
        try {
            test1();
            test2();
            test3();
            test4();
            test5();
            test6();
            test7();
            test8();
            test9();
            test10();
            test11();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
