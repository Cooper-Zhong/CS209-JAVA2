package lab8;

import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class prac1 {
    static int totalOccurrences = 0;

    public static void main(String[] args) {
        // 输入要搜索的关键字
        String keyword = getInputKeyword();

        // 指定目标目录
        String targetDirectory = "/Users/cooperz/SUSTech/2023_Fall/CS209_Java2/CS209-JAVA2-repo/jdk8"; // 修改为您的目标目录

        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(6); // 使用固定大小线程池

        try {
            Instant startTime = Instant.now();
            // 创建任务列表
            List<Callable<Integer>> tasks = new ArrayList<>();

            // 递归遍历目录并为每个文件创建一个任务
            processDirectory(new File(targetDirectory), tasks, keyword);

            // 执行任务并获取Future对象列表
            List<Future<Integer>> futures = executor.invokeAll(tasks);

            // 计算总出现次数
            int totalOccurrences = futures.stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                            return 0;
                        }
                    })
                    .reduce(0, Integer::sum);

            System.out.println("Occurrences of " + keyword + ": " + totalOccurrences);
            Instant endTime = Instant.now();
            System.out.println("Time elapsed: "
                    + Duration.between(startTime, endTime).toMillis() + " ms\n");

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();

        }
    }

    // 递归遍历目录并创建任务
    private static void processDirectory(File directory, List<Callable<Integer>> tasks, String keyword) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    tasks.add(() -> countOccurrencesInFile(file, keyword));
                } else if (file.isDirectory()) {
                    processDirectory(file, tasks, keyword);
                }
            }
        }
    }

    // 从用户输入获取关键字
    private static String getInputKeyword() {
        System.out.print("Enter keyword (e.g. volatile): ");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine().trim();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    // 统计文件中关键字的出现次数
    private static int countOccurrencesInFile(File file, String keyword) throws IOException {

        int count = 0;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] words = line.split("\\s+");
                for (String word : words) {
                    if (word.equals(keyword)) {
                        count++;
                    }
                }
            }
        } catch (Exception e) {
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        return count;

    }
}
