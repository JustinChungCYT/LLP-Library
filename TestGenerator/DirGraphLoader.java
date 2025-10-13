package TestGenerator;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class DirGraphLoader {
    public static WeightedDirectedGraphMatrix loadFromFile(String pathStr) throws IOException {
        Path path = Path.of(pathStr);
        return loadFromFile(path);
    }

    public static WeightedDirectedGraphMatrix loadFromFile(Path path) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(path)) {
            // 1) read number of vertices (skip leading blank lines)
            String line;
            while ((line = br.readLine()) != null && line.trim().isEmpty()) {}
            if (line == null) throw new IllegalArgumentException("Missing vertex count on first line.");
            int n;
            try {
                n = Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("First non-empty line must be an integer vertex count, found: " + line);
            }
            if (n < 0) throw new IllegalArgumentException("Vertex count must be non-negative.");

            WeightedDirectedGraphMatrix g = new WeightedDirectedGraphMatrix(n);

            // 2) For each vertex v = 0..n-1, read 2 lines: destinations, then weights
            for (int v = 0; v < n; v++) {
                String destLine = readRequiredLine(br, "destinations for vertex " + v);
                String wtsLine  = readRequiredLine(br, "weights for vertex " + v);

                List<Integer> dests = parseCsvInts(destLine);
                List<Integer> wts   = parseCsvInts(wtsLine);

                if (dests.size() != wts.size()) {
                    throw new IllegalArgumentException(String.format(
                        "Line pair for vertex %d has %d destinations but %d weights. Lines were:\n'%s'\n'%s'",
                        v, dests.size(), wts.size(), destLine, wtsLine
                    ));
                }

                for (int i = 0; i < dests.size(); i++) {
                    int to = dests.get(i);
                    int w  = wts.get(i);
                    if (to < 0 || to >= n) {
                        throw new IllegalArgumentException(String.format(
                            "Destination index out of bounds for vertex %d: %d (valid: 0..%d).",
                            v, to, n - 1
                        ));
                    }
                    g.addEdge(v, to, w);
                }
            }
            return g;
        }
    }

    // Reads the next line; throws if EOF (we expect exactly 2 lines per vertex).
    private static String readRequiredLine(BufferedReader br, String what) throws IOException {
        String s = br.readLine();
        if (s == null) throw new IllegalArgumentException("Unexpected end of file while reading " + what + ".");
        return s.trim();
    }

    // Parses a CSV list of integers; empty or blank line -> empty list (no edges).
    private static List<Integer> parseCsvInts(String s) {
        List<Integer> out = new ArrayList<>();
        if (s == null) return out;
        s = s.trim();
        if (s.isEmpty() || s.equals("*")) return out;

        String[] parts = s.split(",");
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) continue;
            try {
                out.add(Integer.parseInt(t));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Not an integer in list: '" + t + "' from line: '" + s + "'");
            }
        }
        return out;
    }

    public static void main(String[] args) {
        try {
            WeightedDirectedGraphMatrix graph = DirGraphLoader.loadFromFile("./TestGenerator/Tests/BF/test2.txt");
            graph.printGraph();
        } catch (IOException e) { e.printStackTrace(); }
    }
}

