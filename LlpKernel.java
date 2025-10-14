import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.BitSet;

public abstract class LlpKernel implements AutoCloseable {
    public final int INF = Integer.MAX_VALUE / 4;
    protected final int n;
    private final ExecutorService pool;

    public LlpKernel(int n, int threads) {
        this.n = n;
        this.pool = Executors.newFixedThreadPool(threads);
    }

    /* ===== Hooks for subclasses (primitive, no boxing) ===== */

    // Check if v should be considered in the current iteration
    protected abstract boolean eligible(int v);

    // Fobidden conditions 
    protected abstract IntPredicate forbiddens(int forbIdx);

    // Number of ordered advance steps per iteration (>=1).
    protected abstract int numAdvanceSteps();

    /**
     * Perform the 'stepIdx'-th advance step for vertex v.
     * Subclasses can switch(stepIdx) { case 0: stepA(v); case 1: stepB(v); ... }
     */
    protected abstract void advanceStep(int stepIdx, int v);

    /**
     * By default, every step runs over the same set L.
     * Override to use a different selection for a given step (e.g., run pointer-jumping on all vertices).
     * Return null to indicate “use the L provided”.
     */
    protected IntPredicate selectionForStep(int stepIdx) { return null; }

    /* ===== Orchestrator methods (reusable) ===== */

    /** Build forbidden set L in parallel; returns true if any were marked. */
    public final boolean collectForbidden(int forbIdx, BitSet L) {
        L.clear();
        // Capture method refs (no allocation per v once the lambda object is created)
        IntPredicate elig = this::eligible;
        IntPredicate forb = forbiddens(forbIdx);

        List<Callable<Void>> tasks = new ArrayList<>(n);
        for (int v = 0; v < n; v++) {
            int vv = v;
            tasks.add(() -> {
                if (elig.test(vv) && forb.test(vv))
                    synchronized (L) { L.set(vv); }
                return null;
            });
        }
        invokeAndWait(tasks);
        return !L.isEmpty();
    }

    /** Run the multi-step advance for the current iteration. */
    public void advance(BitSet L) {
        int steps = numAdvanceSteps();
        if (steps <= 0) return;

        for (int s = 0; s < steps; s++) {
            final int step = s;
            IntPredicate select = selectionForStep(step);
            if (select != null) {
                // Run step s over a predicate (e.g., all vertices, or a custom subset)
                parallelForEach(select, v -> advanceStep(step, v));
            } else {
                // Default: run step s over the current L
                parallelForEach(L, v -> advanceStep(step, v));
            }
        }
        // caller may choose to clear L outside; we don’t auto-clear here to allow re-use across steps if desired
    }

    /* ===== Parallel helpers (no boxing via primitive SAMs) ===== */

    protected final void parallelForEach(BitSet S, IntConsumer action) {
        if (S.isEmpty()) return;
        List<Callable<Void>> tasks = new ArrayList<>(S.cardinality());
        for (int v = S.nextSetBit(0); v >= 0; v = S.nextSetBit(v + 1)) {
            final int vv = v;
            tasks.add(() -> { action.accept(vv); return null; });
        }
        invokeAndWait(tasks);
    }

    protected final void parallelForEach(IntPredicate select, IntConsumer action) {
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int v = 0; v < n; v++) {
            final int vv = v;
            if (select.test(vv))
                tasks.add(() -> { action.accept(vv); return null; });
        }
        if (!tasks.isEmpty()) invokeAndWait(tasks);
    }

    private void invokeAndWait(List<Callable<Void>> tasks) {
        try {
            List<Future<Void>> fs = pool.invokeAll(tasks);
            for (Future<Void> f : fs) f.get(); // check exceptions from workers and ensure task completions
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    @Override public void close() { pool.shutdown(); }
}
