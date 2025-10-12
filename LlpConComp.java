import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerArray;

import TestGenerator.GraphLoader;
import TestGenerator.GraphLoader.GraphData;

// Step 1 execution
class VmaxCallable implements Callable<String> {
    int id;
    int[] neighbors;
    final AtomicIntegerArray atomicVmax;
    final AtomicIntegerArray atomicParent;

    VmaxCallable(int id, int[] neighbors, AtomicIntegerArray atomicVmax, AtomicIntegerArray atomicParent) {
        this.id = id;
        this.neighbors = neighbors;
        this.atomicVmax = atomicVmax;
        this.atomicParent = atomicParent;
    }

    @Override
    public String call() throws Exception {
        int maxVmax = atomicVmax.get(id);
        for (int w: neighbors) maxVmax = Math.max(maxVmax, atomicParent.get(w));
        atomicVmax.set(id, maxVmax);
        // return "v: " + id + ", vmax[" + id + "]: " + atomicVmax.get(id);
        return "";
    }
}

// Step 2 execution
class SetParentCallable implements Callable<String> {
    int id;
    int[] neighbors;
    final AtomicIntegerArray atomicVmax;
    final AtomicIntegerArray atomicParent;

    SetParentCallable(int id, int[] neighbors, AtomicIntegerArray atomicVmax, AtomicIntegerArray atomicParent) {
        this.id = id;
        this.neighbors = neighbors;
        this.atomicVmax = atomicVmax;
        this.atomicParent = atomicParent;
    }

    @Override
    public String call() throws Exception {
        int curParent = atomicParent.get(id);
        if (id == curParent){
            int maxVmax = atomicVmax.get(id);
            for (int u = 0; u < atomicParent.length(); u++)
                if (atomicParent.get(u) == curParent) maxVmax = Math.max(maxVmax, atomicVmax.get(u));
            atomicParent.set(id, maxVmax);
        }
        return "";
    }
}

// Step 3 execution
class PtrJumpCallable implements Callable<String> {
    int id;
    int[] neighbors;
    final AtomicIntegerArray atomicParent;

    PtrJumpCallable(int id, int[] neighbors, AtomicIntegerArray atomicParent) {
        this.id = id;
        this.neighbors = neighbors;
        this.atomicParent = atomicParent;
    }

    @Override
    public String call() throws Exception {
        int curParent = atomicParent.get(id);
        int curGrandParent = atomicParent.get(curParent);
        if (curParent != curGrandParent) atomicParent.set(id, curGrandParent);
        return "";
    }
}

// Ptr-jump condition checker
class ParentCheckCallable {

}

public class LlpConComp {
    int[] V;
    int[][] E;
    int[][] graph;
    AtomicIntegerArray atomicParent;
    AtomicIntegerArray atomicVmax;
    ArrayList<VmaxCallable> vmaxCallables;
    ArrayList<SetParentCallable> setParentCallables;
    ArrayList<PtrJumpCallable> ptrJumpCallables;
    final ExecutorService pool;

    public LlpConComp(int[][] E, int[][] graph) {
        this.V = new int[graph.length];
        this.E = E;
        this.graph = graph;

        this.atomicParent = new AtomicIntegerArray(graph.length);
        this.atomicVmax = new AtomicIntegerArray(graph.length);
        this.vmaxCallables = new ArrayList<VmaxCallable>();
        this.setParentCallables = new ArrayList<SetParentCallable>();
        this.ptrJumpCallables = new ArrayList<PtrJumpCallable>();

        for (int i = 0; i < graph.length; i++) {
            V[i] = i;
            atomicParent.set(i, i);
            atomicVmax.set(i, i);
            
            // initialize parallelism
            vmaxCallables.add(new VmaxCallable(i, graph[i], atomicVmax, atomicParent));
            setParentCallables.add(new SetParentCallable(i, graph[i], atomicVmax, atomicParent));
            ptrJumpCallables.add(new PtrJumpCallable(i, graph[i], atomicParent));
        }
        this.pool = Executors.newFixedThreadPool(graph.length);
    }

    private void findComponents() throws InterruptedException {
            // Step 1
            pool.invokeAll(vmaxCallables);
            System.out.println("atomicVmax after Step 1:");
            for (int i = 0; i < atomicVmax.length(); i++) System.out.print(atomicVmax.get(i) + " ");
            System.out.println();

            // Step 2
            pool.invokeAll(setParentCallables);
            System.out.println("atomicParent after Step 2:");
            for (int i = 0; i < atomicParent.length(); i++) System.out.print(atomicParent.get(i) + " ");
            System.out.println();

            // Step 3
            boolean forbParent = true;
            while (forbParent) {
                forbParent = false;
                pool.invokeAll(ptrJumpCallables);
                
                for (int v: V){
                    int p = atomicParent.get(v);
                    if (p != atomicParent.get(p)) forbParent = true;
                }
            }
            System.out.println("atomicParent after Step 3");
            for (int i = 0; i < atomicParent.length(); i++) System.out.print(atomicParent.get(i) + " ");
            System.out.println();
    }

    public AtomicIntegerArray fastAlgo() throws InterruptedException {
        // check if there exists an edge (v, w) such that parent[v] < parent[w]
        try {
            boolean hasForbidden = true;
            while (hasForbidden) {
                hasForbidden = false;
                // Sequential check for now
                for (int u = 0; u < graph.length; u++)
                    for (int w: graph[u])
                        if (atomicParent.get(u) < atomicParent.get(w)) hasForbidden = true;

                if (hasForbidden) findComponents();
                System.out.println("");
            }
        } finally { pool.shutdown(); }
        return atomicParent;
    }

    public static void main(String[] args) {
        GraphData g;
        try {
            g = GraphLoader.loadGraph("TestGenerator/Tests/ConComp/test1.txt");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        int[][] graph;
        try {
            graph = GraphLoader.loadUndirectedGraph("TestGenerator/Tests/ConComp/test1.txt");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        LlpConComp component = new LlpConComp(g.getE(), graph);

        try {
            AtomicIntegerArray parent = component.fastAlgo();
            for (int i = 0; i < parent.length(); i++) {
                System.out.println("Vertex: " + i + ", Component: " + parent.get(i));
            }
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
    }
}
