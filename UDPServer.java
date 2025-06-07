import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Base64;

public class UDPServer {
    public static void main(String[] args) {
        DatagramSocket serverSocket = null;
        try {
            serverSocket = new DatagramSocket(51234);
            System.out.println("UDP Server is running and waiting for clients...");
            byte[] receiveData = new byte[1024];
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                System.out.println("Main thread received a new client request from " + receivePacket.getAddress().getHostAddress());
                DatagramPacket newClientRequest = new DatagramPacket(
                    receivePacket.getData(),
                    receivePacket.getLength(),
                    receivePacket.getAddress(),
                    receivePacket.getPort()
                );
                ClientHandler clientHandler = new ClientHandler(newClientRequest);
                Thread handlerThread = new Thread(clientHandler);
                handlerThread.start();
                handlerThread.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        }
    }

static class ClientHandler implements Runnable {

    private DatagramPacket requestPacket;
    public ClientHandler(DatagramPacket requestPacket) {
        this.requestPacket = requestPacket;
    }

    @Override
    public void run() {
        try (DatagramSocket dataSocket = new DatagramSocket()) {
            
            InetAddress clientAddress = requestPacket.getAddress();
            int clientPort = requestPacket.getPort();

            String requestString = new String(requestPacket.getData(), 0, requestPacket.getLength());
            String[] parts = requestString.split(" ");

            if (parts.length != 2 || !parts[0].equalsIgnoreCase("DOWNLOAD")) {
                System.err.println("Invalid request from client: " + requestString);
                String errorMsg = "ERR INVALID_REQUEST";
                byte[] errorData = errorMsg.getBytes();
                DatagramPacket errorPacket = new DatagramPacket(errorData, errorData.length, clientAddress, clientPort);
                dataSocket.send(errorPacket); 
                return;
            }

            String filename = parts[1];
            File file = new File(filename);
            if (!file.exists()) {
                String errorMsg = "ERR " + filename + " NOT_FOUND";
                byte[] errorData = errorMsg.getBytes();
                DatagramPacket errorPacket = new DatagramPacket(errorData, errorData.length, clientAddress, clientPort);
                dataSocket.send(errorPacket); 
                return;
            }

            long fileSize = file.length();
            int newPort = dataSocket.getLocalPort();
            String okResponse = "OK " + filename + " SIZE " + fileSize + " PORT " + newPort;
            byte[] okData = okResponse.getBytes();
            DatagramPacket okPacket = new DatagramPacket(okData, okData.length, clientAddress, clientPort);
            dataSocket.send(okPacket); // Use the new socket to send the OK message

            System.out.println("Worker thread for " + filename + " on port " + newPort + " is ready.");


            byte[] buffer = new byte[2048]; // Buffer for receiving requests like "FILE GET..."
            while (true) { // This loop will handle all chunks for this file transfer
                DatagramPacket fileRequestPacket = new DatagramPacket(buffer, buffer.length);
                
                // Set a timeout in case the client stops sending requests
                dataSocket.setSoTimeout(30000); // 30 seconds timeout
                
                dataSocket.receive(fileRequestPacket); // Wait for "FILE GET..." on the new port
                
                String fileRequest = new String(fileRequestPacket.getData(), 0, fileRequestPacket.getLength());
                System.out.println("Received on port " + newPort + ": " + fileRequest);

                if (fileRequest.contains("GET")) {
                    // For this example, we just send the first 1000 bytes as one chunk.
                    // In your assignment, you'd read the specific byte range requested.
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] fileChunk = new byte[1000];
                        int bytesRead = fis.read(fileChunk, 0, 1000);

                        if (bytesRead != -1) {
                            byte[] actualChunk = new byte[bytesRead];
                            System.arraycopy(fileChunk, 0, actualChunk, 0, bytesRead);
                            
                            // Encode data with Base64
                            String encodedData = Base64.getEncoder().encodeToString(actualChunk);
                            String dataResponse = "FILE " + filename + " OK START 0 END " + (bytesRead - 1) + " DATA " + encodedData;
                            byte[] sendData = dataResponse.getBytes();

                            DatagramPacket dataPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                            dataSocket.send(dataPacket);
                        }
                    }
                }
                
                // In the real assignment, you would check for a "CLOSE" message to break the loop.
                if (fileRequest.contains("CLOSE")) {
                    System.out.println("Client finished. Closing connection on port " + newPort);
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("Handler thread error: " + e.getMessage());
            //e.printStackTrace();
        }



    }}}

