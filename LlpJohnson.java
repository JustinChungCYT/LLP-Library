import java.util.*;
import java.util.function.IntPredicate;

import TestGenerator.*;

public class LlpJohnson extends LlpKernel {
    private final WeightedDirectedGraphMatrix g;
    private final int[] price;  // price array
    private final int[] newPrice;
    private final int[] budget; // n-1 down to 0
    private final BitSet L;
    private volatile boolean negCycle;

    public LlpJohnson(WeightedDirectedGraphMatrix g){
        super(g.getNumVertices(), g.getNumVertices());
        this.g = g;
        this.L = new BitSet();
        L.clear(0, g.getNumVertices());
        this.negCycle = false;
        this.price = new int[g.getNumVertices()];
        this.newPrice = new int[g.getNumVertices()];
        this.budget = new int[g.getNumVertices()];
        Arrays.fill(budget, g.getNumVertices() - 1);
    }

    private boolean forbidden(int v){
        int maxPrice = price[v];
        for (int i: g.getParents(v)) maxPrice = Math.max(maxPrice, price[i] - g.getWeight(i, v));
        boolean hasForb = price[v] < maxPrice;
        if(hasForb) newPrice[v] = maxPrice;
        return hasForb;
    }

    @Override
    protected boolean eligible(int v){ return budget[v] >= 0; }

    @Override
    protected IntPredicate forbiddens(int forbIdx){ return v -> forbidden(v); }

    @Override
    protected int numAdvanceSteps(){ return 1; }

    @Override
    protected void advanceStep(int stepIdx, int v){
        price[v] = newPrice[v];
        if (budget[v] == 0) negCycle = true; // improvement after n-1
        budget[v]--;
    }

    public boolean hasNegativeCycle() { return negCycle; }

    public int[] solve(){
        boolean hasForbidden = true;
        while(hasForbidden){
            hasForbidden = collectForbidden(0, L);
            if (hasForbidden) advance(L);
        }
        if (negCycle) return null;
        return price;
    }
}


class LlpJohnsonTest {
    private static String testDir = "./TestGenerator/Tests/Johnsons/";

    private static void test1() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile(testDir + "test1.txt");
        LlpJohnson js = new LlpJohnson(graph);
        int[] price = js.solve();
        js.close();
        SimpleTests.checkArrEq(price, new int[]{0, 2, 3, 1, 6, 0});
        System.out.println("Test1 ---------- OK");
    }

    private static void test2() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile(testDir + "test2.txt");
        LlpJohnson js = new LlpJohnson(graph);
        int[] price = js.solve();
        js.close();
        SimpleTests.checkArrEq(price, new int[]{0});
        System.out.println("Test2 ---------- OK");
    }

    private static void test3() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile(testDir + "test3.txt");
        LlpJohnson js = new LlpJohnson(graph);
        int[] price = js.solve();
        js.close();
        SimpleTests.checkArrEq(price, new int[]{0, 0});
        System.out.println("Test3 ---------- OK");
    }

    private static void test4() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile(testDir + "test4.txt");
        LlpJohnson js = new LlpJohnson(graph);
        int[] price = js.solve();
        js.close();
        SimpleTests.checkArrEq(price, new int[]{0, 2});
        System.out.println("Test4 ---------- OK");
    }

    private static void test5() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile(testDir + "test5.txt");
        LlpJohnson js = new LlpJohnson(graph);
        int[] price = js.solve();
        js.close();
        SimpleTests.checkArrEq(price, new int[]{0, 2, 5});
        System.out.println("Test5 ---------- OK");
    }

    private static void test6() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile(testDir + "test6.txt");
        LlpJohnson js = new LlpJohnson(graph);
        int[] price = js.solve();
        js.close();
        SimpleTests.checkArrEq(price, new int[]{0, 0, 4});
        System.out.println("Test6 ---------- OK");
    }

    private static void test7() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile(testDir + "test7.txt");
        LlpJohnson js = new LlpJohnson(graph);
        int[] price = js.solve();
        js.close();
        SimpleTests.checkArrEq(price, new int[]{0, 1, 0});
        System.out.println("Test7 ---------- OK");
    }

    private static void test8() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile(testDir + "test8.txt");
        LlpJohnson js = new LlpJohnson(graph);
        int[] price = js.solve();
        js.close();
        SimpleTests.checkArrEq(price, new int[]{0, 0, 0});
        System.out.println("Test8 ---------- OK");
    }

    private static void test9() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile(testDir + "test9.txt");
        LlpJohnson js = new LlpJohnson(graph);
        int[] price = js.solve();
        js.close();
        SimpleTests.checkArrEq(price, new int[]{0, 5});
        System.out.println("Test9 ---------- OK");
    }

    private static void test10() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile(testDir + "test10.txt");
        LlpJohnson js = new LlpJohnson(graph);
        int[] price = js.solve();
        js.close();
        SimpleTests.checkArrEq(price, new int[]{0, 0, 4});
        System.out.println("Test10 ---------- OK");
    }

    private static void test11() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile(testDir + "test11.txt");
        LlpJohnson js = new LlpJohnson(graph);
        int[] price = js.solve();
        js.close();
        SimpleTests.checkArrEq(price, new int[]{0, 0, 1});
        System.out.println("Test11 ---------- OK");
    }

    private static void test12() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile(testDir + "test12.txt");
        LlpJohnson js = new LlpJohnson(graph);
        int[] price = js.solve();
        js.close();
        SimpleTests.checkArrEq(price, new int[]{0, 0, 0});
        System.out.println("Test12 ---------- OK");
    }

    private static void test13() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile(testDir + "test13.txt");
        LlpJohnson js = new LlpJohnson(graph);
        int[] price = js.solve();
        js.close();
        SimpleTests.checkNull(price, "test13");
        System.out.println("Test13 ---------- OK");
    }

    private static void test14() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile(testDir + "test14.txt");
        LlpJohnson js = new LlpJohnson(graph);
        int[] price = js.solve();
        js.close();
        SimpleTests.checkNull(price, "test14");
        System.out.println("Test14 ---------- OK");
    }

    private static void test15() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile(testDir + "test15.txt");
        LlpJohnson js = new LlpJohnson(graph);
        int[] price = js.solve();
        js.close();
        SimpleTests.checkArrEq(price, new int[]{0, 2, 4, 0, 2});
        System.out.println("Test15 ---------- OK");
    }

    private static void test16() throws Exception {
        WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile(testDir + "test16.txt");
        LlpJohnson js = new LlpJohnson(graph);
        int[] price = js.solve();
        js.close();
        SimpleTests.checkArrEq(price, new int[]{0, 3, 2, 0, 0, 0});
        System.out.println("Test16 ---------- OK");
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
            test12();
            test13();
            test14();
            test15();
            test16();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
