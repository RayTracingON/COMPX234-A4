import java.io.File;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Base64;

public class UDPServer {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java UDPServer <port_number>");
            return;
        }
        int port = -1;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Error: Port number must be an integer.");
            return;
        }

        try (DatagramSocket welcomeSocket = new DatagramSocket(port)) {
            System.out.println("UDP Server is running and waiting for clients on port " + port);
            byte[] receiveData = new byte[1024];
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                welcomeSocket.receive(receivePacket);
                System.out.println("Main thread received a new client request from " + receivePacket.getAddress().getHostAddress());
                
                // 将 welcomeSocket 和请求包一起传递给 Handler
                ClientHandler clientHandler = new ClientHandler(welcomeSocket, receivePacket);
                Thread handlerThread = new Thread(clientHandler);
                handlerThread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private DatagramSocket welcomeSocket; // 用于发送初始OK响应
        private DatagramPacket requestPacket; // 初始请求包

        public ClientHandler(DatagramSocket welcomeSocket, DatagramPacket requestPacket) {
            this.welcomeSocket = welcomeSocket;
            // 创建请求包的副本，以防主线程的缓冲区被覆盖
            this.requestPacket = new DatagramPacket(
                requestPacket.getData(),
                requestPacket.getLength(),
                requestPacket.getAddress(),
                requestPacket.getPort()
            );
        }

        @Override
        public void run() {
            String filename = "";
            try {
                String requestString = new String(requestPacket.getData(), 0, requestPacket.getLength()).trim();
                String downloadKeyword = "DOWNLOAD ";

                if (requestString.toUpperCase().startsWith(downloadKeyword)) {
                    filename = requestString.substring(downloadKeyword.length());
                } else {
                    System.err.println("Invalid initial request: " + requestString);
                    String errorMsg = "ERR INVALID_REQUEST";
                    byte[] errorData = errorMsg.getBytes();
                    DatagramPacket errorPacket = new DatagramPacket(errorData, errorData.length, requestPacket.getAddress(), requestPacket.getPort());
                    this.welcomeSocket.send(errorPacket); // 使用 welcomeSocket 发送错误
                    return;
                }
                
                File file = new File(filename);
                if (!file.exists()) {
                    String errorMsg = "ERR " + filename + " NOT_FOUND";
                    byte[] errorData = errorMsg.getBytes();
                    DatagramPacket errorPacket = new DatagramPacket(errorData, errorData.length, requestPacket.getAddress(), requestPacket.getPort());
                    this.welcomeSocket.send(errorPacket); // 使用 welcomeSocket 发送错误
                    return;
                }

                // 创建一个新的数据Socket，用于后续的文件传输
                try (DatagramSocket dataSocket = new DatagramSocket()) {
                    InetAddress clientAddress = requestPacket.getAddress();
                    int clientPort = requestPacket.getPort();
                    long fileSize = file.length();
                    int newPort = dataSocket.getLocalPort();

                    // **核心修复：使用主欢迎Socket来发送OK响应**
                    String okResponse = "OK " + filename + " SIZE " + fileSize + " PORT " + newPort;
                    byte[] okData = okResponse.getBytes();
                    DatagramPacket okPacket = new DatagramPacket(okData, okData.length, clientAddress, clientPort);
                    
                    // 使用传递进来的 welcomeSocket 发送，保证客户端能收到
                    this.welcomeSocket.send(okPacket); 

                    System.out.println("Worker thread for '" + filename + "' on port " + newPort + " is ready.");

                    // 使用RandomAccessFile处理文件块
                    try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                        byte[] buffer = new byte[2048];
                        while (true) {
                            DatagramPacket fileRequestPacket = new DatagramPacket(buffer, buffer.length);
                            dataSocket.setSoTimeout(30000); // 30秒超时
                            
                            // 在新端口等待GET请求
                            dataSocket.receive(fileRequestPacket); 
                            
                            String fileRequest = new String(fileRequestPacket.getData(), 0, fileRequestPacket.getLength()).trim();
                            System.out.println("Received on port " + newPort + ": \"" + fileRequest + "\"");

                            if (fileRequest.toUpperCase().contains("CLOSE")) {
                                System.out.println("Client finished. Closing connection on port " + newPort);
                                break;
                            }
                            if (fileRequest.toUpperCase().contains("GET")) {
                                String[] requestParts = fileRequest.split(" ");
                                int startIndex = -1;
                                int endIndex = -1;
                                for (int i = 0; i < requestParts.length; i++) {
                                    if ("START".equalsIgnoreCase(requestParts[i])) {
                                        startIndex = i;
                                        } else if ("END".equalsIgnoreCase(requestParts[i])) {
                                        endIndex = i;
                                        }
                                    }   

                            if (startIndex != -1 && endIndex != -1 && startIndex + 1 < requestParts.length && endIndex + 1 < requestParts.length) {
                                long startByte = Long.parseLong(requestParts[startIndex + 1]);
                                long endByte = Long.parseLong(requestParts[endIndex + 1]);
                                int bytesToRead = (int)(endByte - startByte + 1);

                                byte[] fileChunk = new byte[bytesToRead];

                                raf.seek(startByte);

                                int bytesRead = raf.read(fileChunk, 0, bytesToRead);

                                if (bytesRead != -1) {
                                    byte[] actualChunk = fileChunk;
                                    if (bytesRead < bytesToRead) {
                                        actualChunk = java.util.Arrays.copyOf(fileChunk, bytesRead);
                                }

                                String encodedData = Base64.getEncoder().encodeToString(actualChunk);
                                String dataResponse = "FILE " + filename + " OK START " + startByte + " END " + (startByte + bytesRead - 1) + " DATA " + encodedData;
                                byte[] sendData = dataResponse.getBytes();
                                DatagramPacket dataPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                                dataSocket.send(dataPacket);
                                }
                                } else {
                                System.err.println("Malformed GET request: " + fileRequest);
                                }
                            } 
                            if (fileRequest.toUpperCase().contains("CLOSE")) {
                            System.out.println("Client finished. Closing connection on port " + newPort);
                            String closeResponse = "FILE " + filename + " CLOSE_OK";
                            byte[] closeData = closeResponse.getBytes();
                            DatagramPacket closeOkPacket = new DatagramPacket(closeData, closeData.length, clientAddress, clientPort);
                            dataSocket.send(closeOkPacket);
                            break; 
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Handler thread error for file '" + filename + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}