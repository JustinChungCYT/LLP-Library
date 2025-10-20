package TestGenerator;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class UUGLoader {

    /** Convenience: path as String, disallow self-loops by default. */
    public static WeightedDirectedGraphMatrix loadFromFile(String pathStr) throws IOException {
        return loadFromFile(Path.of(pathStr), /*allowSelfLoops=*/false);
    }

    /** Convenience: path as Path, disallow self-loops by default. */
    public static WeightedDirectedGraphMatrix loadFromFile(Path path) throws IOException {
        return loadFromFile(path, /*allowSelfLoops=*/false);
    }

    /**
     * Load an Unweighted Undirected Graph (UUG).
     * Format:
     *   n
     *   <neighbors of 0>
     *   <neighbors of 1>
     *   ...
     *   <neighbors of n-1>
     *
     * Each neighbors line may be:
     *   - space/comma separated integers (e.g., "1 2" or "1,2" or "1, 2  3")
     *   - "*" meaning no neighbors
     *   - blank line meaning no neighbors
     *
     * For each edge (u,v) we add both (u->v) and (v->u) with weight 1.
     */
    public static WeightedDirectedGraphMatrix loadFromFile(Path path, boolean allowSelfLoops) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(path)) {
            // 1) read vertex count (skip leading blanks)
            String line;
            while ((line = br.readLine()) != null && line.trim().isEmpty()) {}
            if (line == null) throw new IllegalArgumentException("Missing vertex count on first line.");
            final int n;
            try {
                n = Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("First non-empty line must be an integer vertex count, found: " + line);
            }
            if (n < 0) throw new IllegalArgumentException("Vertex count must be non-negative.");

            WeightedDirectedGraphMatrix g = new WeightedDirectedGraphMatrix(n);

            // 2) Read exactly n neighbor lines
            for (int u = 0; u < n; u++) {
                String neighLine = readRequiredLine(br, "neighbors for vertex " + u);
                List<Integer> vs = parseNeighbors(neighLine);

                for (int v : vs) {
                    if (v < 0 || v >= n) {
                        throw new IllegalArgumentException(String.format(
                            "Neighbor index out of bounds at vertex %d: %d (valid: 0..%d).",
                            u, v, n - 1
                        ));
                    }
                    if (!allowSelfLoops && v == u) continue; // skip self-loop unless allowed
                    // Add both directions with weight 1
                    g.addEdge(u, v, 1);
                    g.addEdge(v, u, 1);
                }
            }
            return g;
        }
    }

    // Reads next line; throws if EOF encountered before we get all n lines.
    private static String readRequiredLine(BufferedReader br, String what) throws IOException {
        String s = br.readLine();
        if (s == null) throw new IllegalArgumentException("Unexpected end of file while reading " + what + ".");
        return s.trim();
    }

    // Parse neighbors split by spaces and/or commas; "*" or blank => empty list.
    private static List<Integer> parseNeighbors(String s) {
        List<Integer> out = new ArrayList<>();
        if (s == null) return out;
        s = s.trim();
        if (s.isEmpty() || s.equals("*")) return out;

        // Split on one-or-more of comma or whitespace
        String[] parts = s.split("[,\\s]+");
        for (String t : parts) {
            if (t.isEmpty()) continue;
            try {
                out.add(Integer.parseInt(t));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Not an integer in neighbors list: '" + t + "' from line: '" + s + "'");
            }
        }
        return out;
    }

    // Quick sanity check
    public static void main(String[] args) throws Exception {
        // Example input:
        // 4
        // 1 2
        // 0
        // 0
        // *
        Path p = Paths.get("./TestGenerator/Tests/ConComp/sample.txt");
        WeightedDirectedGraphMatrix g = UUGLoader.loadFromFile(p);
        g.printGraph();
    }
}

