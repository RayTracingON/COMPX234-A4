import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPServer {
    public void start() {
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

    private DatagramPacket initialRequestPacket;
    public ClientHandler(DatagramPacket requestPacket) {
        this.initialRequestPacket = requestPacket;
    }

    @Override
    public void run() {
        try (DatagramSocket dataSocket = new DatagramSocket()) {
            
            InetAddress clientAddress = initialRequestPacket.getAddress();
            int clientPort = initialRequestPacket.getPort();
            String clientMessage = new String(initialRequestPacket.getData(), 0, initialRequestPacket.getLength());

            System.out.println("----------------------------------------------------");
            System.out.println("Worker thread created for: " + clientAddress.getHostAddress() + ":" + clientPort);
            System.out.println("This thread is running on port: " + dataSocket.getLocalPort()); // 打印出这个线程使用的新端口
            System.out.println("Initial client message: \"" + clientMessage + "\"");

            // --- 在实际作业中，这里的逻辑会是 ---
            // 1. 准备 "OK <filename> SIZE <size> PORT <new_port>" 这样的响应
            // 2. 将包含新端口号(dataSocket.getLocalPort())的响应发送给客户端
            // 3. 进入循环，等待客户端在这个新端口上发送 FILE GET 请求
            // 4. 发送文件数据块...
            // 5. 直到文件传输结束
            
            // 为了演示，我们只发送一个简单的回复
            String responseMessage = "Hello from the worker thread! Your request is being processed.";
            byte[] sendData = responseMessage.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
            
            // 注意：为了让这个简化的例子能跑通，我们仍然通过主socket的端口回复。
            // 在你的作业里，客户端在收到OK消息后，会向dataSocket.getLocalPort()这个新端口发消息。
            // 这里我们用一个假的DatagramSocket来发送，模拟在主端口回复。
            DatagramSocket mainSocketSimulator = new DatagramSocket(); // 临时的socket来发送回复
            mainSocketSimulator.send(sendPacket);
            mainSocketSimulator.close();


            System.out.println("Response sent to client. Worker thread finishing.");
            System.out.println("----------------------------------------------------");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }}}

