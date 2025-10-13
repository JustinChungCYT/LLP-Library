import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.Arrays;
import java.util.Random;

public class LlpReduceGeneric {

  public interface IntReducer {
    int identity();
    int combine(int a, int b);
  }

  public static final IntReducer SUM = new IntReducer() {
    public int identity() { return 0; }
    public int combine(int a, int b) { return a + b; }
  };
  public static final IntReducer MAX = new IntReducer() {
    public int identity() { return Integer.MIN_VALUE; }
    public int combine(int a, int b) { return Math.max(a, b); }
  };

  public static int seqReduce(int[] A) {
    int res = 0;
    long t1 = System.nanoTime();
    for (int a: A) res += a;
    long t2 = System.nanoTime();
    System.out.println("SeqReduce: " + (t2 - t1) + " ns.");
    return res;
  }

  public static int parallelReduceLLP(int[] A, IntReducer op, int parallelism) throws InterruptedException {
    final int n = A.length;
    if (n == 0) throw new IllegalArgumentException("empty input");
    if (n == 1) return A[0];

    final int N = nextPow2(n);                  // #leaves after padding
    final int[] B = Arrays.copyOf(A, N);        // leaves array
    if (n < N) Arrays.fill(B, n, N, op.identity());

    final int[] G = new int[N];                 // internal nodes: indices 1..N-1
    final AtomicIntegerArray rem = new AtomicIntegerArray(N);
    for (int j = 1; j < N / 2; j++) rem.set(j, 2);      // two internal children
    for (int j = N / 2; j < N; j++) rem.set(j, 0);      // parents-of-leaves ready

    final BlockingQueue<Integer> q = new LinkedBlockingQueue<>();
    for (int j = N / 2; j < N; j++) q.add(j);

    final CountDownLatch done = new CountDownLatch(N - 1);
    final ExecutorService pool = Executors.newFixedThreadPool(parallelism);

    Runnable worker = () -> {
      try {
        while (done.getCount() > 0) {
          Integer j = q.poll(50, TimeUnit.MILLISECONDS);
          if (j == null) continue;

          if (j >= N / 2) {
            // combine leaf pair: B[2j-N], B[2j-N+1]
            int idx0 = 2 * j - N;
            G[j] = op.combine(B[idx0], B[idx0 + 1]);
          } else {
            int left = 2 * j, right = left + 1;
            G[j] = op.combine(G[left], G[right]);
          }

          done.countDown();

          if (j > 1) {
            int parent = j / 2;
            if (rem.decrementAndGet(parent) == 0) q.add(parent);
          }
        }
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    };

    long t1 = System.nanoTime();
    for (int p = 0; p < parallelism; p++) pool.execute(worker);
    done.await();
    long t2 = System.nanoTime();
    System.out.println("ParReduce: " + (t2 - t1) + " ns.");
    pool.shutdownNow();
    return G[1];
  }

  private static int nextPow2(int x) {
    // ceil to power of two for x>=1
    return 1 << (32 - Integer.numberOfLeadingZeros(x - 1));
  }

  public static void main(String[] args) {
    int arraySize = 10000000;
    int[] A = new int[arraySize];
    Random random = new Random(42);

    // Populate the array with random integers
    for (int i = 0; i < arraySize; i++) A[i] = random.nextInt(100);

    try {
      int sumPar = LlpReduceGeneric.parallelReduceLLP(A, MAX, Runtime.getRuntime().availableProcessors());
      int sumSeq = LlpReduceGeneric.seqReduce(A);
      System.out.println("sumPar: " + sumPar + ", sumSeq: " + sumSeq);
    } catch (InterruptedException e){}
  }
}