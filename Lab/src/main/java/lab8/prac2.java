package lab8;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.*;

public class prac2 {
    public static void main(String[] args) {
        // 输入要搜索的关键字
        String keyword = getInputKeyword();

        // 指定目标目录
        String targetDirectory = "/Users/cooperz/SUSTech/2023_Fall/CS209_Java2/CS209-JAVA2-repo/jdk8"; // 修改为您的目标目录

        // 创建线程池
        ExecutorService executor = Executors.newCachedThreadPool(); // 使用缓存线程池
//        ExecutorService executor = Executors.newFixedThreadPool(6);

        try {
            // 创建任务列表
            List<Callable<String>> tasks = new ArrayList<>();

            // 递归遍历目录并为每个文件创建一个任务
            processDirectory(new File(targetDirectory), tasks, keyword);

            // 使用invokeAny并监控线程池的最大线程数
            String firstMatchingFile = executor.invokeAny(tasks);

            System.out.println("Found the first file that contains " + keyword + ": " + firstMatchingFile);

            if (executor instanceof ThreadPoolExecutor) {
                System.out.println("Largest pool size: " + ((ThreadPoolExecutor) executor).getLargestPoolSize());
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdownNow(); // 终止线程池
        }
    }

    // 递归遍历目录并创建任务
    private static void processDirectory(File directory, List<Callable<String>> tasks, String keyword) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    tasks.add(() -> searchFileForKeyword(file, keyword));
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

    // 搜索文件中是否包含关键字，返回文件路径或抛出异常
    private static String searchFileForKeyword(File file, String keyword) throws IOException {
        String content = Files.readString(file.toPath());
        if (content.contains(keyword)) {
            return file.getPath();
        } else {
            throw new NoSuchElementException();
        }
    }
}
