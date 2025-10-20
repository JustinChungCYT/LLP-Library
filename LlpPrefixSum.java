import java.util.*;
import java.util.function.IntPredicate;

import TestGenerator.IntArrayFileLoader;
import TestGenerator.SimpleTests;

public class LlpPrefixSum extends LlpKernel {
    private final int INF = Integer.MAX_VALUE / 4;
    private final int[] A;
    private final int[] G;
    private final int[] tempG;
    private final BitSet L; // starts all v
    private final int lengthWithoutPadding;
    private int[] S;

    public LlpPrefixSum(int[] A) {
        super(nextPow2(A.length), 2*nextPow2(A.length)-1);

        this.lengthWithoutPadding = A.length;
        // Pad to power-of-two size
        this.A = padToPow2(A);

        // S = summation tree
        try { this.S = calculateSummationTree(); }
        catch (Exception e) { e.printStackTrace(); }

        this.G = new int[2*(this.A.length)-1];
        this.tempG = new int[G.length];
        Arrays.fill(G, -INF);
        G[0] = 0;

        this.L = new BitSet();
        L.set(0, G.length);
    }

    private int[] calculateSummationTree() throws Exception {
        LlpReduce rd = new LlpReduce(A);
        int[] tree = rd.solve();
        rd.close();
        return tree;
    }

    private boolean forbidden(int v) {
        if (v == 0) return false;

        int V = v + 1;
        // second ensure
        if (V % 2 == 0) {
            if (G[V - 1] == -INF || G[V - 1] < G[V/2 - 1]) {
                tempG[V - 1] = G[V/2 - 1];
                return true;
            }
        }
        // third ensure
        else if (V < n) {
            if (G[V - 1] == -INF || G[V - 1] < S[V - 1 - 1] + G[V/2 - 1]) {
                tempG[V - 1] = S[V - 1 - 1] + G[V/2 - 1];
                return true;
            }
        }
        // fourth ensure
        else {
            if (G[V - 1] == -INF || G[V - 1] < A[V - n - 1] + G[V/2 - 1]) {
                tempG[V - 1] = A[V - n - 1] + G[V/2 - 1];
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

    // No synchronization required; advanceStep() becomes the main loop for each v
    @Override
    protected void advanceStep(int stepIdx, int v){
        while (forbidden(v)) {
            G[v] = tempG[v];
        }
    }

    @Override
    public int[] solve() throws Exception {
        advance(L);
        int start = 2*n - 1 - A.length;
        int[] G = Arrays.copyOfRange(this.G, start, start + lengthWithoutPadding);
        for (int i = 0; i < G.length; i++) G[i] += A[i];
        return G;
    }
}


class LlpPrefixSumTest {
    private final static String testDir = "TestGenerator/Tests/PrefixSum/";

  private static void test1() throws Exception {
    int[] A = IntArrayFileLoader.load(testDir + "test1.txt");
    LlpPrefixSum ps = new LlpPrefixSum(A);
    int[] G = ps.solve();
    ps.close();
    SimpleTests.checkArrEq(G, new int[]{1, 3, 6, 10, 15, 21, 28, 36});
    System.out.println("Test1 ---------- OK");
  }

  private static void test2() throws Exception {
    int[] A = IntArrayFileLoader.load(testDir + "test2.txt");
    LlpPrefixSum ps = new LlpPrefixSum(A);
    int[] G = ps.solve();
    ps.close();
    SimpleTests.checkArrEq(G, new int[]{1, 3, 6, 10, 15, 21, 28, 36, 45, 55, 66, 78, 91, 105});
    System.out.println("Test2 ---------- OK");
  }

  public static void main(String[] args) {
    try {
      test1();
      test2();
    } catch (Exception e) { e.printStackTrace(); }
  }
}
