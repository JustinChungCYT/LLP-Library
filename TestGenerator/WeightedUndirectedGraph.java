package TestGenerator;

import java.util.*;

/**
 * Weighted undirected graph representation for MST algorithms.
 */
public class WeightedUndirectedGraph {
    public static class Edge implements Comparable<Edge> {
        public final int u, v;
        public final int weight;

        public Edge(int u, int v, int weight) {
            // Store with smaller vertex first for consistency
            if (u <= v) {
                this.u = u;
                this.v = v;
            } else {
                this.u = v;
                this.v = u;
            }
            this.weight = weight;
        }

        public int other(int vertex) {
            if (vertex == u) return v;
            if (vertex == v) return u;
            throw new IllegalArgumentException("Vertex not in edge");
        }

        @Override
        public int compareTo(Edge other) {
            // Compare by weight first
            if (this.weight != other.weight) {
                return Integer.compare(this.weight, other.weight);
            }
            // Tie-break by vertex pairs (deterministic)
            if (this.u != other.u) {
                return Integer.compare(this.u, other.u);
            }
            return Integer.compare(this.v, other.v);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Edge)) return false;
            Edge e = (Edge) obj;
            return this.u == e.u && this.v == e.v && this.weight == e.weight;
        }

        @Override
        public int hashCode() {
            return Objects.hash(u, v, weight);
        }

        @Override
        public String toString() {
            return "(" + u + "," + v + "," + weight + ")";
        }
    }

    private final int n;
    private final List<Edge> edges;
    private final List<List<Edge>> adjacency; // adjacency[v] = edges incident to v

    public WeightedUndirectedGraph(int n) {
        this.n = n;
        this.edges = new ArrayList<>();
        this.adjacency = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            adjacency.add(new ArrayList<>());
        }
    }

    public int getNumVertices() {
        return n;
    }

    public List<Edge> getEdges() {
        return new ArrayList<>(edges);
    }

    public List<Edge> getIncidentEdges(int v) {
        return new ArrayList<>(adjacency.get(v));
    }

    public void addEdge(int u, int v, int weight) {
        Edge e = new Edge(u, v, weight);
        edges.add(e);
        adjacency.get(u).add(e);
        adjacency.get(v).add(e);
    }

    public int getTotalWeight() {
        int total = 0;
        for (Edge e : edges) {
            total += e.weight;
        }
        return total;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Graph with ").append(n).append(" vertices and ").append(edges.size()).append(" edges:\n");
        for (Edge e : edges) {
            sb.append("  ").append(e).append("\n");
        }
        return sb.toString();
    }
}
