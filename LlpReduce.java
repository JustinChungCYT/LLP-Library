import java.util.*;
import java.util.function.IntPredicate;

import TestGenerator.IntArrayFileLoader;
import TestGenerator.SimpleTests;

public class LlpReduce extends LlpKernel {
    private final int INF = Integer.MAX_VALUE / 4;
    private final int[] A;
    private final int[] tempG;
    private final int[] G;
    private final BitSet L;

    public LlpReduce(int[] A) {
        // super(nextPow2(A.length), nextPow2(A.length));
        super(A.length, A.length);

        // Pad to power-of-two size
        final int N = nextPow2(n);
        this.A = Arrays.copyOf(A, N);
        if (n < N) Arrays.fill(this.A, n, N, 0);

        this.G = new int[this.A.length];
        this.tempG = new int[this.A.length];
        Arrays.fill(G, -INF);
        this.L = new BitSet();
    }

    private boolean forbidden(int v) {
        if (0 <= v && v < n/2 - 1) {
            if (G[v] < G[2*v+1] + G[2*v+2]) {
                tempG[v] = G[2*v+1] + G[2*v+2];
                return true;
            }
        }
        else if (v >= n/2 - 1 && v < n - 1) {
            int base = 2*v - n + 2;
            if (G[v] < A[base] + A[base + 1]) {
                tempG[v] = A[base] + A[base + 1];
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean eligible(int v) { return true; }

    @Override
    protected IntPredicate forbiddens(int forbIdx){ return v -> forbidden(v); }

    @Override
    protected int numAdvanceSteps(){ return 1; }

    @Override
    protected void advanceStep(int stepIdx, int v){ G[v] = tempG[v]; }

    @Override
    public int[] solve() throws Exception {
        boolean hasForbidden = true;
        while (hasForbidden) {
            hasForbidden = collectForbidden(0, L);
            if (hasForbidden) advance(L);
        }
        return Arrays.copyOf(G, n - 1);
    }
}


class LlpReduceTest {
    private final static String testDir = "TestGenerator/Tests/Reduce/";

    private static void test1() throws Exception {
        int[] A = IntArrayFileLoader.load(testDir + "test1.txt");
        LlpReduce rd = new LlpReduce(A);
        int[] G = rd.solve();
        rd.close();
        SimpleTests.checkArrEq(G, new int[]{55, 37, 18, 34, 3, 7, 11, 15, 19});
        System.out.println("Test1 ---------- OK");
    }

    private static void test2() throws Exception {
        int[] A = IntArrayFileLoader.load(testDir + "test2.txt");
        LlpReduce rd = new LlpReduce(A);
        int[] G = rd.solve();
        rd.close();
        SimpleTests.checkArrEq(G, new int[]{36, 10, 26, 3, 7, 11, 15});
        System.out.println("Test2 ---------- OK");
    }

    public static void main(String[] args) {
        try {
            test1();
            test2();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
