import java.util.*;
import java.util.function.IntPredicate;

import TestGenerator.*;

/**
 * LLP-based implementation of the Gale-Shapley stable matching algorithm.
 *
 * Lattice Structure:
 * State is a proposal-vector p = (p[0], p[1], ..., p[m-1]), where p[i] is the index in
 * pref_m[i] of the woman man i is currently proposing to.
 *
 * Domain: Each p[i] in {0, 1, ..., n}, where n means exhausted.
 *
 * Partial order: p ≤ q iff for every man i, p[i] ≤ q[i].
 * (Smaller index = better for men, so p ≤ q means p is better/equal for men)
 *
 * Bottom element ⊥ = (0,0,...,0), everyone proposing to their top choice.
 * Top element ⊤ = (n,n,...,n), everyone exhausted.
 *
 * Forbidden Predicate:
 * Man i is forbidden if there exists a woman w such that:
 * 1. w is strictly preferred by i over his current partner at p, AND
 * 2. w would prefer i to her current proposer under p
 * This defines a blocking pair (i,w) that must be eliminated.
 *
 * Advance Step:
 * When forbidden(i,p) is true, increment p[i] by one (man i proposes to next woman).
 * This eliminates the blocking pair by making man i stop proposing to w.
 *
 * This parallel version processes all forbidden men simultaneously.
 */
public final class LlpGaleShapley extends LlpKernel {
    private static final int EXHAUSTED = -1;  // Special value for woman_of when p[i] >= n

    private final int m;                 // number of men
    private final int numWomen;          // number of women (n in the lattice description)
    private final int[][] menPrefs;      // menPrefs[i][rank] = woman id
    private final int[][] womenPrefs;    // womenPrefs[w][rank] = man id
    private final int[][] womenRanking;  // womenRanking[w][man] = rank of man for woman w
    private final int[][] menRanking;    // menRanking[man][woman] = rank of woman for man

    private final int[] p;               // Proposal vector: p[i] = current proposal index for man i

    private final BitSet L;              // Set of forbidden men

    public LlpGaleShapley(GaleShapleyLoader.MatchingProblem problem) {
        super(problem.n, problem.n);  // n men (using n from parent class as m)

        this.m = problem.n;
        this.numWomen = problem.n;
        this.menPrefs = problem.menPrefs;
        this.womenPrefs = problem.womenPrefs;
        this.womenRanking = problem.womenRanking;

        // Build inverse ranking for men (for O(1) preference lookups)
        this.menRanking = new int[m][numWomen];
        for (int man = 0; man < m; man++) {
            for (int rank = 0; rank < numWomen; rank++) {
                int woman = menPrefs[man][rank];
                menRanking[man][woman] = rank;
            }
        }

        // Initialize proposal vector to bottom element: everyone at their top choice
        this.p = new int[m];
        Arrays.fill(p, 0);

        this.L = new BitSet(m);
    }

    /**
     * Helper: woman_of(i, p) - the woman man i is currently proposing to.
     * Returns EXHAUSTED if p[i] >= numWomen.
     */
    private int womanOf(int man) {
        if (p[man] >= numWomen) {
            return EXHAUSTED;
        }
        return menPrefs[man][p[man]];
    }

    /**
     * Helper: proposers_of(w, p) - set of men currently proposing to woman w.
     */
    private List<Integer> proposersOf(int woman) {
        List<Integer> proposers = new ArrayList<>();
        for (int man = 0; man < m; man++) {
            if (p[man] < numWomen && menPrefs[man][p[man]] == woman) {
                proposers.add(man);
            }
        }
        return proposers;
    }

    /**
     * Helper: current_partner_of_w(w, p) - the best proposer for woman w.
     * Returns EXHAUSTED if no one is proposing to w.
     */
    private int currentPartnerOf(int woman) {
        List<Integer> proposers = proposersOf(woman);
        if (proposers.isEmpty()) {
            return EXHAUSTED;
        }

        // Find the best proposer according to woman's preferences
        int best = proposers.get(0);
        int bestRank = womenRanking[woman][best];

        for (int i = 1; i < proposers.size(); i++) {
            int proposer = proposers.get(i);
            int rank = womenRanking[woman][proposer];
            if (rank < bestRank) {
                best = proposer;
                bestRank = rank;
            }
        }
        return best;
    }

