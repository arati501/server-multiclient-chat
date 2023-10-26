import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client 
{
    public static final int SERVER_PORT = 9591;
    
    /*
	 * getMessage(): This message receives the message of the day from server
	 */
    static void getMessage(String serverInput){
            System.out.println(serverInput); 
    }
    
    /*
	 * storeMessage(): After the user is logged in, this code takes a message as
	 * input and sends it to the server as a response. The server will then store
	 * this message in the message directory.
	 */
    static void storeMessage(String serverInput, String userInput, DataInputStream dataIpStr,
			DataOutputStream dataOpStr, BufferedReader input) {
        try {
            System.out.println(serverInput);    
            serverInput = dataIpStr.readUTF();        
            System.out.println(serverInput);    
            if(!serverInput.equals("")) {
                userInput = input.readLine(); 
                dataOpStr.writeUTF(userInput);           
                serverInput = dataIpStr.readUTF();     
                System.out.println(serverInput); 
            } 
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /*
	 * quit(): This method exits the application and closes any open socket for
	 * client when confirmed by server.
	 */
	static boolean quit(DataInputStream dataIpStr, DataOutputStream dataOpStr, String serverInput, Socket clientSocket,
			boolean keepRunning) {
		try {
			System.out.println(serverInput);
			serverInput = dataIpStr.readUTF();
			System.out.println(serverInput);
			dataOpStr.close();
			dataIpStr.close();
			clientSocket.close();
			keepRunning = false;
			System.exit(SERVER_PORT); 
		} catch (IOException ex) {
			Logger.getLogger(Client1.class.getName()).log(Level.SEVERE, null, ex);
		}
		return keepRunning;
	}

	/*
	 * Shutdown: This method closes all sockets and exits the application at both
	 * client and server's end.
	 */
	static void shutdown(DataInputStream dataIpStr, DataOutputStream dataOpStr, String serverInput,
				Socket clientSocket) {
			try {
				System.out.println(serverInput);
				if (serverInput.equals("200 OK")) {
					dataOpStr.close();
					dataIpStr.close();
					clientSocket.close();
					System.exit(2); // using exit code 2 for exiting when SHUTDOWN
				}
			} catch (IOException ex) {
				Logger.getLogger(Client1.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
    
    public static void main(String[] args) 
    {
	Socket clientSocket = null;  
	DataOutputStream dataOpStr = null;
	DataInputStream dataIpStr = null;
	String userInput;
	String serverInput;
	BufferedReader input = null;
        boolean keep_going = true;
	
	if (args.length < 1)
	{
	    System.out.println("Usage: client <Server IP Address>");
	    System.exit(1);
	}

	try 
	{
	    clientSocket = new Socket("10.0.0.210", SERVER_PORT);
	    dataOpStr = new DataOutputStream(clientSocket.getOutputStream());
	    dataIpStr = new DataInputStream(clientSocket.getInputStream());
	    input = new BufferedReader(new InputStreamReader(System.in));
            new Thread(new ChatHandler(clientSocket)).start();
            
	} 
	catch (UnknownHostException e) 
	{
	    System.err.println("Don't know about host: hostname");
	} 
	catch (IOException e) 
	{
	    System.err.println("Couldn't get I/O for the connection to: hostname");
	}

	if (clientSocket != null && dataOpStr != null && dataIpStr != null) // add implementation for invalid commands
	{
	    try 
	    {
                    while (keep_going == true)
                    {
                        userInput = input.readLine();
                        dataOpStr.writeUTF(userInput);
                        serverInput = dataIpStr.readUTF();
                        if (userInput.startsWith("LOGIN", 0)) {
                            System.out.println(serverInput);
                        }
                        else if (userInput.startsWith("SEND", 0)) { 
                            System.out.println(serverInput);
                        }
                        else {
                            switch(userInput) { 
                                case "MSGGET":
                                	getMessage(serverInput);
                                    break;
                                case "MSGSTORE":
                                	storeMessage(serverInput, userInput, dataIpStr, dataOpStr, input);
                                    break;
                                case "QUIT" :
                                    keep_going = quit(dataIpStr, dataOpStr, serverInput, clientSocket, keep_going);
                                    break;
                                case "SHUTDOWN" :
                                	shutdown(dataIpStr, dataOpStr, serverInput, clientSocket);
                                    break;
                                case "LOGOUT" :
                                    System.out.println(serverInput);
                                    break;
                                case "WHO" :
                                    System.out.println(serverInput);
                                    break;
                                default :
                                	dataOpStr.flush();
                                    break;
                            }
                        }
                } 
	    } 
	    catch (IOException e) 
	    {
		System.err.println("IOException:  " + e);
	    }
	}
    }
        static class ChatHandler implements Runnable { 
            final private Socket clientSocket;
            ChatHandler(Socket socket) {
                clientSocket = socket;
            }
            @Override
            public void run() {
            DataInputStream dataIpStr;
            BufferedReader inputStr;
            try {
            	inputStr = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                dataIpStr = new DataInputStream(clientSocket.getInputStream());
                int result = dataIpStr.readInt();
                if(result == 1){
                    System.out.println("The server is full!\n");
                }
                else {
                    System.out.println("Connected. \n");
                    while(true) {
                        if(!inputStr.ready()) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                        else{
                            String message = dataIpStr.readUTF();
                            System.out.println(message + "\n");
                            if ((message.equals("Thank you for using the server.")) || (message.equals("Server shutting down...")))
                                System.exit(1);
                        }
                        
                    }
                }
            } 
            catch (IOException ex) { 
                System.out.println("Lost connection to the server.");

            }
        }


        }
}
