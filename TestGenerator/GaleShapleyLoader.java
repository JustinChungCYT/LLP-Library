package TestGenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads Gale-Shapley stable matching problem from file.
 *
 * File format:
 * Line 1: n (number of pairs, e.g., men and women)
 * Next n lines: preferences for men (0 to n-1), each line contains n integers (preference order)
 * Next n lines: preferences for women (0 to n-1), each line contains n integers (preference order)
 *
 * Example for n=3:
 * 3
 * 0 1 2    <- Man 0 prefers Woman 0 > Woman 1 > Woman 2
 * 1 0 2    <- Man 1 prefers Woman 1 > Woman 0 > Woman 2
 * 0 1 2    <- Man 2 prefers Woman 0 > Woman 1 > Woman 2
 * 0 1 2    <- Woman 0 prefers Man 0 > Man 1 > Man 2
 * 1 0 2    <- Woman 1 prefers Man 1 > Man 0 > Man 2
 * 0 1 2    <- Woman 2 prefers Man 0 > Man 1 > Man 2
 */
public class GaleShapleyLoader {

    public static class MatchingProblem {
        public final int n;
        public final int[][] menPrefs;    // menPrefs[m][rank] = woman id
        public final int[][] womenPrefs;  // womenPrefs[w][rank] = man id
        public final int[][] womenRanking; // womenRanking[w][m] = rank of man m for woman w

        public MatchingProblem(int n, int[][] menPrefs, int[][] womenPrefs) {
            this.n = n;
            this.menPrefs = menPrefs;
            this.womenPrefs = womenPrefs;

            // Build inverse ranking for women (for O(1) preference lookups)
            this.womenRanking = new int[n][n];
            for (int w = 0; w < n; w++) {
                for (int rank = 0; rank < n; rank++) {
                    int man = womenPrefs[w][rank];
                    womenRanking[w][man] = rank;
                }
            }
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

            // Read men's preferences
            int[][] menPrefs = new int[n][n];
            for (int m = 0; m < n; m++) {
                line = br.readLine();
                if (line == null) {
                    throw new IllegalArgumentException("Missing preference line for man " + m);
                }
                String[] parts = line.trim().split("\\s+");
                if (parts.length != n) {
                    throw new IllegalArgumentException("Expected " + n + " preferences for man " + m + ", got " + parts.length);
                }
                for (int rank = 0; rank < n; rank++) {
                    menPrefs[m][rank] = Integer.parseInt(parts[rank]);
                }
            }

            // Read women's preferences
            int[][] womenPrefs = new int[n][n];
            for (int w = 0; w < n; w++) {
                line = br.readLine();
                if (line == null) {
                    throw new IllegalArgumentException("Missing preference line for woman " + w);
                }
                String[] parts = line.trim().split("\\s+");
                if (parts.length != n) {
                    throw new IllegalArgumentException("Expected " + n + " preferences for woman " + w + ", got " + parts.length);
                }
                for (int rank = 0; rank < n; rank++) {
                    womenPrefs[w][rank] = Integer.parseInt(parts[rank]);
                }
            }

            return new MatchingProblem(n, menPrefs, womenPrefs);
        }
    }
}