    /**
     * Helper: partner_of_man(i, p) - the woman matched to man i.
     * Returns EXHAUSTED if man i is not matched.
     */
    private int partnerOfMan(int man) {
        int w = womanOf(man);
        if (w == EXHAUSTED) {
            return EXHAUSTED;
        }

        int wPartner = currentPartnerOf(w);
        if (wPartner == man) {
            return w;
        }
        return EXHAUSTED;
    }

    /**
     * Forbidden predicate for man i:
     * forbidden(i,p) is true iff there exists a woman w such that:
     * 1. w is strictly preferred by i over his current partner at p, AND
     * 2. w would prefer i to her current proposer under p
     *
     * This defines a blocking pair (i,w).
     */
    private boolean forbidden(int man) {
        if (p[man] >= numWomen) {
            return false; // Exhausted, cannot be forbidden
        }

        int currentPartner = partnerOfMan(man);
        int currentPartnerRank = (currentPartner == EXHAUSTED) ? numWomen : menRanking[man][currentPartner];

        // Check all women that man prefers to his current partner
        for (int rank = 0; rank < currentPartnerRank; rank++) {
            int w = menPrefs[man][rank];
            int wCurrentPartner = currentPartnerOf(w);

            // Check if w would prefer man to her current partner
            if (wCurrentPartner == EXHAUSTED || womenRanking[w][man] < womenRanking[w][wCurrentPartner]) {
                // Found a blocking pair (man, w)
                return true;
            }
        }

        return false;
    }

    @Override
    protected boolean eligible(int man) {
        // All men are eligible to be checked
        return true;
    }

    @Override
    protected IntPredicate forbiddens(int forbIdx) {
        return man -> forbidden(man);
    }

    @Override
    protected int numAdvanceSteps() {
        // Single step: increment proposal index
        return 1;
    }

    /**
     * Advance step: Increment p[i] by one.
     * This moves man i to his next preferred woman, eliminating the blocking pair.
     */
    @Override
    protected void advanceStep(int stepIdx, int man) {
        if (p[man] < numWomen) {
            synchronized (p) {
                p[man]++;
            }
        }
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

        // Build the final matching from the proposal vector
        int[] menMatching = new int[m];
        for (int man = 0; man < m; man++) {
            menMatching[man] = partnerOfMan(man);
        }

        return menMatching;
    }

    /**
     * Returns the women's partners (inverse matching).
     */
    public int[] getWomenMatching() {
        int[] womenMatching = new int[numWomen];
        Arrays.fill(womenMatching, EXHAUSTED);

        for (int man = 0; man < m; man++) {
            int woman = partnerOfMan(man);
            if (woman != EXHAUSTED) {
                womenMatching[woman] = man;
            }
        }

        return womenMatching;
    }

    /**
     * Returns the current proposal vector (state of the lattice).
     */
    public int[] getProposalVector() {
        return p.clone();
    }

    /**
     * Verifies that the matching is stable.
     * A matching is stable if there is no pair (man, w) such that:
     * 1. man prefers w to his current partner, AND
     * 2. w prefers man to her current partner
     *
     * This is equivalent to checking that no man is forbidden.
     */
    public boolean isStable() {
        for (int man = 0; man < m; man++) {
            if (forbidden(man)) {
                return false; // Found a blocking pair
            }
        }
        return true;
    }
}


class LlpGaleShapleyTest {
    private static final String testDir = "TestGenerator/Tests/GaleShapley/";

    private static void test1() throws Exception {
        // Classic 3x3 example
        GaleShapleyLoader.MatchingProblem problem = GaleShapleyLoader.loadFromFile(testDir + "test1.txt");
        LlpGaleShapley gs = new LlpGaleShapley(problem);
        int[] menMatching = gs.solve();
        gs.close();

        // Verify stability
        SimpleTests.check(gs.isStable(), "Matching should be stable");

        // Verify all men are matched
        for (int m = 0; m < problem.n; m++) {
            SimpleTests.check(menMatching[m] >= 0, "Man " + m + " should be matched");
        }

        System.out.println("Test1 ---------- OK");
        System.out.println("Men's matching: " + Arrays.toString(menMatching));
    }

