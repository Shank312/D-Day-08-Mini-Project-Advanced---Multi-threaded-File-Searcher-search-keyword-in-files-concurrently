
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileSearcher {

    public static final class SearchResult {
        public final Path file;
        public final int lineNumber;
        public final String lineText;

        public SearchResult(Path file, int lineNumber, String lineText) {
            this.file = Objects.requireNonNull(file);
            this.lineNumber = lineNumber;
            this.lineText = Objects.requireNonNull(lineText);
        }

        @Override
        public String toString() {
            return String.format("%s:%d: %s", file.toString(), lineNumber, lineText.trim());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 2) {
            System.out.println("Usage: java FileSearcher <root-directory> <keyword> [threads] [case-insensitive]");
            System.exit(1);
        }

        Path root = Paths.get(args[0]);
        String keyword = args[1];
        int threads = (args.length >= 3) ? Integer.parseInt(args[2]) : Runtime.getRuntime().availableProcessors();
        boolean caseInsensitive = (args.length >= 4) && Boolean.parseBoolean(args[3]);

        System.out.println("Starting search");
        System.out.println("Root: " + root.toAbsolutePath());
        System.out.println("Keyword: '" + keyword + "' (case-insensitive=" + caseInsensitive + ")");
        System.out.println("Threads: " + threads);

        long start = System.currentTimeMillis();
        List<SearchResult> results = search(root, keyword, threads, caseInsensitive);
        long end = System.currentTimeMillis();

        System.out.println();
        System.out.println("Search completed in " + (end - start) + " ms");
        System.out.println("Total matches: " + results.size());
        System.out.println();

        results.stream().limit(200).forEach(r -> System.out.println(r.toString()));
        if (results.size() > 200) {
            System.out.println("(truncated output — use results collection programmatically for full data)");
        }
    }

    public static List<SearchResult> search(Path root, String keyword, int threads, boolean caseInsensitive) throws InterruptedException {
        if (!Files.isDirectory(root)) {
            System.err.println("Provided root is not a directory: " + root);
            return Collections.emptyList();
        }

        Queue<SearchResult> results = new ConcurrentLinkedQueue<>();
        AtomicInteger filesScanned = new AtomicInteger(0);
        ReentrantLock printLock = new ReentrantLock();

        ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, threads));
        List<Future<?>> futures = new ArrayList<>();

        final String lookup = caseInsensitive ? keyword.toLowerCase() : keyword;

        try (Stream<Path> pathStream = Files.walk(root)) {
            List<Path> files = pathStream
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());

            System.out.println("Files to scan: " + files.size());

            for (Path file : files) {
                Callable<Void> task = () -> {
                    try {
                        searchInFile(file, lookup, caseInsensitive, results);
                    } catch (IOException e) {
                        printLock.lock();
                        try {
                            System.err.println("Failed reading: " + file + " — " + e.getMessage());
                        } finally {
                            printLock.unlock();
                        }
                    }

                    int scanned = filesScanned.incrementAndGet();
                    if (scanned % 50 == 0) {
                        printLock.lock();
                        try {
                            System.out.println("Files scanned: " + scanned + "/" + files.size());
                        } finally {
                            printLock.unlock();
                        }
                    }
                    return null;
                };

                futures.add(executor.submit(task));
            }

            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (ExecutionException e) {
                    System.err.println("Task failed: " + e.getCause());
                }
            }

        } catch (IOException e) {
            System.err.println("Directory walk failed: " + e.getMessage());
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("Executor did not terminate in time; forcing shutdown");
                executor.shutdownNow();
            }
        }

        List<SearchResult> resultList = new ArrayList<>(results);
        resultList.sort((a, b) -> {
            int cmp = a.file.toString().compareTo(b.file.toString());
            if (cmp != 0) return cmp;
            return Integer.compare(a.lineNumber, b.lineNumber);
        });

        return resultList;
    }

    private static void searchInFile(Path file, String lookup, boolean caseInsensitive, Queue<SearchResult> results) throws IOException {
        try {
            if (!Files.isReadable(file)) return;
            long size = Files.size(file);
            if (size > 200L * 1024 * 1024) return;
        } catch (IOException ignored) {
        }

        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                String hay = caseInsensitive ? line.toLowerCase() : line;
                if (hay.contains(lookup)) {
                    results.add(new SearchResult(file, lineNo, line));
                }
            }
        }
    }
}
