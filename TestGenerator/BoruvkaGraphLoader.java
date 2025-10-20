package TestGenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads weighted undirected graphs for Boruvka's MST algorithm.
 *
 * File format:
 * Line 1: n (number of vertices)
 * Line 2: m (number of edges)
 * Next m lines: u v weight (each edge, undirected)
 *
 * Example:
 * 4
 * 5
 * 0 1 10
 * 0 2 6
 * 0 3 5
 * 1 3 15
 * 2 3 4
 */
public class BoruvkaGraphLoader {

    public static WeightedUndirectedGraph loadFromFile(String pathStr) throws IOException {
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

            // Read m
            line = br.readLine();
            if (line == null) {
                throw new IllegalArgumentException("Missing edge count");
            }
            int m = Integer.parseInt(line.trim());
            if (m < 0) {
                throw new IllegalArgumentException("m must be non-negative, got: " + m);
            }

            WeightedUndirectedGraph graph = new WeightedUndirectedGraph(n);

            // Read edges
            for (int i = 0; i < m; i++) {
                line = br.readLine();
                if (line == null) {
                    throw new IllegalArgumentException("Missing edge line " + i);
                }
                String[] parts = line.trim().split("\\s+");
                if (parts.length != 3) {
                    throw new IllegalArgumentException("Expected 3 values per edge, got " + parts.length);
                }

                int u = Integer.parseInt(parts[0]);
                int v = Integer.parseInt(parts[1]);
                int weight = Integer.parseInt(parts[2]);

                if (u < 0 || u >= n || v < 0 || v >= n) {
                    throw new IllegalArgumentException("Invalid vertex indices: " + u + ", " + v);
                }

                graph.addEdge(u, v, weight);
            }

            return graph;
        }
    }
}
