# HOW TO USE THIS PROGRAM
1. compile UDPServer.java and run
```sh
javac UDPServer.java
java UDPServer 51234 #This is your port, and you can customer them
```
2. compile UDPClient.java and run
```sh
javac UDPClient.java
java UDPClient localhost 51234 files.txt 
#If it isn't in a same machine, change the localhost to the IP.
#port must same to the server port
#files.txt is the demo file, and you can customer them
```