import java.util.*;
import java.util.function.IntPredicate;

import TestGenerator.*;

/**
 * LLP-based stable marriage algorithm.
 *
 * This implements Algorithm LLP-StableMarriage1.
 *
 * State vector G[i] represents the choice number for man i.
 * G[i] = k means man i is currently proposing to his k-th choice woman.
 *
 * A man j is "forbidden" if there exists another man i such that:
 * - Both are proposing to the same woman z
 * - Woman z prefers man i over man j
 *
 * When a man is forbidden, he advances to his next choice.
 */
public final class LlpStableMarriage extends LlpKernel {
    private final StableMarriageLoader.MatchingProblem problem;
    private final int[][] mpref;  // mpref[i][k] = kth choice for man i
    private final int[][] rank;   // rank[j][i] = woman j's ranking of man i (lower is better)
    private final BitSet L;
    private final int[] G;        // G[i] = current choice index for man i
    private final int[] partner;  // partner[w] = current man proposing to woman w (-1 if none)
    private int iterationCount = 0;  // Track iterations for critical path analysis

    public LlpStableMarriage(StableMarriageLoader.MatchingProblem problem) {
        super(problem.n, problem.n);
        this.problem = problem;
        this.mpref = problem.mpref;
        this.rank = problem.rank;
        this.G = new int[n];
        this.L = new BitSet(n);
        this.partner = new int[n];

        // Initialize: each man starts with his first choice (index 0)
        Arrays.fill(G, 0);
        Arrays.fill(partner, -1);  // No woman has a partner initially
    }

    /**
     * A man j is forbidden if there exists a man i such that:
     * - Both i and j are proposing to the same woman z
     * - Woman z prefers man i to man j (rank[z][i] < rank[z][j])
     *
     * Optimized O(1) version using partner array.
     */
    private boolean forbidden(int j) {
        if (G[j] >= n) return false; // man j has exhausted all choices

        int z = mpref[j][G[j]]; // woman that man j is currently proposing to

        // Check if woman z already has a partner
        if (partner[z] == -1) {
            return false; // No one proposes to z yet, j can propose
        }

        // Woman z has a current partner; check if she prefers partner over j
        return rank[z][partner[z]] < rank[z][j];
    }

    @Override
    protected int numAdvanceSteps() {
        return 1;
    }

    @Override
    protected boolean eligible(int v) {
        return G[v] < n; // man v still has choices to make
    }

    @Override
    protected IntPredicate forbiddens(int forbIdx) {
        return this::forbidden;
    }

    @Override
    protected void advanceStep(int stepIdx, int v) {
        // Before advancing, if this man was proposing to someone, clear that
        if (G[v] < n) {
            int oldWoman = mpref[v][G[v]];
            if (partner[oldWoman] == v) {
                partner[oldWoman] = -1; // Woman loses this partner
            }
        }

        // Advance man v to his next choice
        G[v]++;

        // After advancing, if still has choices, update partner
        if (G[v] < n) {
            int newWoman = mpref[v][G[v]];
            int currentPartner = partner[newWoman];

            // Man v proposes to newWoman
            if (currentPartner == -1 || rank[newWoman][v] < rank[newWoman][currentPartner]) {
                partner[newWoman] = v; // Woman accepts v
            }
        }
    }

    /**
     * Extract the final matching using the partner array.
     * Returns an array where result[i] = woman matched to man i.
     * Optimized O(n) version.
     */
    public int[] getMatching() {
        int[] matching = new int[n];

        // Use partner array to build matching
        for (int w = 0; w < n; w++) {
            if (partner[w] != -1) {
                matching[partner[w]] = w;
            }
        }

        return matching;
    }

    /**
     * Verify that the matching is stable (no blocking pairs).
     */
    public boolean isStable(int[] matching) {
        // Check each man-woman pair for blocking
        for (int m = 0; m < n; m++) {
            int w = matching[m];

            // Find which man is matched with each woman
            int[] reverseMatching = new int[n];
            for (int i = 0; i < n; i++) {
                reverseMatching[matching[i]] = i;
            }

            // Check if man m and any woman form a blocking pair
            for (int wp = 0; wp < n; wp++) {
                if (wp == w) continue;

                int mp = reverseMatching[wp]; // man matched to woman wp

                // Get man m's preference for woman wp
                int mPrefW = -1, mPrefWp = -1;
                for (int k = 0; k < n; k++) {
                    if (mpref[m][k] == w) mPrefW = k;
                    if (mpref[m][k] == wp) mPrefWp = k;
                }

                // If man m prefers woman wp to his current match w
                if (mPrefWp < mPrefW) {
                    // Check if woman wp prefers man m to her current match mp
                    if (rank[wp][m] < rank[wp][mp]) {
                        return false; // blocking pair found
                    }
                }
            }
        }

        return true; // no blocking pairs
    }