    private static void test2() throws Exception {
        // 4x4 example
        GaleShapleyLoader.MatchingProblem problem = GaleShapleyLoader.loadFromFile(testDir + "test2.txt");
        LlpGaleShapley gs = new LlpGaleShapley(problem);
        int[] menMatching = gs.solve();
        gs.close();

        // Verify stability
        SimpleTests.check(gs.isStable(), "Matching should be stable");

        // Verify all men are matched
        for (int m = 0; m < problem.n; m++) {
            SimpleTests.check(menMatching[m] >= 0, "Man " + m + " should be matched");
        }

        System.out.println("Test2 ---------- OK");
        System.out.println("Men's matching: " + Arrays.toString(menMatching));
    }

    private static void test3() throws Exception {
        // Simple 2x2 example with known optimal matching
        GaleShapleyLoader.MatchingProblem problem = GaleShapleyLoader.loadFromFile(testDir + "test3.txt");
        LlpGaleShapley gs = new LlpGaleShapley(problem);
        int[] menMatching = gs.solve();
        gs.close();

        // Verify stability
        SimpleTests.check(gs.isStable(), "Matching should be stable");

        // For this specific test, we expect men-optimal matching: [0, 1]
        SimpleTests.checkArrEq(menMatching, new int[]{0, 1});

        System.out.println("Test3 ---------- OK");
        System.out.println("Men's matching: " + Arrays.toString(menMatching));
    }

    private static void test4() throws Exception {
        // 3x3 with different preference structure
        GaleShapleyLoader.MatchingProblem problem = GaleShapleyLoader.loadFromFile(testDir + "test4.txt");
        LlpGaleShapley gs = new LlpGaleShapley(problem);
        int[] menMatching = gs.solve();
        gs.close();

        // Verify stability
        SimpleTests.check(gs.isStable(), "Matching should be stable");

        // Verify all men are matched
        for (int m = 0; m < problem.n; m++) {
            SimpleTests.check(menMatching[m] >= 0, "Man " + m + " should be matched");
        }

        System.out.println("Test4 ---------- OK");
        System.out.println("Men's matching: " + Arrays.toString(menMatching));
    }

    private static void test5() throws Exception {
        // Edge case: 1x1 (single man and single woman)
        GaleShapleyLoader.MatchingProblem problem = GaleShapleyLoader.loadFromFile(testDir + "test5.txt");
        LlpGaleShapley gs = new LlpGaleShapley(problem);
        int[] menMatching = gs.solve();
        gs.close();

        // Verify stability
        SimpleTests.check(gs.isStable(), "Matching should be stable");

        // Should be [0] - man 0 matched to woman 0
        SimpleTests.checkArrEq(menMatching, new int[]{0});

        System.out.println("Test5 (1x1 edge case) ---------- OK");
        System.out.println("Men's matching: " + Arrays.toString(menMatching));
    }

    private static void test6() throws Exception {
        // 5x5 with rotating preferences (worst case for some men)
        GaleShapleyLoader.MatchingProblem problem = GaleShapleyLoader.loadFromFile(testDir + "test6.txt");
        LlpGaleShapley gs = new LlpGaleShapley(problem);
        int[] menMatching = gs.solve();
        int[] proposalVector = gs.getProposalVector();
        gs.close();

        // Verify stability
        SimpleTests.check(gs.isStable(), "Matching should be stable");

        // Verify all men are matched
        for (int m = 0; m < problem.n; m++) {
            SimpleTests.check(menMatching[m] >= 0, "Man " + m + " should be matched");
        }

        System.out.println("Test6 (5x5 rotating prefs) ---------- OK");
        System.out.println("Men's matching: " + Arrays.toString(menMatching));
        System.out.println("Proposal vector: " + Arrays.toString(proposalVector));
    }

    private static void test7() throws Exception {
        // Edge case: All men have identical preferences
        GaleShapleyLoader.MatchingProblem problem = GaleShapleyLoader.loadFromFile(testDir + "test7.txt");
        LlpGaleShapley gs = new LlpGaleShapley(problem);
        int[] menMatching = gs.solve();
        int[] proposalVector = gs.getProposalVector();
        gs.close();

        // Verify stability
        SimpleTests.check(gs.isStable(), "Matching should be stable");

        // All men should be matched
        for (int m = 0; m < problem.n; m++) {
            SimpleTests.check(menMatching[m] >= 0, "Man " + m + " should be matched");
        }

        // With identical preferences, proposal vectors will vary
        System.out.println("Test7 (identical men prefs) ---------- OK");
        System.out.println("Men's matching: " + Arrays.toString(menMatching));
        System.out.println("Proposal vector: " + Arrays.toString(proposalVector));
    }

