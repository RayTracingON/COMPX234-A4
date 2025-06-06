import java.util.ArrayList;
import java.util.List;


public class main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("--- Starting Multi-threaded UDP Demo ---");

        // 启动服务器线程
        UDPServer server = new UDPServer();
        Thread serverThread = new Thread(() -> server.start());
        serverThread.start();

        // 创建一个包含多个客户端的线程池
        List<Thread> threads = new ArrayList<>();
        UDPClient [] clients = new UDPClient[10];
        for (int i = 0; i < clients.length; i++) {
            final int index = i; 
            clients[i] = new UDPClient();
            Thread thread = new Thread(() -> {
                try {
                    clients[index].start();
                } catch (java.net.SocketException e) {
                    e.printStackTrace();
                }
            });
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.start();
        }
        serverThread.join();
        System.out.println("Server has finished.");
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("All clients have finished.");


        System.out.println("--- Demo Finished ---");
    }
}