    @Override
    public int[] solve() throws Exception {
        // Initial proposals: each man proposes to his first choice
        for (int m = 0; m < n; m++) {
            int w = mpref[m][0];
            if (partner[w] == -1 || rank[w][m] < rank[w][partner[w]]) {
                partner[w] = m;
            }
        }

        // Main loop: advance forbidden men
        boolean hasForbidden = true;
        while (hasForbidden) {
            hasForbidden = collectForbidden(0, L);
            if (hasForbidden) {
                advance(L);
            }
        }

        return getMatching();
    }
}


class LlpStableMarriageTest {
    private static final String testDir = "TestGenerator/Tests/StableMarriage/";

    private static void test1() throws Exception {
        // Simple 2x2 matching
        StableMarriageLoader.MatchingProblem mp = StableMarriageLoader.loadFromFile(testDir + "test1.txt");
        LlpStableMarriage gs = new LlpStableMarriage(mp);
        int[] matching = gs.solve();
        gs.close();

        // Verify it's a valid perfect matching
        SimpleTests.check(matching.length == 2, "Should have 2 matches");
        SimpleTests.check(gs.isStable(matching), "Matching should be stable");
        System.out.println("Test1 ---------- OK");
    }

    private static void test2() throws Exception {
        // 3x3 matching
        StableMarriageLoader.MatchingProblem mp = StableMarriageLoader.loadFromFile(testDir + "test2.txt");
        LlpStableMarriage gs = new LlpStableMarriage(mp);
        int[] matching = gs.solve();
        gs.close();

        SimpleTests.check(matching.length == 3, "Should have 3 matches");
        SimpleTests.check(gs.isStable(matching), "Matching should be stable");
        System.out.println("Test2 ---------- OK");
    }

    private static void test3() throws Exception {
        // 4x4 matching with complex preferences
        StableMarriageLoader.MatchingProblem mp = StableMarriageLoader.loadFromFile(testDir + "test3.txt");
        LlpStableMarriage gs = new LlpStableMarriage(mp);
        int[] matching = gs.solve();
        gs.close();

        SimpleTests.check(matching.length == 4, "Should have 4 matches");
        SimpleTests.check(gs.isStable(matching), "Matching should be stable");
        System.out.println("Test3 ---------- OK");
    }

    private static void test4() throws Exception {
        // Edge case: single man-woman pair
        StableMarriageLoader.MatchingProblem mp = StableMarriageLoader.loadFromFile(testDir + "test4.txt");
        LlpStableMarriage sm = new LlpStableMarriage(mp);
        int[] matching = sm.solve();
        sm.close();

        SimpleTests.check(matching.length == 1, "Should have 1 match");
        SimpleTests.checkIntEq(matching[0], 0, "Man 0 should be matched with woman 0");
        SimpleTests.check(sm.isStable(matching), "Matching should be stable");
        System.out.println("Test4 ---------- OK");
    }

    private static void test5() throws Exception {
        // Edge case: all men have identical preferences, all women have identical preferences
        StableMarriageLoader.MatchingProblem mp = StableMarriageLoader.loadFromFile(testDir + "test5.txt");
        LlpStableMarriage sm = new LlpStableMarriage(mp);
        int[] matching = sm.solve();
        sm.close();

        SimpleTests.check(matching.length == 3, "Should have 3 matches");
        SimpleTests.check(sm.isStable(matching), "Matching should be stable");
        // With identical preferences, expect men to get their top choices in order
        System.out.println("Test5 ---------- OK");
    }

    private static void test6() throws Exception {
        // Edge case: men and women have completely opposite preference orderings
        StableMarriageLoader.MatchingProblem mp = StableMarriageLoader.loadFromFile(testDir + "test6.txt");
        LlpStableMarriage sm = new LlpStableMarriage(mp);
        int[] matching = sm.solve();
        sm.close();

        SimpleTests.check(matching.length == 3, "Should have 3 matches");
        SimpleTests.check(sm.isStable(matching), "Matching should be stable");
        System.out.println("Test6 ---------- OK");
    }

