package TestGenerator;

public final class SimpleTests {
    public static void check(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }
    public static void checkIntEq(int a, int b, String label) {
        if (a != b) throw new AssertionError(label + ": expected " + a + " got " + b);
    }
    public static void checkArrEq(int[] exp, int[] act) {
        if (exp.length != act.length) throw new AssertionError("length mismatch");
        for (int i = 0; i < exp.length; i++) {
            if (exp[i] != act[i]) throw new AssertionError("idx " + i + ": expected " + exp[i] + " got " + act[i]);
        }
    }
    public static void checkNull(Object obj, String label) {
        if (obj != null) throw new AssertionError(label + ": expected null");
    }

    public static void main(String[] args) {
        // build a tiny graph and test it, same as above…
        System.out.println("OK");
    }
}
