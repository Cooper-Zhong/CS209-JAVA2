import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSocketExample {
    public static void main(String[] args) {
        final int portNumber = 8888;

        try {
            ServerSocket server = new ServerSocket(portNumber);
            Socket clientSocket = server.accept();
            try (InputStream input = clientSocket.getInputStream();
                 OutputStream output = clientSocket.getOutputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    String clientMsg = new String(buffer, 0, bytesRead);
                    String responseMessage = "Hello from server!";
                    output.write(responseMessage.getBytes());
                    System.out.println("Server: Sent response to client.");
                }
                input.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
