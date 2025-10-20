import TestGenerator.*;

public class LlpAlgo {
    private final String algo;
    private final String inputFile;

    public LlpAlgo(String algo, String inputFile) {
        this.algo = algo;
        this.inputFile = inputFile;
    }

    public int[] solve() {
        try {
            int[] A;
            WeightedDirectedGraphMatrix g;
            GaleShapleyLoader.MatchingProblem mp;
            WeightedUndirectedGraph ug;
            switch (algo) {
                case "Reduce":
                    A = IntArrayFileLoader.load(inputFile);
                    return reduce(A);
                case "PrefixSum":
                    A = IntArrayFileLoader.load(inputFile);
                    return prefixSum(A);
                case "BellmanFord":
                    g = DirGraphLoader.loadFromFile(inputFile);
                    return bellmanFord(g, 0);
                case "Johnson":
                    g = DirGraphLoader.loadFromFile(inputFile);
                    return johnson(g);
                case "FastComp":
                    g = UUGLoader.loadFromFile(inputFile);
                    return fastComp(g);
                case "GaleShapley":
                    mp = GaleShapleyLoader.loadFromFile(inputFile);
                    return galeShapley(mp);
                case "Boruvka":
                    ug = BoruvkaGraphLoader.loadFromFile(inputFile);
                    return boruvka(ug);
                default:
                    throw new IllegalArgumentException("Unknown algorithm: " + algo);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    protected int[] reduce(int[] A) throws Exception {
        LlpReduce rd = new LlpReduce(A);
        int[] G = rd.solve();
        rd.close();
        return G;
    }

    protected int[] prefixSum(int[] A) throws Exception {
        LlpPrefixSum ps = new LlpPrefixSum(A);
        int[] G = ps.solve();
        ps.close();
        return G;
    }

    protected int[] bellmanFord(WeightedDirectedGraphMatrix graph, int source) throws Exception {
        LlpBellmanFord bf = new LlpBellmanFord(graph, source);
        int[] path = bf.solve();
        bf.close();
        return path;
    }

    protected int[] johnson(WeightedDirectedGraphMatrix graph) throws Exception {
        LlpJohnson js = new LlpJohnson(graph);
        int[] price = js.solve();
        js.close();
        return price;
    }

    protected int[] fastComp(WeightedDirectedGraphMatrix graph) throws Exception {
        LlpFastComp fc = new LlpFastComp(graph);
        int[] result = fc.solve();
        fc.close();
        return result;
    }

    protected int[] galeShapley(GaleShapleyLoader.MatchingProblem problem) throws Exception {
        LlpGaleShapley gs = new LlpGaleShapley(problem);
        int[] matching = gs.solve();
        gs.close();
        return matching;
    }

    protected int[] boruvka(WeightedUndirectedGraph graph) throws Exception {
        LlpBoruvka boruvka = new LlpBoruvka(graph);
        int[] components = boruvka.solve();
        boruvka.close();
        return components;
    }
}


class LlpAlgoTest {
    private final static String testDir = "TestGenerator/Tests/";
    private static final int INF = Integer.MAX_VALUE / 4;

    private void testReduce() throws Exception {
        LlpAlgo algo = new LlpAlgo("Reduce", testDir + "Reduce/test1.txt");
        int[] res = algo.solve();
        SimpleTests.checkArrEq(res, new int[]{55, 37, 18, 34, 3, 7, 11, 15, 19});
        System.out.println("testReduce ---------- OK");
    }

    private void testPrefixSum() throws Exception {
        LlpAlgo algo = new LlpAlgo("PrefixSum", testDir + "PrefixSum/test1.txt");
        int[] res = algo.solve();
        SimpleTests.checkArrEq(res, new int[]{1, 3, 6, 10, 15, 21, 28, 36});
        System.out.println("testPrefixSum ---------- OK");
    }

    private void testBellmanFord() throws Exception {
        LlpAlgo algo = new LlpAlgo("BellmanFord", testDir + "BF/test1.txt");
        int[] res = algo.solve();
        SimpleTests.checkArrEq(res, new int[]{0, 10, -1, INF});
        System.out.println("testBellmanFord ---------- OK");
    }

    private void testJohnson() throws Exception {
        LlpAlgo algo = new LlpAlgo("Johnson", testDir + "Johnsons/test1.txt");
        int[] res = algo.solve();
        SimpleTests.checkArrEq(res, new int[]{0, 2, 3, 1, 6, 0});
        System.out.println("testJohnson ---------- OK");
    }

    private void testFastComp() throws Exception {
        LlpAlgo algo = new LlpAlgo("FastComp", testDir + "FastComp/test1.txt");
        int[] res = algo.solve();
        SimpleTests.checkArrEq(res, new int[]{0});
        System.out.println("testFastComp ---------- OK");
    }

    private void testGaleShapley() throws Exception {
        LlpAlgo algo = new LlpAlgo("GaleShapley", testDir + "GaleShapley/test3.txt");
        int[] res = algo.solve();
        SimpleTests.checkArrEq(res, new int[]{0, 1});
        System.out.println("testGaleShapley ---------- OK");
    }

    private void testBoruvka() throws Exception {
        LlpAlgo algo = new LlpAlgo("Boruvka", testDir + "Boruvka/test2.txt");
        int[] res = algo.solve();
        // Result is component leaders, all vertices should be in same component
        SimpleTests.check(res[0] == res[1] && res[1] == res[2], "All vertices should be in same component");
        System.out.println("testBoruvka ---------- OK");
    }

    public static void main(String[] args) throws Exception {
        LlpAlgoTest test = new LlpAlgoTest();
        test.testReduce();
        test.testPrefixSum();
        test.testBellmanFord();
        test.testJohnson();
        test.testFastComp();
        test.testGaleShapley();
        test.testBoruvka();
    }
}