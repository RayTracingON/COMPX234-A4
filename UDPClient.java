import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Base64;
import java.util.Scanner;
import java.io.File;


public class UDPClient {

    static int state=1;
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java UDPClient <hostname> <port> <filelist.txt>");
            return;
        }
        String serverHostname = args[0];
        int serverPort = -1;
        try {
            serverPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Error: Port number must be an integer.");
            return;
        }
        String filelistName = args[2];
        try (DatagramSocket clientSocket = new DatagramSocket();Scanner fileScanner = new Scanner(new File(filelistName))){
            Scanner scanner = new Scanner(System.in);
            InetAddress serverAddress = InetAddress.getByName(serverHostname);
            System.out.println("Client started. Reading files from '" + filelistName + "'.");
            System.out.println("----------------------------------------------------");

            while(fileScanner.hasNextLine()){
            String filenameToDownload = fileScanner.nextLine().trim();
            if (filenameToDownload.isEmpty()) {
                continue; 
            }
            System.out.println("Attempting to download file: '" + filenameToDownload + "'...");
            System.out.println("----------------------------------------------------");

            String downloadRequest = "DOWNLOAD " + filenameToDownload;
            byte[] sendData = downloadRequest.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
            clientSocket.send(sendPacket);
            System.out.println("Message sent to server.");
            byte[] receiveData = new byte[2048];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try{
            clientSocket.setSoTimeout(5000); 
            clientSocket.receive(receivePacket);
            String serverResponse = new String(receivePacket.getData(), 0, receivePacket.getLength());
            if (serverResponse.startsWith("ERR")) {
                System.err.println("Server returned an error: " + serverResponse);
                continue; 
            }

            if (!serverResponse.startsWith("OK")) {
                System.err.println("Received an unexpected response from server: " + serverResponse);
                continue;
            }
            System.out.println("Received from server: '" + serverResponse + "'");
            String sizeKeyword = " SIZE ";
            String portKeyword = " PORT ";

            int sizeIndex = serverResponse.indexOf(sizeKeyword);
            int portIndex = serverResponse.indexOf(portKeyword);

            String filename = serverResponse.substring(3, sizeIndex).trim();
        
            String sizeStr = serverResponse.substring(sizeIndex + sizeKeyword.length(), portIndex).trim();
            long fileSize = Long.parseLong(sizeStr);

            String portStr = serverResponse.substring(portIndex + portKeyword.length()).trim();
            int dataPort = Integer.parseInt(portStr);
        
            System.out.println("Successfully parsed response. Filename: '" + filename + "', Size: " + fileSize + ", New Port: " + dataPort);            

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
                    String dataHeader = " DATA ";
                    int dataIndex = dataResponse.indexOf(dataHeader);

                    if (dataIndex != -1) {
                    String base64Data = dataResponse.substring(dataIndex + dataHeader.length());
    
                    byte[] decodedData = Base64.getDecoder().decode(base64Data);
                    fos.write(decodedData);
                    bytesReceived += decodedData.length;
                    System.out.printf("Received chunk. Total downloaded: %d / %d bytes (%.2f%%)\n", bytesReceived, fileSize, (double)bytesReceived / fileSize * 100);
                    } 
                    else {
                        System.err.println("Received corrupted or invalid data packet: " + dataResponse);
                    }
                }
                String closeRequest = "FILE " + filename + " CLOSE";
                byte[] closeData = closeRequest.getBytes();
                DatagramPacket closePacket = new DatagramPacket(closeData, closeData.length, serverAddress, dataPort);
                clientSocket.send(closePacket);
                System.out.println("Sent CLOSE message to server on port " + dataPort);
            }}
                catch (SocketTimeoutException e) {
                    System.err.println("Timeout: No response from server within 5 seconds.");
                    System.err.println("Client will now shut down.");
                    break; 
                }
            }}
        
        catch (Exception e) {
            e.printStackTrace();
        }}}
