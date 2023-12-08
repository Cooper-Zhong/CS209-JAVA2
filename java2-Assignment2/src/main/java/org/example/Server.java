package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 12345;
    private static final String STORAGE_FOLDER = "Server/Storage/";
    private static final String RESOURCES_FOLDER = "Server/Resources/";
    private static final Map<String, ProgressInfo> progressMap = new ConcurrentHashMap<>();


    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server is listening on port " + PORT);
            ExecutorService clientExecutor = Executors.newFixedThreadPool(12);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getInetAddress());

                clientExecutor.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void handleClient(Socket clientSocket) {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            DataInputStream dis = new DataInputStream(inputStream);
            String signal = dis.readUTF(); //!!!!!!!

            if (signal.equals("main")) {
                handleMainClient(clientSocket, inputStream, dis); // operations: upload, download ...
            } else if (signal.equals("upload")) {
                handleUpload(clientSocket, inputStream, dis); // filename, filesize.
            } else if (signal.equals("download")) {
                handleDownload(clientSocket, dis); // filename
            }

        } catch (IOException e) {
            System.out.println("Client disconnected.");
        }
    }

    private static void handleDownload(Socket clientSocket, DataInputStream dis) {
        try {
            String filename = dis.readUTF();
            sendFile(clientSocket, filename);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void sendFile(Socket clientSocket, String fileName) {
        try {
            File fileToSend = new File(RESOURCES_FOLDER + fileName);
            if (fileToSend.exists() && !fileToSend.isDirectory()) {
                OutputStream outputStream = clientSocket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(outputStream);
                FileInputStream fis = new FileInputStream(fileToSend);

                long fileSize = fileToSend.length();
                ProgressInfo progressInfo = new ProgressInfo("download", 0, fileSize);
                progressMap.put(fileName, progressInfo);
                boolean canceled = false;

                dos.writeLong(fileSize); // file size

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    if (progressInfo.isCanceled()) {// 检查是否取消
                        canceled = true;
                        break;
                    }
                    updateProgress(fileName, bytesRead);
                    outputStream.write(buffer, 0, bytesRead);

                }

                if (fileSize != progressInfo.getCurrentBytes()) {
                    canceled = true;
                }

                if (canceled) {
                    File file = new File(STORAGE_FOLDER + fileName);
                    file.delete();
                    System.out.println("download " + fileName + " canceled by the client.");
                }

                progressMap.remove(fileName);
                fis.close();
                dos.close(); //????

                outputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Client canceled the download: " + fileName);
        }
    }


    private static void handleMainClient(Socket clientSocket, InputStream in, DataInputStream dis) {
        try {
            String op;
            label:
            while (true) {
                op = dis.readUTF();
                String fileName;
                switch (op) {
                    case "query":
                        sendResourceList(clientSocket);
                        break;
                    case "ask":
                        sendFileList(clientSocket);
                        break;
                    case "cancel":
                        fileName = dis.readUTF();
                        ProgressInfo info = progressMap.get(fileName);
                        if (info == null) {
                            System.out.println("No file " + fileName + " is transferring.");
                            break;
                        }
                        info.setCanceled(true);
                        break;
                    case "exit":
                        System.out.println("Client exits.");
                        clientSocket.close();
                        break label;
                    default:
                        break label;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Client disconnected.");
        }

    }

    private static List<String> getSubFiles(String fileName) {
        List<String> fileList = new ArrayList<>();
        File file = new File(fileName);

        if (!file.exists()) {
            System.out.println("File or folder does not exist.");
            return fileList;
        }

        if (file.isFile()) {
            fileList.add(file.getName());
        } else if (file.isDirectory()) {
            getAllFilesRecursive(file, fileList, fileName);
        }

        return fileList;
    }

    private static void getAllFilesRecursive(File folder, List<String> fileList, String basePath) {
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    getAllFilesRecursive(file, fileList, basePath);
                } else {
                    // 将文件相对于资源目录的路径添加到列表中
                    fileList.add(basePath.substring(RESOURCES_FOLDER.length()) + file.getPath().substring(basePath.length()));
                }
            }
        }
    }

    private static void sendFileList(Socket clientSocket) {
        try {
            OutputStream outputStream = clientSocket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(outputStream);
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());

            String askedName = dis.readUTF(); // read file name
            List<String> fileList = getSubFiles(RESOURCES_FOLDER + askedName);
            for (String fileName : fileList) {
                dos.writeUTF(fileName);
            }
            dos.writeUTF("END_OF_LIST");


        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void sendResourceList(Socket clientSocket) {
        try {
            OutputStream outputStream = clientSocket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(outputStream);
            File resourcesFolder = new File(RESOURCES_FOLDER);
            File[] files = resourcesFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    dos.writeUTF(file.getName());
                }
            }
            dos.writeUTF("END_OF_LIST");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static String getDirectoryName(String fileName) {
        int lastSeparatorIndex = fileName.lastIndexOf('/');
        if (lastSeparatorIndex != -1) {
            return fileName.substring(0, lastSeparatorIndex);
        }
        return "";
    }

    private static void handleUpload(Socket clientSocket, InputStream inputStream, DataInputStream dis) {
        try {
            // Read file name and size from the client
            String fileName = dis.readUTF();

            // 检查并创建文件夹
            File directory = new File(STORAGE_FOLDER + getDirectoryName(fileName));
            if (!directory.exists()) {
                directory.mkdirs(); // 递归创建文件夹
            }

            long fileSize = dis.readLong();
            ProgressInfo progressInfo = new ProgressInfo("upload", 0, fileSize);
            progressMap.put(fileName, progressInfo);

            // Set up output stream to write the file
            FileOutputStream fileOutputStream = new FileOutputStream(STORAGE_FOLDER + fileName);
            byte[] buffer = new byte[4096];
            int bytesRead = 0;
            boolean canceled = false;


            // Receive file content from the client
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);

                if (progressInfo.isCanceled()) {// 检查是否取消
                    canceled = true;
                    break;
                }
                updateProgress(fileName, bytesRead);

            }
            fileOutputStream.close();

            if (fileSize != progressInfo.getCurrentBytes()) {
                canceled = true;
            }

            if (!canceled) {
                System.out.println("====================================");
                System.out.println("File received: " + fileName);
                System.out.println("====================================");
            } else {
                File file = new File(STORAGE_FOLDER + fileName);
                file.delete();
                System.out.println(fileName + " canceled by the client.");
            }

            progressMap.remove(fileName);
            dis.close();
            clientSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void updateProgress(String fileName, int bytesRead) {
        ProgressInfo progressInfo = progressMap.get(fileName);
        if (progressInfo != null) {
            progressInfo.setCurrentBytes(progressInfo.getCurrentBytes() + bytesRead);
        }
    }
}
