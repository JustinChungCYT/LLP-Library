import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import TestGenerator.WeightedDirectedGraphMatrix;
import TestGenerator.DirGraphLoader;
import TestGenerator.SimpleTests;


// Check forbidden
class VertexCallable implements Callable<Boolean> {
    final AtomicIntegerArray d;
    final WeightedDirectedGraphMatrix graph;
    final AtomicIntegerArray advanceCount;
    // final ArrayList<EdgeCallable> edgeCallables;
    // final ExecutorService pool;
    final AtomicIntegerArray L;
    AtomicBoolean hasForbidden;
    int id;
    List<Integer> parents;

    class EdgeCallable implements Callable<Void> {
        int id;
        int from;
        int to;

        EdgeCallable(int id, int from, int to){
            this.id = id;
            this.from = from;
            this.to = to;
        }

        @Override
        public Void call() throws Exception {
            // Check forbidden
            if (d.get(to) == Integer.MAX_VALUE || d.get(to) > d.get(from) + graph.getWeight(from, to)) {
                hasForbidden.set(true);
                L.set(id, 1);
            }
            return null;
        }
    }

    VertexCallable(WeightedDirectedGraphMatrix graph, int id, AtomicIntegerArray d, AtomicIntegerArray L, AtomicBoolean hasForbidden, AtomicIntegerArray advanceCount){
        this.id = id;
        this.graph = graph;
        this.d = d;
        this.advanceCount = advanceCount;
        this.L = L;
        parents = graph.getParents(id);
        // edgeCallables = new ArrayList<EdgeCallable>();
        // for (int p = 0; p < parents.size(); p++) edgeCallables.add(new EdgeCallable(p, parents.get(p), p));

        this.hasForbidden = hasForbidden;
        // pool = Executors.newFixedThreadPool(parents.size());
    }

    private void checkFobiddenSeq() throws Exception {
        if (advanceCount.get(id) <= 0) return;
        for (int p: parents) {
            if (d.get(id) == Integer.MAX_VALUE || d.get(id) > d.get(p) + graph.getWeight(p, id)) {
                hasForbidden.set(true);
                L.set(id, 1);
            }
        }
    }

    @Override
    public Boolean call() throws Exception {
        checkFobiddenSeq();
        return hasForbidden.get();
    }
}

class AdvanceCallable implements Callable<Void> {
    final AtomicIntegerArray d;
    final WeightedDirectedGraphMatrix graph;
    final ExecutorService pool;
    final AtomicIntegerArray advanceCount;
    int id;

    AdvanceCallable(int id, WeightedDirectedGraphMatrix graph, AtomicIntegerArray d, AtomicIntegerArray advanceCount) {
        this.id = id;
        this.graph = graph;
        this.d = d;
        this.advanceCount = advanceCount;
        pool = Executors.newFixedThreadPool(graph.getNumVertices());
    }

    @Override
    public Void call() throws Exception {
        int minPath = Integer.MAX_VALUE;
        for (int p: graph.getParents(id)) {
            if (d.get(p) == Integer.MAX_VALUE) continue;
            int val = d.get(p) + graph.getWeight(p, id);
            minPath = Math.min(minPath, val);
        }
        d.set(id, minPath);
        advanceCount.decrementAndGet(id);
        return null;
    }
}


public class LlpBF {
    final AtomicIntegerArray d;
    final ExecutorService pool;
    final ArrayList<VertexCallable> vertexCallables;
    final ArrayList<AdvanceCallable> advanceCallables;
    final AtomicIntegerArray advanceCount;
    AtomicIntegerArray L;
    AtomicBoolean hasForbidden;

