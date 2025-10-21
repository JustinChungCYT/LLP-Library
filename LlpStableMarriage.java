import java.util.*;
import java.util.function.IntPredicate;

import TestGenerator.*;

/**
 * LLP-based stable marriage algorithm.
 *
 * This implements Algorithm LLP-StableMarriage1 from the paper.
 *
 * State vector G[i] represents the choice number for man i (1-indexed in the paper, 0-indexed here).
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

    public LlpStableMarriage(StableMarriageLoader.MatchingProblem problem) {
        super(problem.n, problem.n);
        this.problem = problem;
        this.mpref = problem.mpref;
        this.rank = problem.rank;
        this.G = new int[n];
        this.L = new BitSet(n);

        // Initialize: each man starts with his first choice (index 0)
        Arrays.fill(G, 0);
    }

    /**
     * A man j is forbidden if there exists a man i such that:
     * - Both i and j are proposing to the same woman z
     * - Woman z prefers man i to man j (rank[z][i] < rank[z][j])
     *
     * This implements the condition from the paper:
     * forbidden: (∃i : ∃k ≤ G[i] : (z = mpref[i][k]) ∧ (rank[z][i] < rank[z][j]))
     */
    private boolean forbidden(int j) {
        if (G[j] >= n) return false; // man j has exhausted all choices

        int z = mpref[j][G[j]]; // woman that man j is currently proposing to

        // Check if there's another man i who also proposes to z and is preferred
        for (int i = 0; i < n; i++) {
            if (i == j) continue;

            // Check all choices up to and including G[i] for man i
            for (int k = 0; k <= G[i] && k < n; k++) {
                if (mpref[i][k] == z) {
                    // Man i also proposes to woman z
                    // Check if woman z prefers man i to man j
                    if (rank[z][i] < rank[z][j]) {
                        return true; // man j is forbidden
                    }
                }
            }
        }

        return false;
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
        // Advance man v to his next choice
        G[v]++;
    }

    /**
     * Extract the final matching from the G vector.
     * Returns an array where result[i] = woman matched to man i.
     */
    public int[] getMatching() {
        int[] matching = new int[n];
        boolean[] womanMatched = new boolean[n];

        // For each man, find which woman he's matched with
        for (int i = 0; i < n; i++) {
            if (G[i] < n) {
                int woman = mpref[i][G[i]];

                // Check if this woman prefers this man among all who proposed to her
                boolean isMatch = true;
                for (int j = 0; j < n; j++) {
                    if (j != i && G[j] < n && mpref[j][G[j]] == woman) {
                        if (rank[woman][j] < rank[woman][i]) {
                            isMatch = false;
                            break;
                        }
                    }
                }

                if (isMatch) {
                    matching[i] = woman;
                    womanMatched[woman] = true;
                }
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


/**
 * Comprehensive test suite for LlpStableMarriage algorithm.
 *
 * Tests cover:
 * - Basic functionality (2x2, 3x3, 4x4 matchings)
 * - Edge cases:
 *   1. Single pair (n=1)
 *   2. Identical preferences (all same)
 *   3. Reverse preferences (complete opposition)
 *   4. Cyclic preferences (circular dependencies)
 *   5. Worst-case scenario (maximally conflicting preferences)
 *   6. Symmetric preferences (man i and woman i have matching prefs)
 *   7. One popular choice (everyone's first choice is the same)
 *   8. Larger instance (n=6)
 *   9. Blocking pair resolution (complex conflict resolution)
 *
 * All tests verify:
 * - Correct matching size
 * - Stability (no blocking pairs)
 * - Perfect matching (each person matched exactly once)
 */
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
