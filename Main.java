import java.util.Random;
import java.time.LocalTime;
import java.time.Duration;

public class Main {

    private static int seqReduce(int[] A) {
        int sum = 0;
        for (int v : A) sum += v;
        return sum;
    }

    public static void main(String[] args) throws Exception {
        int arraySize = 10000000;
        int[] A1 = new int[arraySize];
        Random random = new Random(42);

        // Populate the array with random integers
        for (int i = 0; i < arraySize; i++) {
            A1[i] = random.nextInt(100); // Generates a random integer within the full int range
        }

        LocalTime currentTime = LocalTime.now();
        int seqSum = seqReduce(A1);
        System.out.println("Total time for seq: " +
            Duration.between(currentTime, LocalTime.now()).toMillis() + " ms");
        System.out.println("seq sum = " + seqSum);

        currentTime = LocalTime.now();
        int sum1 = LlpReduceGeneric.parallelReduceLLP(A1, LlpReduceGeneric.SUM, Math.min(4, Runtime.getRuntime().availableProcessors()));
        System.out.println("Total time for LLP: " +
            Duration.between(currentTime, LocalTime.now()).toMillis() + " ms");
        System.out.println("sum = " + sum1);

        // int[] A2 = {7, -2, 9, 0, 4, 6, 3}; // n=7
        // int mx = LlpReduceGeneric.parallelReduceLLP(A2, LlpReduceGeneric.MAX, 4);
        // System.out.println("max = " + mx); // 9
    }
}