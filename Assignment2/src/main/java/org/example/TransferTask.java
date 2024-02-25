package org.example;

import java.io.*;
import java.net.Socket;

public class TransferTask implements Runnable {
    private static final String UPLOAD_FOLDER = "Client/Upload/";

    private static final String STORAGE_FOLDER = "Server/Storage/";

    private static final String DOWNLOAD_FOLDER = "Client/Download/";

    public String direction; // upload or download

    public Socket socket;

    public DataInputStream dis;

    public DataOutputStream dos;

    public InputStream inputStream;

    public OutputStream outputStream;

    public long currentBytes;

    public long totalBytes;

    public File file;

    public volatile boolean canceled;
    public volatile boolean isPaused = false;

    public TransferTask(Socket socket, File file, String direction) {
        this.socket = socket;
        this.file = file;
        this.direction = direction;
        this.currentBytes = 0;
        this.totalBytes = 0;
        this.canceled = false;
        try {
            this.inputStream = socket.getInputStream();
            this.outputStream = socket.getOutputStream();
            this.dis = new DataInputStream(inputStream);
            this.dos = new DataOutputStream(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TransferTask(Socket socket, String direction) {
        this.socket = socket;
        this.direction = direction;
        this.currentBytes = 0;
        this.totalBytes = 0;
        this.canceled = false;
        try {
            this.inputStream = socket.getInputStream();
            this.outputStream = socket.getOutputStream();
            this.dis = new DataInputStream(inputStream);
            this.dos = new DataOutputStream(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        switch (direction) {
            case "upload":
                upload();
                break;
            case "download":
                download();
                break;
            case "handleUpload":
                handleUpload();
                break;
            default:
                System.out.println("Invalid direction.");
        }
    }

    private static String getDirectoryName(String fileName) {
        int lastSeparatorIndex = fileName.lastIndexOf('/');
        if (lastSeparatorIndex != -1) {
            return fileName.substring(0, lastSeparatorIndex);
        }
        return "";
    }

    private void download() {
        try {
            String fileName = file.getPath();
            // 检查并创建文件夹
            File directory = new File(DOWNLOAD_FOLDER + getDirectoryName(fileName));
            if (!directory.exists()) {
                directory.mkdirs(); // 递归创建文件夹
            }

            dos.writeUTF("download");
            dos.writeUTF(fileName);

            totalBytes = dis.readLong();
            FileOutputStream fileOutputStream = new FileOutputStream(DOWNLOAD_FOLDER + fileName);
            byte[] buffer = new byte[4096];
            int bytesRead;

            label:
            while ((bytesRead = inputStream.read(buffer)) != -1) {

                while (isPaused) {
                    if (canceled) break label;
                    try {
                        Thread.sleep(100); // Sleep for a short time and check again
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (canceled) break;
                fileOutputStream.write(buffer, 0, bytesRead);
                currentBytes += bytesRead;
            }

            fileOutputStream.close();

            if (canceled) {
                File file = new File(DOWNLOAD_FOLDER + fileName);
                file.delete(); // Delete the file if the download is canceled
                System.out.println("====================================");
                System.out.println("Download canceled: " + fileName);
                System.out.println("====================================");
            } else {
                System.out.println("====================================");
                System.out.println("File downloaded: " + fileName);
                System.out.println("====================================");
            }

            dis.close();
            socket.close();
            Client.TaskMap.remove(file.getName());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void upload() {
        try {
            dos.writeUTF("upload");
            String fileName = file.getPath().substring(UPLOAD_FOLDER.length());

            dos.writeUTF(fileName); //fileName with relative path
            dos.writeLong(file.length()); //size
            totalBytes = file.length();

            // Create input stream to read the file
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;

            // Send file content to the server
            label:
            while ((bytesRead = fis.read(buffer)) != -1) {
                while (isPaused) {
                    if (canceled) break label;
                    try {
                        Thread.sleep(100); // Sleep for a short time and check again
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (canceled) break;
                outputStream.write(buffer, 0, bytesRead);
                currentBytes += bytesRead;
            }

            // Signal the end of file
            fis.close();

            if (canceled) {
                System.out.println("====================================");
                System.out.println("Upload canceled: " + file.getName());
                System.out.println("====================================");

            } else {
                try {
                    Thread.sleep(100); // Sleep for a short time and check again
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("====================================");
                System.out.println("Upload success: " + file.getName());
                System.out.println("====================================");

            }
            dos.close();
            socket.close();
            Client.TaskMap.remove(file.getName());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleUpload() {
        try {
            // Read file name and size from the client
            String fileName = dis.readUTF();

            // 检查并创建文件夹
            File directory = new File(STORAGE_FOLDER + getDirectoryName(fileName));
            if (!directory.exists()) {
                directory.mkdirs(); // 递归创建文件夹
            }

            long fileSize = dis.readLong();

            // Set up output stream to write the file
            FileOutputStream fileOutputStream = new FileOutputStream(STORAGE_FOLDER + fileName);
            byte[] buffer = new byte[4096];
            int bytesRead = 0;

            // Receive file content from the client
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
                if (canceled) break; // 检查是否取消
            }

            fileOutputStream.close();
            if (!canceled) {
                System.out.println("====================================");
                System.out.println("File received: " + fileName);
                System.out.println("====================================");
            } else {
                File file = new File(STORAGE_FOLDER + fileName);
                file.delete();
                System.out.println(fileName + " canceled by the client.");
            }

            dis.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 设置暂停状态
    public void pause() {
        isPaused = true;
    }

    // 恢复任务执行
    public void resume() {
        isPaused = false;
    }

    // 取消任务执行
    public void cancel() {
        canceled = true;
    }


}