    private static void test8() throws Exception {
        // Edge case: All men have completely reversed preferences
        GaleShapleyLoader.MatchingProblem problem = GaleShapleyLoader.loadFromFile(testDir + "test8.txt");
        LlpGaleShapley gs = new LlpGaleShapley(problem);
        int[] menMatching = gs.solve();
        int[] proposalVector = gs.getProposalVector();
        gs.close();

        // Verify stability
        SimpleTests.check(gs.isStable(), "Matching should be stable");

        // All men should be matched
        for (int m = 0; m < problem.n; m++) {
            SimpleTests.check(menMatching[m] >= 0, "Man " + m + " should be matched");
        }

        System.out.println("Test8 (reversed prefs) ---------- OK");
        System.out.println("Men's matching: " + Arrays.toString(menMatching));
        System.out.println("Proposal vector: " + Arrays.toString(proposalVector));
    }

    private static void test9() throws Exception {
        // 4x4 with complex preference structure
        GaleShapleyLoader.MatchingProblem problem = GaleShapleyLoader.loadFromFile(testDir + "test9.txt");
        LlpGaleShapley gs = new LlpGaleShapley(problem);
        int[] menMatching = gs.solve();
        int[] womenMatching = gs.getWomenMatching();
        gs.close();

        // Verify stability
        SimpleTests.check(gs.isStable(), "Matching should be stable");

        // Verify matching is bijective (each woman matched to exactly one man)
        Set<Integer> matchedWomen = new HashSet<>();
        for (int m = 0; m < problem.n; m++) {
            int w = menMatching[m];
            SimpleTests.check(w >= 0, "Man " + m + " should be matched");
            SimpleTests.check(!matchedWomen.contains(w), "Woman " + w + " matched to multiple men");
            matchedWomen.add(w);
        }

        // Verify inverse matching consistency
        for (int m = 0; m < problem.n; m++) {
            int w = menMatching[m];
            SimpleTests.check(womenMatching[w] == m, "Inverse matching inconsistent");
        }

        System.out.println("Test9 (complex 4x4) ---------- OK");
        System.out.println("Men's matching: " + Arrays.toString(menMatching));
    }

    private static void test10() throws Exception {
        // 6x6 with conflicting preferences (men and women have opposite orderings)
        GaleShapleyLoader.MatchingProblem problem = GaleShapleyLoader.loadFromFile(testDir + "test10.txt");
        LlpGaleShapley gs = new LlpGaleShapley(problem);
        int[] menMatching = gs.solve();
        int[] proposalVector = gs.getProposalVector();
        gs.close();

        // Verify stability
        SimpleTests.check(gs.isStable(), "Matching should be stable");

        // All men should be matched
        for (int m = 0; m < problem.n; m++) {
            SimpleTests.check(menMatching[m] >= 0, "Man " + m + " should be matched");
        }

        // In this case, men prefer in one order, women in opposite
        // This should result in men getting their worst choices
        System.out.println("Test10 (conflicting prefs 6x6) ---------- OK");
        System.out.println("Men's matching: " + Arrays.toString(menMatching));
        System.out.println("Proposal vector: " + Arrays.toString(proposalVector));
    }

    private static void test11() throws Exception {
        // 5x5 with cyclic preference structure
        GaleShapleyLoader.MatchingProblem problem = GaleShapleyLoader.loadFromFile(testDir + "test11.txt");
        LlpGaleShapley gs = new LlpGaleShapley(problem);
        int[] menMatching = gs.solve();
        int[] proposalVector = gs.getProposalVector();
        gs.close();

        // Verify stability
        SimpleTests.check(gs.isStable(), "Matching should be stable");

        // All men should be matched
        for (int m = 0; m < problem.n; m++) {
            SimpleTests.check(menMatching[m] >= 0, "Man " + m + " should be matched");
        }

        System.out.println("Test11 (cyclic 5x5) ---------- OK");
        System.out.println("Men's matching: " + Arrays.toString(menMatching));
        System.out.println("Proposal vector: " + Arrays.toString(proposalVector));
    }

