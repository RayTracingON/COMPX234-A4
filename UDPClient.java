import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Base64;
import java.util.Scanner;

public class UDPClient {

    static int state=1;
    public static void main(String[] args) {

        try (DatagramSocket clientSocket = new DatagramSocket()){
            Scanner scanner = new Scanner(System.in);
            InetAddress serverAddress = InetAddress.getByName("localhost");
            int serverPort = 51234;

            while(true){
                System.out.print("Enter a message to send to the server: ,Enter 0 to exit: ");
                String messageToSend = scanner.nextLine();
                if (messageToSend.equals("0")) {
                    state = 0;
                    System.out.println("Exiting client.");
                    scanner.close();
                    break;
                }
            
            byte[] sendData = messageToSend.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
            clientSocket.send(sendPacket);
            System.out.println("Message sent to server.");
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            String serverResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Response from server: " + serverResponse);
            if (serverResponse.startsWith("ERR")) {
                System.err.println("Server returned an error: " + serverResponse);
                continue; 
            }

            if (!serverResponse.startsWith("OK")) {
                System.err.println("Received an unexpected response from server: " + serverResponse);
                continue;
            }
            System.out.println("Received from server: '" + serverResponse + "'");
            String[] parts = serverResponse.split(" ");
            long fileSize = Long.parseLong(parts[3]);
            int dataPort = Integer.parseInt(parts[5]); 
            serverResponse.split(" ");
            String filename = parts[1];

            System.out.println("Starting file download from port " + dataPort + "...");
            try (FileOutputStream fos = new FileOutputStream("downloaded_" + filename)) {
                long bytesReceived = 0;
                int chunkSize = 1000; 

                while (bytesReceived < fileSize) {
                    long startByte = bytesReceived;
                    long endByte = Math.min(startByte + chunkSize - 1, fileSize - 1);

                    String fileGetRequest = "FILE " + filename + " GET START " + startByte + " END " + endByte;
                    byte[] fileGetData = fileGetRequest.getBytes();
                    DatagramPacket fileGetPacket = new DatagramPacket(fileGetData, fileGetData.length, serverAddress, dataPort);
                    clientSocket.send(fileGetPacket);

                    clientSocket.receive(receivePacket); 
                    String dataResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    
                    // "FILE <filename> OK START <start> END <end> DATA <base64_data>"
                    String[] dataParts = dataResponse.split(" ", 7);
                    if(dataParts.length == 7 && dataParts[0].equals("FILE") && dataParts[6] != null) {
                        byte[] decodedData = Base64.getDecoder().decode(dataParts[6]);
                        fos.write(decodedData);
                        bytesReceived += decodedData.length;
                        System.out.printf("Received chunk. Total downloaded: %d / %d bytes (%.2f%%)\n", bytesReceived, fileSize, (double)bytesReceived / fileSize * 100);
                    } else {
                        System.err.println("Received corrupted or invalid data packet.");
                    }


            }}}
        }
        catch (Exception e) {
            e.printStackTrace();
        }}}
