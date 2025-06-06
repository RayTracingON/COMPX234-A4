import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class UDPClient {
    int ID;
    public UDPClient(int name) {
        this.ID = name;
    }
    public void start() throws java.net.SocketException {
        try (DatagramSocket clientSocket = new DatagramSocket();Scanner scanner = new Scanner(System.in)){
            InetAddress serverAddress = InetAddress.getByName("localhost");
            int serverPort = 51234;
            System.out.print("Enter a message to send to the server: ");
            String messageToSend = scanner.nextLine();
            byte[] sendData = messageToSend.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
            clientSocket.send(sendPacket);
            System.out.println("Message sent to server.");
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            String serverResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Response from server: " + serverResponse);

        }catch (Exception e) {
            e.printStackTrace();
        }
}}