    LlpBF(WeightedDirectedGraphMatrix graph) {
        // d[source] = 0, d[i] = Inf for all sourcce != 0
        int n = graph.getNumVertices();
        d = new AtomicIntegerArray(n);
        d.set(0, 0);
        
        pool = Executors.newFixedThreadPool(n);
        vertexCallables = new ArrayList<VertexCallable>();
        advanceCallables = new ArrayList<AdvanceCallable>();
        hasForbidden = new AtomicBoolean(true);
        L = new AtomicIntegerArray(n);
        advanceCount = new AtomicIntegerArray(n);

        for (int i = 0; i < graph.getNumVertices(); i++) {
            advanceCallables.add(new AdvanceCallable(i, graph, d, advanceCount));
            advanceCount.set(i, n - 1);
            L.set(i, 0);
            if (graph.getParents(i).size() > 0) vertexCallables.add(new VertexCallable(graph, i, d, L, hasForbidden, advanceCount));
            if (i != 0) d.set(i, Integer.MAX_VALUE);
        }
    }

    private AtomicBoolean checkForbidden() throws InterruptedException {
        hasForbidden.set(false);
        pool.invokeAll(vertexCallables);
        return hasForbidden;
    }

    private void advance() throws InterruptedException {
        ArrayList<AdvanceCallable> advances = new ArrayList<AdvanceCallable>();
        // filter out all fobiedden vertices
        for (int i = 0; i < L.length(); i++) {
            if (L.get(i) == 1){
                advances.add(advanceCallables.get(i));
                L.set(i, 0);
            }
        }
        pool.invokeAll(advances);
    }

    public int[] findShortest() {
        // while there exists forbidden
        int[] sol = new int[d.length()];
        try {
            while (hasForbidden.get()) {
                hasForbidden = checkForbidden();
                if (hasForbidden.get()) advance();
            }
        } catch (Exception e) { e.printStackTrace(); }
        finally {
            // for (VertexCallable v: vertexCallables) v.pool.shutdown();
            pool.shutdown();
        }

        for (int i = 0; i < d.length(); i++){
            sol[i] = d.get(i);
        }
        return sol;
    }
}

class LlpBFTest {
    private static void test1() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test1.txt");
        LlpBF bf = new LlpBF(graph);
        int[] path = bf.findShortest();
        SimpleTests.checkArrEq(path, new int[]{0, 10, -1, Integer.MAX_VALUE});
        System.out.println("Test1 ---------- OK");
    }

    private static void test2() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test2.txt");
        LlpBF bf = new LlpBF(graph);
        int[] path = bf.findShortest();
        SimpleTests.checkArrEq(path, new int[]{0});
        System.out.println("Test2 ---------- OK");
    }

    private static void test3() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test3.txt");
        LlpBF bf = new LlpBF(graph);
        int[] path = bf.findShortest();
        SimpleTests.checkArrEq(path, new int[]{0, 5});
        System.out.println("Test3 ---------- OK");
    }

    private static void test4() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test4.txt");
        LlpBF bf = new LlpBF(graph);
        int[] path = bf.findShortest();
        SimpleTests.checkArrEq(path, new int[]{0, 2, 1});
        System.out.println("Test4 ---------- OK");
    }

    private static void test5() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test5.txt");
        LlpBF bf = new LlpBF(graph);
        int[] path = bf.findShortest();
        SimpleTests.checkArrEq(path, new int[]{0, 3, 7, Integer.MAX_VALUE});
        System.out.println("Test5 ---------- OK");
    }
    
    private static void test6() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test6.txt");
        LlpBF bf = new LlpBF(graph);
        int[] path = bf.findShortest();
        SimpleTests.checkArrEq(path, new int[]{0, 0, 0, 0, 0});
        System.out.println("Test6 ---------- OK");
    }

    private static void test7() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test7.txt");
        LlpBF bf = new LlpBF(graph);
        int[] path = bf.findShortest();
        SimpleTests.checkArrEq(path, new int[]{0, 3, 2, 5});
        System.out.println("Test7 ---------- OK");
    }

    // Negative Cycle -- not yet handled
    private static void test8() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test8.txt");
        LlpBF bf = new LlpBF(graph);
        int[] path = bf.findShortest();
        SimpleTests.checkArrEq(path, new int[]{0, 2, 1});
        System.out.println("Test8 ---------- OK");
    }
    private static void test9() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test9.txt");
        LlpBF bf = new LlpBF(graph);
        int[] path = bf.findShortest();
        SimpleTests.checkArrEq(path, new int[]{0, 0, 1, 6, 3, 1});
        System.out.println("Test9 ---------- OK");
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
            test9();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
