# Questions

This document provides additional assistance for A4. It lays out the solution in baby steps and shows the functions/methods to be used in key tasks.

I suggest that you attempt the work by yourself before using the help. Use Intellisense assistance, check both Java library/Python library documentation, StackOverflow, and other online resources. Then use this help if you are stranded or after you completed the work, to confirm you did not miss anything. 

Again, please note that client and server implementations are embarrassingly small and should NOT represent a big challenge in terms of programming. Combined, they are a mere 250 lines of code! 

Questions:
- how to organise the client?
- how to organise the multi-threaded server?
- how to create a function (at client side only) that will attempt to send and receive a message a number of times until success?
- how to use threads in the server?
- how to process a string message via tokenisation?
- how to convert binary data to encoded strings and vice-versa?
- how to set a timeout to wait for a response?

## how to organise the client? (independent of language)
- validate and process the command line arguments
- (try to) open the file containing the list of files to download
- create datagram socket
- prepare DOWNLOAD message to request the first file on the list
- send request and wait for response
- before blocking to receive a response, set a timeout with the initial value
- if a response does not arrive, there is a timeout, then print a message, double the timeout value, and attempt to receive again
- when a response is received, check the value of the response
- if an error (e.g. file does not exist), then print a message and go to the next file
- if the message was ok, then you got the size of the file
- create the file for writing on it
- enter a cycle (a function) through which the file will be downloaded until it is completed
- iterate progressively
  - sending a request to download a block of data (provide the byte range) from the server
  - wait for the response using the same approach for reliability as in the DOWNLOAD message (i.e. set timeout, etc.)
  - at each data message received
    - decode the Base64 data into binary
    - write the data on the file using random writing (seek to the position in the file, then write), and 
    - print a "*" to show the progress
- when the file is completed, send a "closing request", and wait for a "closing response" from the server, using timeouts as before
- when the closing confirmation has been received, close the current file, print a message, and advance to the next file
- if there is no next file, terminate

## how to organise the multi-threaded server?
- validate and process the command line arguments (port number)
- create datagram socket to be the welcome socket and bind it to the server port from the command line
- wait for a client DOWNLOAD request
- when the request arrives, validate the message (must be a DOWNLOAD and have a filename as parameter) 
- if the message is incorrect, ignore it and go back waiting another
- if the message is correct (a DOWNLOAD), check if the filename exists
- if the file does not exist, send an error message back to the client
- if the file exists, send a message to the client it is okay and inform the file size
- create a new thread to handle the data requests that will come from this client
- in the new thread
  - select a random port number between 50000 and 51000
  - create a new datagram socket and bind it to the new port (so that this thread can receive the data requests via the socket without interfering with the download requests from other clients)
  - get the file size and prepare an OK response message with the file size and port information
  - send the response to the client (either via the new socket or the old one, it does not matter)
  - open the file for reading
  - enter a loop in which 
    - wait for a request from the client (do a receive on the new datagram socket) 
    - when the message is received, get the string contents of the request message 
    - split the string into parts or tokens (using " " as delimiter)
    - using the parts of the message, check if the request is a "closing message"
    - if so, close the file, send a "closing confirmation" to the client, and exit the loop (and terminate the thread)
    - otherwise, if the message is a "data request", then check the byte range requested (start and end) 
    - if the range is ok, then read the requested part of the file (seek to the position in the file, then read) 
    - if data is read ok, encode the data into a Base64 format (a string)
    - compose a response message (a string) with the encoded data
    - prepare a response packet with the response message string
    - send the packet to the client
  - close the socket
  
## how to create a function (at client side only)that will attempt to send and receive a message a number of times until success?

The arguments to this function include the socket to be used in sending and receiveing a packet, the IP address and port of the destiantion (the server), and the message/packet to be sent. The function returns the response message/packet received. The function uses a loop in which it configures an initial timeout, sends the packet, and waits for a response (blocks on receive).
If there is a timeout, which is an exception, then
- a counter will be increased
- the timeout value will be doubled, restart timer
- print an error message
- retransmit the message
- wait for a response

## how to use threads in the server?
Use them the same way as you have used in A2. However, this time you do not need to worry about synchronisation as the threads do not share any data.

In Java, I have the following:
    // create new thread to handle the DATA requests from the client
    new Thread(() -> handleFileTransmission(clientSocket, file, clientAddress)).start();
and 
    private static void handleFileTransmission(DatagramSocket clientSocket, File file, InetAddress clientAddress) {

In Python, I have the following:
    # create a new thread to handle the data requests that will come from this client
    threading.Thread(target=handleFileTransmission, args=(file, clientAddress)).start()
and
    def handleFileTransmission(fileName, clientAddress):


## how to process a string message via tokenisation?

In Java, 
    String clientRequest = new String(requestPacket.getData(), 0, requestPacket.getLength()).trim();
    // System.out.println("Received: <" + clientRequest + ">");
    String[] parts = clientRequest.split(" ");

In Python
    clientRequest = requestBuffer.decode().strip()
    # print(f"Received: {clientRequest}")
    # split the parts of the request
    parts = clientRequest.split(" ")

## how to convert binary data to encoded strings and vice-versa?

In Java, use
    import java.util.Base64;

I am using on the server
    base64_data = base64.b64encode(file_data).decode()
and on the client
    // extract the Base64-encoded data
    String base64Data = dataResponse.substring(dataPayloadIndex).trim();

In Python, use
    import base64

I am using on the server
    String base64Data = Base64.getEncoder().encodeToString(actualBytes);
and on the client
    # extract the Base64-encoded data
    fileData = base64.b64decode(base64Data)

## how to set a timeout to wait for a response?

In Java, I am using
    socket.setSoTimeout(currentTimeout);

In Python, I am using
    sock.settimeout(currentTimeout/1000)


Hope this helps! 