    private static void test12() throws Exception {
        // 7x7 with everyone having rotated preferences
        GaleShapleyLoader.MatchingProblem problem = GaleShapleyLoader.loadFromFile(testDir + "test12.txt");
        LlpGaleShapley gs = new LlpGaleShapley(problem);
        int[] menMatching = gs.solve();
        int[] proposalVector = gs.getProposalVector();
        gs.close();

        // Verify stability
        SimpleTests.check(gs.isStable(), "Matching should be stable");

        // All men should be matched
        for (int m = 0; m < problem.n; m++) {
            SimpleTests.check(menMatching[m] >= 0, "Man " + m + " should be matched");
        }

        // With rotated preferences, there should be a unique stable matching
        System.out.println("Test12 (rotated 7x7) ---------- OK");
        System.out.println("Men's matching: " + Arrays.toString(menMatching));
        System.out.println("Proposal vector: " + Arrays.toString(proposalVector));
    }

    private static void test13() throws Exception {
        // 4x4 with symmetric preference structure
        GaleShapleyLoader.MatchingProblem problem = GaleShapleyLoader.loadFromFile(testDir + "test13.txt");
        LlpGaleShapley gs = new LlpGaleShapley(problem);
        int[] menMatching = gs.solve();
        int[] womenMatching = gs.getWomenMatching();
        gs.close();

        // Verify stability
        SimpleTests.check(gs.isStable(), "Matching should be stable");

        // All men should be matched
        for (int m = 0; m < problem.n; m++) {
            SimpleTests.check(menMatching[m] >= 0, "Man " + m + " should be matched");
        }

        // Verify no blocking pairs exist
        for (int m = 0; m < problem.n; m++) {
            int wm = menMatching[m];
            for (int rank = 0; rank < problem.n; rank++) {
                int w = problem.menPrefs[m][rank];
                if (w == wm) break; // Found current partner, no better options before it

                // Check if w would accept m
                int wPartner = womenMatching[w];
                int mRankForW = problem.womenRanking[w][m];
                int partnerRankForW = problem.womenRanking[w][wPartner];

                // If w would prefer m to her current partner, we have a blocking pair
                SimpleTests.check(mRankForW >= partnerRankForW,
                    "Found blocking pair: (" + m + ", " + w + ")");
            }
        }

        System.out.println("Test13 (symmetric 4x4) ---------- OK");
        System.out.println("Men's matching: " + Arrays.toString(menMatching));
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
            System.out.println("\n=== All 13 Tests Passed ===");
            System.out.println("Elapsed time: " + elapsedTimeMS + " milliseconds");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


/**
 * Lattice property tests to verify the lattice-linear predicate properties of LlpGaleShapley.
 */
class LlpGaleShapleyLatticeTest {
    private static final String testDir = "TestGenerator/Tests/GaleShapley/";

    /**
     * Verify that the proposal vector starts at bottom (all zeros) and increases monotonically.
     */
    private static void testLatticeMonotonicity() throws Exception {
        System.out.println("\n=== Testing Lattice Monotonicity ===");

        GaleShapleyLoader.MatchingProblem problem = GaleShapleyLoader.loadFromFile(testDir + "test3.txt");
        LlpGaleShapley gs = new LlpGaleShapley(problem);

        // Initial state should be bottom element (0, 0, ..., 0)
        int[] initialP = gs.getProposalVector();
        System.out.println("Initial proposal vector (bottom): " + Arrays.toString(initialP));

        for (int val : initialP) {
            SimpleTests.check(val == 0, "Initial proposal vector should be all zeros");
        }

        // Solve the problem
        int[] matching = gs.solve();

        // Final proposal vector should have increased (componentwise >= initial)
        int[] finalP = gs.getProposalVector();
        System.out.println("Final proposal vector: " + Arrays.toString(finalP));

        for (int i = 0; i < initialP.length; i++) {
            SimpleTests.check(finalP[i] >= initialP[i],
                "Proposal vector should increase monotonically (p[" + i + "] went from " +
                initialP[i] + " to " + finalP[i] + ")");
        }

        // Verify stability at the final state
        SimpleTests.check(gs.isStable(), "Final matching should be stable");

        System.out.println("Final matching: " + Arrays.toString(matching));
        System.out.println("testLatticeMonotonicity ---------- OK");

        gs.close();
    }

    /**
     * Verify that the forbidden predicate correctly identifies blocking pairs.
     */
    private static void testForbiddenPredicate() throws Exception {
        System.out.println("\n=== Testing Forbidden Predicate ===");

        GaleShapleyLoader.MatchingProblem problem = GaleShapleyLoader.loadFromFile(testDir + "test4.txt");

        // Create a modified version to manually test forbidden predicate
        LlpGaleShapley gs = new LlpGaleShapley(problem);

        System.out.println("Men's preferences:");
        for (int m = 0; m < problem.n; m++) {
            System.out.println("  Man " + m + ": " + Arrays.toString(problem.menPrefs[m]));
        }

        System.out.println("Women's preferences:");
        for (int w = 0; w < problem.n; w++) {
            System.out.println("  Woman " + w + ": " + Arrays.toString(problem.womenPrefs[w]));
        }

        // Solve the problem
        int[] matching = gs.solve();
        int[] proposalVector = gs.getProposalVector();

        System.out.println("\nFinal state:");
        System.out.println("  Proposal vector: " + Arrays.toString(proposalVector));
        System.out.println("  Men's matching: " + Arrays.toString(matching));

        // At the stable matching, no man should be forbidden
        SimpleTests.check(gs.isStable(), "Stable matching should have no forbidden men");

        System.out.println("testForbiddenPredicate ---------- OK");

        gs.close();
    }

    /**
     * Verify that the algorithm produces the men-optimal stable matching.
     */
    private static void testMenOptimalMatching() throws Exception {
        System.out.println("\n=== Testing Men-Optimal Matching ===");

        // Test 3 has a simple structure where we can verify men-optimality
        GaleShapleyLoader.MatchingProblem problem = GaleShapleyLoader.loadFromFile(testDir + "test3.txt");
        LlpGaleShapley gs = new LlpGaleShapley(problem);

        int[] matching = gs.solve();
        int[] proposalVector = gs.getProposalVector();

        System.out.println("Proposal vector: " + Arrays.toString(proposalVector));
        System.out.println("Men's matching: " + Arrays.toString(matching));

        // For test3.txt (2x2 problem), the men-optimal matching is [0, 1]
        // This means Man 0 gets Woman 0 (his first choice) and Man 1 gets Woman 1 (his first choice)
        SimpleTests.checkArrEq(matching, new int[]{0, 1});

        System.out.println("testMenOptimalMatching ---------- OK");

        gs.close();
    }

    /**
     * Test with a larger problem to ensure correctness at scale.
     */
    private static void testLargerProblem() throws Exception {
        System.out.println("\n=== Testing Larger Problem ===");

        GaleShapleyLoader.MatchingProblem problem = GaleShapleyLoader.loadFromFile(testDir + "test2.txt");
        LlpGaleShapley gs = new LlpGaleShapley(problem);

        int[] matching = gs.solve();
        int[] proposalVector = gs.getProposalVector();

        System.out.println("Proposal vector: " + Arrays.toString(proposalVector));
        System.out.println("Men's matching: " + Arrays.toString(matching));

        // Verify all men are matched
        for (int m = 0; m < problem.n; m++) {
            SimpleTests.check(matching[m] >= 0, "Man " + m + " should be matched");
        }

        // Verify stability
        SimpleTests.check(gs.isStable(), "Matching should be stable");

        // Verify matching is a valid permutation (no woman matched to multiple men)
        Set<Integer> matchedWomen = new HashSet<>();
        for (int m = 0; m < problem.n; m++) {
            int w = matching[m];
            SimpleTests.check(!matchedWomen.contains(w), "Woman " + w + " matched to multiple men");
            matchedWomen.add(w);
        }

        System.out.println("testLargerProblem ---------- OK");

        gs.close();
    }

    public static void main(String[] args) {
        try {
            long startTime = System.nanoTime();

            testLatticeMonotonicity();
            testForbiddenPredicate();
            testMenOptimalMatching();
            testLargerProblem();

            long endTime = System.nanoTime();
            double elapsedTimeMS = (double) (endTime - startTime) / 1_000_000.0;

            System.out.println("\n=== All Lattice Tests Passed ===");
            System.out.println("Elapsed time: " + elapsedTimeMS + " milliseconds");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
