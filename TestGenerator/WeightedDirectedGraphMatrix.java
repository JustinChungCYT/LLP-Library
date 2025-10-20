package TestGenerator;

import java.util.ArrayList;
import java.util.List;

public class WeightedDirectedGraphMatrix {
    private static final int INF = Integer.MAX_VALUE / 4;
    private int[][] adjMatrix;
    private int numVertices;
    private ArrayList<ArrayList<Integer>> parents;

    public WeightedDirectedGraphMatrix(int numVertices) {
        this.numVertices = numVertices;
        parents = new ArrayList<ArrayList<Integer>>();
        adjMatrix = new int[numVertices][numVertices];
        // Initialize all weights to 0 (or a value indicating no edge)
        for (int i = 0; i < numVertices; i++) {
            parents.add(new ArrayList<Integer>());
            for (int j = 0; j < numVertices; j++)
                adjMatrix[i][j] = INF;
        }
    }

    public int getNumVertices() { return numVertices; }
    public ArrayList<Integer> getParents(int to) { return parents.get(to); }

    public List<Integer> getChildren(int source) {
        List<Integer> children = new ArrayList<>();
        for (int i = 0; i < numVertices; i++)
            if (adjMatrix[source][i] < INF)
                children.add(i);
        return children;
    }

    public void addEdge(int source, int destination, int weight) {
        if (source >= 0 && source < numVertices && destination >= 0 && destination < numVertices) {
            adjMatrix[source][destination] = weight;
            parents.get(destination).add(source);
        } else System.out.println("Invalid vertex index.");
    }

    public int getWeight(int source, int destination) {
        if (source >= 0 && source < numVertices && destination >= 0 && destination < numVertices)
            return adjMatrix[source][destination];
        return -1; // Or throw an exception
    }

    public void printGraph() {
        for (int i = 0; i < numVertices; i++) {
            for (int j = 0; j < numVertices; j++) {
                int w = adjMatrix[i][j];
                if (w == INF) System.out.print("* ");
                else System.out.print(adjMatrix[i][j] + " ");
            }
            System.out.println();
        }
    }
}
