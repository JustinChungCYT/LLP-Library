package TestGenerator;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class IntArrayFileLoader {
    public static int[] load(String pathStr) throws IOException {
        Path path = Path.of(pathStr);
        return load(path);
    }

    public static int[] load(Path path) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            // 1) Read first non-empty line -> n
            String line;
            int n = -1;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try {
                        n = Integer.parseInt(line);
                        if (n < 0) {
                            throw new IllegalArgumentException("n must be non-negative, got: " + n);
                        }
                        break;
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("First non-empty line must be an integer (n). Got: \"" + line + "\"", e);
                    }
                }
            }
            if (n < 0) {
                throw new IllegalArgumentException("Missing first line with n.");
            }

            // 2) Read integers until we have n of them
            int[] arr = new int[n];
            int filled = 0;

            while (filled < n && (line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\s+");
                for (String p : parts) {
                    if (p.isEmpty()) continue;
                    try {
                        arr[filled++] = Integer.parseInt(p);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Expected an integer, got: \"" + p + "\"", e);
                    }
                    if (filled == n) break;
                }
            }

            if (filled < n) {
                throw new IllegalArgumentException("Expected " + n + " integers, but only found " + filled + ".");
            }

            // Detect if there are extra numbers beyond n
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] parts = line.trim().split("\\s+");
                    for (String p : parts) {
                        if (!p.isEmpty()) {
                            throw new IllegalArgumentException("Found extra data beyond the expected " + n + " integers: \"" + p + "\"");
                        }
                    }
                }
            }

            return arr;
        }
    }

    // Small demo: pass the file path as the first argument
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java IntArrayFileLoader <path-to-file>");
            System.exit(1);
        }
        Path path = Path.of(args[0]);
        int[] arr = load(path);
        System.out.println("Loaded array: " + Arrays.toString(arr));
    }
}
