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

    private static final Map<String, Socket> socketMap = new ConcurrentHashMap<>(); // filename, socket


    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server is listening on port " + PORT);

            ExecutorService clientExecutor = Executors.newFixedThreadPool(5);

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
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String signal = dis.readUTF();
//            String signal = br.readLine();
            if (signal.equals("main")) {
                socketMap.put("main", clientSocket);
//                handleMainClient(clientSocket, inputStream, dis); // operations: upload, download ...
            } else if (signal.equals("file")) {
                handleFileTransferClient(clientSocket, inputStream, dis); // filename, filesize.
            }

        } catch (IOException e) {
            System.out.println("Client disconnected.");
            System.out.println(e.getMessage());
        }
    }

    private static void handleMainClient(Socket clientSocket, InputStream in, DataInputStream dis) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String op;
            label:
            while ((op = br.readLine()) != null) {
                String fileName;
                switch (op) {
                    case "pause":
                        //                    fileName = in.readLine();
                        //                    Socket socket = socketMap.get(fileName);

                        break;
                    case "resume":
                        //                    fileName = in.readLine();
                        //                    progressInfo = progressMap.get(fileName);
                        //                    if (progressInfo != null) {
                        //                        progressInfo.setPaused(false);
                        //                    }
                        break;
                    case "cancel":
                        fileName = br.readLine();
                        System.out.println(fileName + " canceled by the client.");
                        progressMap.remove(fileName);
                        try {
                            // delete the file
                            File file = new File(STORAGE_FOLDER + fileName);
                            file.delete();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
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
        }

    }

    private static void handleFileTransferClient(Socket clientSocket, InputStream inputStream, DataInputStream dis) {
        try {
            // Read file name and size from the client
            String fileName = dis.readUTF();
            socketMap.put(fileName, clientSocket);
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
            System.out.println("====================================");
            System.out.println("File received: " + fileName);
            System.out.println("====================================");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static boolean checkCancel(String fileName) {
        ProgressInfo progressInfo = progressMap.get(fileName);
        if (progressInfo != null) {
            return progressInfo.isCanceled();
        }
        return true;
    }


    static void updateProgress(String fileName, int bytesRead) {
        ProgressInfo progressInfo = progressMap.get(fileName);
        if (progressInfo != null) {
            progressInfo.setCurrentBytes(progressInfo.getCurrentBytes() + bytesRead);
        }
    }


}
