package TestGenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class GraphLoader {

    public static final class GraphData {
        private final int[] V;
        private final int[][] E;
        public GraphData(int[] V, int[][] E) {
            this.V = V;
            this.E = E;
        }

        public int[] getV() { return V; }
        public int[][] getE() { return E; }
    }

    // String-path overload
    public static GraphData loadGraph(String pathStr) throws IOException {
        return loadGraph(Path.of(pathStr));
    }

    // Core loader: returns V and E (directed pairs), keeping duplicates
    public static GraphData loadGraph(Path path) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            // Read n
            String line;
            do {
                line = br.readLine();
                if (line == null) throw new IllegalArgumentException("Missing first line (n).");
                line = line.trim();
            } while (line.isEmpty());

            final int n;
            try {
                n = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("First line must be an integer (n). Got: " + line);
            }
            if (n < 0) throw new IllegalArgumentException("n must be non-negative.");

            // Build V = [0..n-1]
            int[] V = new int[n];
            for (int i = 0; i < n; i++) V[i] = i;

            // Build E as a list of [u, v] pairs. Keep duplicates. Symmetrize each input edge.
            List<int[]> edges = new ArrayList<>();

            for (int i = 0; i < n; i++) {
                line = br.readLine();
                if (line == null) {
                    // Missing neighbor lines are treated as empty
                    continue;
                }
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                for (String p : parts) {
                    if (p.isEmpty()) continue;
                    int v;
                    try {
                        v = Integer.parseInt(p);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                            "Invalid neighbor id on line " + (i + 2) + ": '" + p + "'");
                    }
                    if (v < 0 || v >= n) {
                        throw new IllegalArgumentException(
                            "Neighbor id out of range [0," + (n - 1) + "]: " + v + " (line " + (i + 2) + ")");
                    }

                    // Append directed pair (i, v) and its symmetric (v, i). Keep duplicates.
                    edges.add(new int[]{i, v});
                    edges.add(new int[]{v, i});
                }
            }

            // Optional: error if there is extra non-empty content after n lines
            String extra;
            while ((extra = br.readLine()) != null) {
                if (!extra.trim().isEmpty()) {
                    throw new IllegalArgumentException("Found extra non-empty content after " + n + " neighbor lines.");
                }
            }

            // Convert List<int[]> -> int[][]
            int[][] E = new int[edges.size()][2];
            for (int k = 0; k < edges.size(); k++) {
                E[k] = edges.get(k);
            }

            return new GraphData(V, E);
        }
    }

    public static int[][] loadUndirectedGraph(String pathStr) throws IOException {
        return loadUndirectedGraph(Path.of(pathStr));
    }

    // Existing Path-based loader (kept as the core)
    public static int[][] loadUndirectedGraph(Path path) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            do {
                line = br.readLine();
                if (line == null) throw new IllegalArgumentException("Missing first line (n).");
                line = line.trim();
            } while (line.isEmpty());

            int n;
            try {
                n = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("First line must be an integer (n). Got: " + line);
            }
            if (n < 0) throw new IllegalArgumentException("n must be non-negative.");

            List<Set<Integer>> adj = new ArrayList<>(n);
            for (int i = 0; i < n; i++) adj.add(new TreeSet<>());

            for (int i = 0; i < n; i++) {
                line = br.readLine();
                if (line == null) continue;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");
                for (String p : parts) {
                    if (p.isEmpty()) continue;
                    int v;
                    try {
                        v = Integer.parseInt(p);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                            "Invalid neighbor id on line " + (i + 2) + ": '" + p + "'");
                    }
                    if (v < 0 || v >= n) {
                        throw new IllegalArgumentException(
                            "Neighbor id out of range [0," + (n - 1) + "]: " + v + " (line " + (i + 2) + ")");
                    }
                    adj.get(i).add(v);
                    if (v != i) adj.get(v).add(i);
                }
            }

            String extra;
            while ((extra = br.readLine()) != null) {
                if (!extra.trim().isEmpty()) {
                    throw new IllegalArgumentException("Found extra non-empty content after " + n + " neighbor lines.");
                }
            }

            int[][] G = new int[n][];
            for (int i = 0; i < n; i++) {
                Set<Integer> s = adj.get(i);
                int[] arr = new int[s.size()];
                int k = 0;
                for (int v : s) arr[k++] = v;
                G[i] = arr;
            }
            return G;
        }
    }

    // Demo runner: prints V and E
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java GraphLoader <path-to-graph.txt>");
            System.exit(1);
        }
        GraphData g = loadGraph(args[0]);

        // Print V
        System.out.print("V = [");
        for (int i = 0; i < g.V.length; i++) {
            System.out.print(g.V[i]);
            if (i + 1 < g.V.length) System.out.print(", ");
        }
        System.out.println("]");

        // Print E
        System.out.print("E = [");
        for (int i = 0; i < g.E.length; i++) {
            int[] e = g.E[i];
            System.out.print("[" + e[0] + ", " + e[1] + "]");
            if (i + 1 < g.E.length) System.out.print(", ");
        }
        System.out.println("]");
    }
}
