package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;

import static org.example.Client.updateProgress;

public class Server {

    private static final int PORT = 12345;
    private static final String STORAGE_FOLDER = "Storage/";

    private static final Map<String, ProgressInfo> progressMap = new ConcurrentHashMap<>();


    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server is listening on port " + PORT);

            // 使用固定大小的线程池
            ExecutorService clientExecutor = Executors.newFixedThreadPool(5);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getInetAddress());

                // 使用线程池处理客户端连接
                clientExecutor.submit(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
            // 获取输入流
            InputStream inputStream = clientSocket.getInputStream();
            DataInputStream dis = new DataInputStream(inputStream);
            String signal = dis.readUTF(); // main client or file transfer socket.
            if (signal.equals("main")) {
                handleMainClient(clientSocket, inputStream, dis); // operations: upload, download ...
            } else if (signal.equals("file")) {
                handleFileTransferClient(clientSocket, inputStream, dis); // filename, filesize.
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleFileTransferClient(Socket clientSocket, InputStream inputStream, DataInputStream dis) {
        try {
            // Read file name and size from the client
            String fileName = dis.readUTF();
            long fileSize = dis.readLong();
            ProgressInfo progressInfo = new ProgressInfo(0, fileSize);
            progressMap.put(fileName, progressInfo);

            // Set up output stream to write the file
            FileOutputStream fileOutputStream = new FileOutputStream(STORAGE_FOLDER + fileName);
            byte[] buffer = new byte[4096];
            int bytesRead = 0;


            // Receive file content from the client
            while ((bytesRead = inputStream.read(buffer)) != -1) {

                fileOutputStream.write(buffer, 0, bytesRead);
            }

            fileOutputStream.close();
            dis.close();
            clientSocket.close();

            System.out.println("File received: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean cancellationRequested(String fileName) {
        ProgressInfo progressInfo = progressMap.get(fileName);

    }


    static void updateProgress(String fileName, int bytesRead) {
        ProgressInfo progressInfo = progressMap.get(fileName);
        if (progressInfo != null) {
            progressInfo.setCurrentBytes(progressInfo.getCurrentBytes() + bytesRead);
        }
    }


}
