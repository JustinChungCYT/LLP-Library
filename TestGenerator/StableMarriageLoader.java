package TestGenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads stable marriage problem instances for the LLP stable marriage algorithm.
 *
 * File format:
 * Line 1: n (number of men/women pairs)
 * Next n lines: preference list for each man (woman indices, most preferred first)
 * Next n lines: preference list for each woman (man indices, most preferred first)
 *
 * Example (n=3):
 * 3
 * 0 1 2
 * 1 0 2
 * 0 1 2
 * 1 0 2
 * 0 1 2
 * 1 2 0
 *
 * This represents:
 * - Man 0 prefers women in order: 0, 1, 2
 * - Man 1 prefers women in order: 1, 0, 2
 * - Man 2 prefers women in order: 0, 1, 2
 * - Woman 0 prefers men in order: 1, 0, 2
 * - Woman 1 prefers men in order: 0, 1, 2
 * - Woman 2 prefers men in order: 1, 2, 0
 */
public class StableMarriageLoader {

    /**
     * Encapsulates a stable marriage problem instance
     */
    public static class MatchingProblem {
        public final int n;              // number of men (and women)
        public final int[][] mpref;      // mpref[i][k] = kth choice for man i (0-indexed)
        public final int[][] wpref;      // wpref[j][k] = kth choice for woman j (0-indexed)
        public final int[][] rank;       // rank[j][i] = ranking of man i by woman j (lower is better)

        public MatchingProblem(int n, int[][] mpref, int[][] wpref) {
            this.n = n;
            this.mpref = mpref;
            this.wpref = wpref;

            // Build rank array for efficient preference checking
            this.rank = new int[n][n];
            for (int j = 0; j < n; j++) {
                for (int k = 0; k < n; k++) {
                    int man = wpref[j][k];
                    rank[j][man] = k;
                }
            }
        }

        public int getNumPeople() {
            return n;
        }
    }

    public static MatchingProblem loadFromFile(String pathStr) throws IOException {
        Path path = Path.of(pathStr);
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            // Read n
            String line = br.readLine();
            if (line == null) {
                throw new IllegalArgumentException("Empty file");
            }
            int n = Integer.parseInt(line.trim());
            if (n <= 0) {
                throw new IllegalArgumentException("n must be positive, got: " + n);
            }

            int[][] mpref = new int[n][n];
            int[][] wpref = new int[n][n];

            // Read men's preferences
            for (int i = 0; i < n; i++) {
                line = br.readLine();
                if (line == null) {
                    throw new IllegalArgumentException("Missing preference line for man " + i);
                }
                String[] parts = line.trim().split("\\s+");
                if (parts.length != n) {
                    throw new IllegalArgumentException("Expected " + n + " preferences for man " + i + ", got " + parts.length);
                }

                for (int k = 0; k < n; k++) {
                    int woman = Integer.parseInt(parts[k]);
                    if (woman < 0 || woman >= n) {
                        throw new IllegalArgumentException("Invalid woman index: " + woman);
                    }
                    mpref[i][k] = woman;
                }
            }

            // Read women's preferences
            for (int j = 0; j < n; j++) {
                line = br.readLine();
                if (line == null) {
                    throw new IllegalArgumentException("Missing preference line for woman " + j);
                }
                String[] parts = line.trim().split("\\s+");
                if (parts.length != n) {
                    throw new IllegalArgumentException("Expected " + n + " preferences for woman " + j + ", got " + parts.length);
                }

                for (int k = 0; k < n; k++) {
                    int man = Integer.parseInt(parts[k]);
                    if (man < 0 || man >= n) {
                        throw new IllegalArgumentException("Invalid man index: " + man);
                    }
                    wpref[j][k] = man;
                }
            }

            return new MatchingProblem(n, mpref, wpref);
        }
    }
}