    private static void test7() throws Exception {
        // Edge case: cyclic preferences (man i prefers woman (i+1) mod n)
        StableMarriageLoader.MatchingProblem mp = StableMarriageLoader.loadFromFile(testDir + "test7.txt");
        LlpStableMarriage sm = new LlpStableMarriage(mp);
        int[] matching = sm.solve();
        sm.close();

        SimpleTests.check(matching.length == 3, "Should have 3 matches");
        SimpleTests.check(sm.isStable(matching), "Matching should be stable");
        System.out.println("Test7 ---------- OK");
    }

    private static void test8() throws Exception {
        // Edge case: worst-case scenario where all men have same preferences,
        // all women have reverse preferences
        StableMarriageLoader.MatchingProblem mp = StableMarriageLoader.loadFromFile(testDir + "test8.txt");
        LlpStableMarriage sm = new LlpStableMarriage(mp);
        int[] matching = sm.solve();
        sm.close();

        SimpleTests.check(matching.length == 4, "Should have 4 matches");
        SimpleTests.check(sm.isStable(matching), "Matching should be stable");
        System.out.println("Test8 ---------- OK");
    }

    private static void test9() throws Exception {
        // Edge case: symmetric preferences (man i and woman i have matching preferences)
        StableMarriageLoader.MatchingProblem mp = StableMarriageLoader.loadFromFile(testDir + "test9.txt");
        LlpStableMarriage sm = new LlpStableMarriage(mp);
        int[] matching = sm.solve();
        sm.close();

        SimpleTests.check(matching.length == 4, "Should have 4 matches");
        SimpleTests.check(sm.isStable(matching), "Matching should be stable");
        // In symmetric case, expect diagonal matching (man i with woman i)
        System.out.println("Test9 ---------- OK");
    }

    private static void test10() throws Exception {
        // Edge case: one woman is everyone's first choice
        StableMarriageLoader.MatchingProblem mp = StableMarriageLoader.loadFromFile(testDir + "test10.txt");
        LlpStableMarriage sm = new LlpStableMarriage(mp);
        int[] matching = sm.solve();
        sm.close();

        SimpleTests.check(matching.length == 4, "Should have 4 matches");
        SimpleTests.check(sm.isStable(matching), "Matching should be stable");
        // Woman 0 should be matched to one of the men who prefer her
        System.out.println("Test10 ---------- OK");
    }

    private static void test11() throws Exception {
        // Larger instance with 6 men and 6 women
        StableMarriageLoader.MatchingProblem mp = StableMarriageLoader.loadFromFile(testDir + "test11.txt");
        LlpStableMarriage sm = new LlpStableMarriage(mp);
        int[] matching = sm.solve();
        sm.close();

        SimpleTests.check(matching.length == 6, "Should have 6 matches");
        SimpleTests.check(sm.isStable(matching), "Matching should be stable");

        // Verify it's a perfect matching (all women matched exactly once)
        boolean[] womenMatched = new boolean[6];
        for (int i = 0; i < 6; i++) {
            SimpleTests.check(!womenMatched[matching[i]], "Each woman should be matched exactly once");
            womenMatched[matching[i]] = true;
        }
        System.out.println("Test11 ---------- OK");
    }

    private static void test12() throws Exception {
        // 5x5 matching (used by LlpAlgo)
        StableMarriageLoader.MatchingProblem mp = StableMarriageLoader.loadFromFile(testDir + "test12.txt");
        LlpStableMarriage sm = new LlpStableMarriage(mp);
        int[] matching = sm.solve();
        sm.close();

        SimpleTests.check(matching.length == 5, "Should have 5 matches");
        SimpleTests.check(sm.isStable(matching), "Matching should be stable");
        System.out.println("Test12 ---------- OK");
    }

    private static void test13() throws Exception {
        // Complex case that could have blocking pairs if not handled correctly
        StableMarriageLoader.MatchingProblem mp = StableMarriageLoader.loadFromFile(testDir + "test13.txt");
        LlpStableMarriage sm = new LlpStableMarriage(mp);
        int[] matching = sm.solve();
        sm.close();

        SimpleTests.check(matching.length == 4, "Should have 4 matches");
        SimpleTests.check(sm.isStable(matching), "Matching should be stable");
        System.out.println("Test13 ---------- OK");
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
            test8();
            test9();
            test10();
            test11();
            test12();
            test13();

            long endTime = System.nanoTime();
            long elapsedTimeNanos = endTime - startTime;
            double elapsedTimeMS = (double) elapsedTimeNanos / 1_000_000.0;
            System.out.println("\nAll tests passed!");
            System.out.println("Total elapsed time: " + elapsedTimeMS + " milliseconds");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
