package org.example;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.*;

public class Client {

    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final String UPLOAD_FOLDER = "Client/Upload/";
    private static final String DOWNLOAD_FOLDER = "Client/Download/";
    static Map<String, Future<?>> futures = new ConcurrentHashMap<>();
    static ExecutorService executor = Executors.newFixedThreadPool(6);
    public static Map<String, TransferTask> TaskMap = new ConcurrentHashMap<>();


    public static void main(String[] args) {
        try {
            // main socket
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            OutputStream outputStream = socket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(outputStream);
            InputStream inputStream = socket.getInputStream();
            DataInputStream dis = new DataInputStream(inputStream);
            PrintWriter out = new PrintWriter(outputStream, true);

            System.out.println("Connected to server " + SERVER_IP + ":" + SERVER_PORT);
            dos.writeUTF("main");

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
                        uploadFolder(UPLOAD_FOLDER);
                        break;
                    case "2":
                        queryResources(dis, dos);
                        System.out.print("Enter the file/folder name to download: ");
                        downloadFolder(sc.nextLine(), dis, dos);
                        break;
                    case "3":
                        queryTasks(); // Query all tasks and their progress
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
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        break;
                    case "7":
                        dos.writeUTF("exit");
                        out.println("exit");
                        break label;
                    default:
                        System.out.println("Invalid choice.");
                }
            }

            out.close();
            socket.close();
            System.out.println("Disconnected from server.");
            executor.shutdown();
            System.exit(0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void queryResources(DataInputStream dis, DataOutputStream dos) {
        try {
            dos.writeUTF("query");
            String fileName;
            while (!(fileName = dis.readUTF()).equals("END_OF_LIST")) {
                System.out.print(fileName + "\t");
            }
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void cancelTask(String name) {
        TransferTask task = TaskMap.get(name);
        if (task == null) {
            System.out.println("No such file.");
            return;
        }
        task.cancel();
    }

    private static void resumeTask(String name) {
        TransferTask task = TaskMap.get(name);
        if (task == null) {
            System.out.println("No such file.");
            return;
        }
        task.resume();
    }

    private static void pauseTask(String name) {
        TransferTask task = TaskMap.get(name);
        if (task == null) {
            System.out.println("No such file.");
            return;
        }
        task.pause();
    }

    private static void uploadFolder(String folderName) {
        File folder = new File(folderName);
        File[] files = folder.listFiles();
        if (files != null && files.length>0) {
            for (File file : files) {
                if (file.isDirectory()) {
                    uploadFolder(folderName + file.getName() + "/");
                } else {
                    try {
                        Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                        TransferTask task = new TransferTask(socket, file, "upload");
                        TaskMap.put(file.getName(), task);
                        Future<?> future = executor.submit(task);
                        futures.put(file.getName(), future);

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } else {
            System.out.println("No files found in the folder: " + folder.getName());
        }
    }


    private static void downloadFolder(String fileName, DataInputStream dis, DataOutputStream dos) {
        List<String> files = askName(fileName, dis, dos); // 获得文件夹下的所有文件名.
        for (String file : files) {
            try {
                Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                TransferTask task = new TransferTask(socket, new File(file), "download");
                file = getFileName(file);
                TaskMap.put(file, task);
                Future<?> future = executor.submit(task);
                futures.put(file, future);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String getFileName(String filePath) {
        int lastSeparatorIndex = filePath.lastIndexOf('/');
        if (lastSeparatorIndex >= 0 && lastSeparatorIndex < filePath.length() - 1) {
            return filePath.substring(lastSeparatorIndex + 1);
        }
        return filePath; // 如果字符串不包含斜杠，则直接返回原始字符串
    }

    private static List<String> askName(String fileName, DataInputStream dis, DataOutputStream dos) {
        // 获得一个文件到底是文件还是文件夹，如果是文件夹，拿到文件夹下的所有文件名
        try {
            dos.writeUTF("ask");
            dos.writeUTF(fileName);
            ArrayList<String> names = new ArrayList<>();
            String name;
            while (!(name = dis.readUTF()).equals("END_OF_LIST")) {
                System.out.print(name + "\t");
                names.add(name);
            }
            System.out.println();
            return names;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void queryTasks() {
        System.out.println("====================================");
        System.out.println("Current tasks:");
        if (TaskMap.isEmpty()) {
            System.out.println("No tasks.");
            System.out.println("============");
            return;
        }
        for (Map.Entry<String, TransferTask> entry : TaskMap.entrySet()) {
            String fileName = entry.getKey();
            TransferTask task = entry.getValue();
            String status = task.isPaused ? "Paused" : "In Progress";
            long current = task.currentBytes;
            long total = task.totalBytes;
            System.out.print(task.direction + ": ");
            System.out.print(fileName + ": " + (current/1024) + "/" + (total/1024) + " KB\t");
            System.out.printf("%.2f", (current*100.0/total));
            System.out.println("%\t" + status);
            System.out.println("====================================");
        }
    }
}
