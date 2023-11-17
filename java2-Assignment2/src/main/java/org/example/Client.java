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
    private static final String DOWNLOAD_FOLDER = "Download/";

    static ExecutorService executor = Executors.newFixedThreadPool(5);

    static List<Callable<String>> tasks = new ArrayList<>();
    private static Map<String, ProgressInfo> progressMap = new ConcurrentHashMap<>();


    public static void main(String[] args) {
        try {
            // main socket
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            OutputStream outputStream = socket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(outputStream);
            InputStream inputStream = socket.getInputStream();
            DataInputStream dis = new DataInputStream(inputStream);
            PrintWriter out = new PrintWriter(outputStream, true);
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

            System.out.println("Connected to server " + SERVER_IP + ":" + SERVER_PORT);
//            out.println("main");
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
                        cancelTask(sc.nextLine(), dos);
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


    private static void downloadFolder(String fileName, DataInputStream dis, DataOutputStream dos) {
        List<String> files = askName(fileName, dis, dos); // 获得文件夹下的所有文件名.
        for (String file : files) {
            executor.submit(() -> download(file)); //创建新的socket，并发下载文件
        }
    }


    private static void download(String fileName) {
        try {
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            InputStream inputStream = socket.getInputStream();
            DataInputStream dis = new DataInputStream(inputStream);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            File file = new File(fileName);


            // 检查并创建文件夹
            File directory = new File(DOWNLOAD_FOLDER + getDirectoryName(fileName));
            if (!directory.exists()) {
                directory.mkdirs(); // 递归创建文件夹
            }

            dos.writeUTF("download");
            dos.writeUTF(fileName);

            long fileSize = dis.readLong();
            ProgressInfo progress = new ProgressInfo("download", 0, fileSize);
            progressMap.put(file.getName(), progress);
            boolean canceled = false;

            FileOutputStream fileOutputStream = new FileOutputStream(DOWNLOAD_FOLDER + fileName);
            byte[] buffer = new byte[4096];
            int bytesRead;

            label:
            while ((bytesRead = inputStream.read(buffer)) != -1) {

                while (progress.isPaused()) {
                    if (progress.isCanceled()) {
                        canceled = true;
                        break label;
                    }
                    try {
                        Thread.sleep(100); // Sleep for a short time and check again
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (progress.isCanceled()) {
                    canceled = true;
                    break;
                }

                fileOutputStream.write(buffer, 0, bytesRead);
                updateProgress(file.getName(), bytesRead);
            }

            fileOutputStream.close();
            if (canceled) {
                System.out.println("====================================");
                System.out.println("Download canceled: " + fileName);
                System.out.println("====================================");
            }
            else {
                System.out.println("====================================");
                System.out.println("File downloaded: " + fileName);
                System.out.println("====================================");
            }

            dis.close();
            socket.close();
            progressMap.remove(file.getName());



        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static String getDirectoryName(String fileName) {
        int lastSeparatorIndex = fileName.lastIndexOf('/');
        if (lastSeparatorIndex != -1) {
            return fileName.substring(0, lastSeparatorIndex);
        }
        return "";
    }

    private static void receiveFile(InputStream in, String fileName) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(DOWNLOAD_FOLDER + fileName);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void uploadFolder(String folderName) {
        File folder = new File(folderName);
        File[] files = folder.listFiles();
        if (files != null && files.length>0) {
            for (File file : files) {
                if (file.isDirectory()) {
                    uploadFolder(folderName + file.getName() + "/");
                } else {
                    executor.submit(() -> upload(file));
                }
            }
        } else {
            System.out.println("No files found in the folder: " + folder.getName());
        }
    }

    private static void upload(File file) {
        //concurrently upload files
        try {
            //new socket for file transfer
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            OutputStream outputStream = socket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(outputStream);

            dos.writeUTF("upload");
            String fileName = file.getPath().substring(UPLOAD_FOLDER.length());
//            String fileName = file.getPath();

            dos.writeUTF(fileName); //fileName with relative path
            dos.writeLong(file.length()); //size

            ProgressInfo progressInfo = new ProgressInfo("upload", 0, file.length());
            progressMap.put(file.getName(), progressInfo);

            // Create input stream to read the file
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytesRead = 0;
            boolean canceled = false;


            // Send file content to the server
            label:
            while ((bytesRead = fis.read(buffer)) != -1) {

                while (progressInfo.isPaused()) {
                    if (progressInfo.isCanceled()) {
                        canceled = true;
                        break label;
                    }
                    try {
                        Thread.sleep(100); // Sleep for a short time and check again
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (progressInfo.isCanceled()) {
                    canceled = true;
                    break;
                }
                outputStream.write(buffer, 0, bytesRead);
                updateProgress(file.getName(), bytesRead);
            }

            if (canceled) {
                System.out.println("====================================");
                System.out.println("Upload canceled: " + file.getName());
                System.out.println("====================================");
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
    private static void cancelTask(String fileName, DataOutputStream dos) {
        ProgressInfo progressInfo = progressMap.get(fileName);
        if (progressInfo != null) {
            progressInfo.setPaused(false);
            progressInfo.setCanceled(true);
        }
        try {
            dos.writeUTF("cancel");
            dos.writeUTF(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Query all upload tasks and their progress
    private static void queryAllTasks() {
        System.out.println("====================================");
        System.out.println("Current tasks:");
        if (progressMap.isEmpty()) {
            System.out.println("No tasks.");
            System.out.println("============");
            return;
        }

        for (Map.Entry<String, ProgressInfo> entry : progressMap.entrySet()) {
            String fileName = entry.getKey();
            ProgressInfo progressInfo = entry.getValue();
            String task = progressInfo.getTask();
            String status = progressInfo.isPaused() ? "Paused" : "In Progress";
            long current = progressInfo.getCurrentBytes();
            long total = progressInfo.getTotalBytes(); // avoid division by zero
            System.out.print(task + ": ");
            System.out.print(fileName + ": " + (current/1024) + "/" + (total/1024) + " KB\t");
            System.out.printf("%.2f", (current*100.0/total));
            System.out.println("%\t" + status);
            System.out.println("====================================");
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
