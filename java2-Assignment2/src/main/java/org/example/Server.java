package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class Server {

    private static final int PORT = 12345;
    private static final String STORAGE_FOLDER = "Storage/";
    private static final String RESOURCES_FOLDER = "Resources/";
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
            String signal = dis.readUTF(); //!!!!!!!

            if (signal.equals("main")) {
                socketMap.put("main", clientSocket);
                handleMainClient(clientSocket, inputStream, dis); // operations: upload, download ...
            } else if (signal.equals("upload")) {
                handleUpload(clientSocket, inputStream, dis); // filename, filesize.
            } else if (signal.equals("download")) {
                handleDownload(clientSocket, inputStream, dis); // filename
            }

        } catch (IOException e) {
            System.out.println("Client disconnected.");
        }
    }

    private static void handleDownload(Socket clientSocket, InputStream inputStream, DataInputStream dis) {
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

                dos.writeLong(fileToSend.length()); // file size

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                fis.close();
                 dos.close(); //????

                outputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleMainClient(Socket clientSocket, InputStream in, DataInputStream dis) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
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
            getAllFilesRecursive(file, fileList);
        }

        return fileList;
    }

    private static void getAllFilesRecursive(File folder, List<String> fileList) {
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    getAllFilesRecursive(file, fileList);
                } else {
                    fileList.add(file.getName());
                }
            }
        }
    }

    private static void sendFileList(Socket clientSocket) {
        try {
            OutputStream outputStream = clientSocket.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream));
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
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream));
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


    private static void handleUpload(Socket clientSocket, InputStream inputStream, DataInputStream dis) {
        try {
            // Read file name and size from the client
            String fileName = dis.readUTF();
            socketMap.put(fileName, clientSocket);
            long fileSize = dis.readLong();
            ProgressInfo progressInfo = new ProgressInfo("upload", 20, fileSize);
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
