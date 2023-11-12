package org.example;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final String UPLOAD_FOLDER = "Upload/";

    static ExecutorService executor = Executors.newFixedThreadPool(5);

    static List<Callable<String>> tasks = new ArrayList<>();

    private static final Map<String, ProgressInfo> progressMap = new ConcurrentHashMap<>();


    public static void main(String[] args) {
        // 创建单个 Socket
        try {
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            OutputStream outputStream = socket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(outputStream);

            System.out.println("Connected to server " + SERVER_IP + ":" + SERVER_PORT);
            Scanner sc = new Scanner(System.in);

            label:
            while (true) {
                System.out.println("1 : Upload");
                System.out.println("2 : Download");
                System.out.println("3 : Query progress");
                System.out.println("4 : pause");
                System.out.println("5 : resume");
                System.out.println("6 : cancel");
                System.out.println("7 : exit");
                System.out.println("Enter your choice: ");
                String choice = sc.nextLine();
                System.out.println("====================================");
                switch (choice) {
                    case "1":
                        System.out.print("Enter the folder: ");
                        uploadFolder(sc.nextLine());
                        break;
                    case "2":
                        System.out.print("Enter the file name: ");
                        download(sc.nextLine());
                        break;
                    case "3":
                        queryAllTasks(); // Query all tasks and their progress
                        break;
                    case "4":
                        System.out.println("Enter the file name: ");
                        pauseTask(sc.nextLine());
                        break;
                    case "5":
                        System.out.println("Enter the file name: ");
                        resumeTask(sc.nextLine());
                        break;
                    case "6":
                        System.out.println("Enter the file name: ");
                        cancelTask(sc.nextLine());
                        break;
                    case "7":
                        break label;
                    default:
                        System.out.println("Invalid choice.");
                }
            }
            dos.writeUTF("client exit");
            dos.close();
            socket.close();
            System.out.println("Disconnected from server.");
            executor.shutdown(); // 关闭线程池

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void download(String fileName2) {
    }


    private static void uploadFolder(String folderName) {
        File folder = new File(folderName);
        File[] files = folder.listFiles();
        if (files != null && files.length>0) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // If it's a directory, recursively call the method
                    uploadFolder(file.getName());
                } else {
                    // If it's a file, submit it to the executor for concurrent uploading
                    executor.submit(() -> upload(file));
                }
            }
        } else {
            System.out.println("No files found in the folder: " + folder.getName());
        }
    }

    private static void upload(File file) {//concurrently upload files
        try {
            Socket socket = new Socket(SERVER_IP, SERVER_PORT); // socket for file transfer
            OutputStream outputStream = socket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(outputStream);

            dos.writeUTF("file");
            dos.writeUTF(file.getName()); //filename
            dos.writeLong(file.length()); //size

            ProgressInfo progressInfo = new ProgressInfo();
            progressInfo.setTotalBytes(file.length());
            progressMap.put(file.getName(), progressInfo);

            // Create input stream to read the file
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytesRead = 0;

            // Send file content to the server
            while ((bytesRead = fis.read(buffer)) != -1) {

                while (progressInfo.isPaused()) {
                    try {
                        Thread.sleep(100); // Sleep for a short time and check again
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (progressInfo.isCanceled()) {
                    dos.writeUTF("cancel"); //signal the server to cancel
                    progressMap.remove(file.getName());
                    fis.close();
                    dos.close();
                    socket.close();
                    return;
                }


                outputStream.write(buffer, 0, bytesRead);
                updateProgress(file.getName(), bytesRead);

            }

            // Signal the end of file
            fis.close();
            dos.close();
            socket.close();

            // 上传完成后从Map中移除上传进度信息
            progressMap.remove(file.getName());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Pause a specific upload task
    private static void pauseTask(String fileName) {
        ProgressInfo progressInfo = progressMap.get(fileName);
        if (progressInfo != null) {
            progressInfo.setPaused(true);
        }
    }

    // Resume a specific upload task
    private static void resumeTask(String fileName) {
        ProgressInfo progressInfo = progressMap.get(fileName);
        if (progressInfo != null) {
            progressInfo.setPaused(false);
        }
    }

    // Cancel a specific upload task
    private static void cancelTask(String fileName) {
        ProgressInfo progressInfo = progressMap.get(fileName);
        if (progressInfo != null) {
            progressInfo.setCanceled(true);
        }
    }

    // Query all upload tasks and their progress
    private static void queryAllTasks() {
        System.out.println("Current upload tasks:");
        if (progressMap.isEmpty()) {
            System.out.println("No tasks.");
            return;
        }

        for (Map.Entry<String, ProgressInfo> entry : progressMap.entrySet()) {
            String fileName = entry.getKey();
            ProgressInfo progressInfo = entry.getValue();
            String status = progressInfo.isPaused() ? "Paused" : "In Progress";
            long current = progressInfo.getCurrentBytes();
            long total = progressInfo.getTotalBytes(); // avoid division by zero
            System.out.print(fileName + ": " + (current/1024) + "/" + (total/1024) + " KB\t");
            System.out.printf("%.2f", (current/total*100.0));
            System.out.println("%\t" + status);
            System.out.println("====================================");
        }
    }


    // 查询上传进度
    private static void queryProgress() {
        System.out.println("progress for each file:");
        for (Map.Entry<String, ProgressInfo> entry : progressMap.entrySet()) {
            String fileName = entry.getKey();
            ProgressInfo progressInfo = entry.getValue();

        }
    }

    // 更新上传进度信息
    static void updateProgress(String fileName, int bytesRead) {
        ProgressInfo progressInfo = progressMap.get(fileName);
        if (progressInfo != null) {
            progressInfo.setCurrentBytes(progressInfo.getCurrentBytes() + bytesRead);
        }
    }
}
