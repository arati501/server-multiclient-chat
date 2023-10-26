import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Scanner;

public class Server {
	public static final int SERVER_PORT = 9591;
	static public int numClients = 0, messageCount;
	static Socket[] sockets;
	static ServerSocket myServerice = null;
	static String[] messageOfTheDay = new String[20], activeUsers = new String[5];
	static boolean serverContinue = true;

	/*
	 * To send message in a cyclic message of the day to the client . Message can be
	 * sent even if the user is not logged in
	 */
	static int sendMessageOfTheDay(DataOutputStream dataOutputStream, String[] message, int currentMessage,
			int messageCount, Socket clientSocket) {
		int userQuery = -1;
		try {
			for (int i = 0; i < numClients; i++) {
				if (sockets[i] == clientSocket) {
					userQuery = i;
					break;
				}
			}
			dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
			dataOutputStream.writeUTF("200 OK");
			dataOutputStream.writeUTF(messageOfTheDay[currentMessage]);
			currentMessage++;
			if (currentMessage == messageCount)
				currentMessage = 0;
		} catch (IOException ex) {
			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
		}
		return currentMessage;
	}

	/*
	 * Login : Logs in the user by checking to see if their credentials match any
	 * users in the file users.txt and will print an error if they are already
	 * logged in
	 */
	static String[] loginUser(DataOutputStream outputStream, String[] activeUsers, String line, Socket clientSocket) {
		try {
			Scanner inFS;
			int userQuery = -1;
			String temp, stored_user;
			String[] user_password, stored_user_password;
			for (int i = 0; i < numClients; i++) {
				if (sockets[i] == clientSocket) {
					userQuery = i;
					break;
				}
			}
			outputStream = new DataOutputStream(clientSocket.getOutputStream());
			if (!activeUsers[userQuery].equals("")) {
				outputStream.writeUTF("You are already logged in as " + activeUsers[userQuery]);
			} else {
				try {
					inFS = new Scanner(new File("C:\\Users\\khano\\eclipse-workspace\\CN\\P1\\src\\usersDir.txt")); 
					user_password = line.split(" ");
					if (user_password.length != 3) {
						try {
							outputStream.writeUTF("410 Wrong UserID or password");
						} catch (IOException ex) {
							Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
						}
						System.out.println("Wrong UserID or Password");
					} else {
						temp = user_password[1];
						user_password[1] = user_password[2];
						user_password[0] = temp;

						while (inFS.hasNextLine()) {
							stored_user = inFS.nextLine();
							stored_user_password = stored_user.split(" ");
							if ((user_password[0].equals(stored_user_password[0]))
									&& (user_password[1].equals(stored_user_password[1]))) {
								int i = 0;
								while (stored_user.charAt(i) != ' ') {
									activeUsers[userQuery] += stored_user.charAt(i);
									i++;
								}
							}
						}
						inFS.close();
						if (!activeUsers[userQuery].equals("")) {
							try {

								outputStream.writeUTF("200 OK");
							} catch (IOException ex) {
								Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
							}
							System.out.println("Logged in successfull, User: " + activeUsers[userQuery]);
						} else {
							try {
								outputStream.writeUTF("410 Wrong UserID or password");
							} catch (IOException ex) {
								Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
							}
							System.out.println("Login failed for the user");
						}
					}
				} catch (FileNotFoundException ex) {
					Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		} catch (IOException ex) {
			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
		}
		return activeUsers;
	}

	/* Who: returns a list of users that are currently logged into the server */
	static void who(DataOutputStream dataOutputStream, Socket clientSocket) {
		int userQuery = -1;
		try {
			for (int i = 0; i < numClients; i++) {
				if (sockets[i] == clientSocket) {
					userQuery = i;
					break;
				}
			}
			dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
			dataOutputStream.writeUTF("200 OK \n The list of the active users:");
			for (int i = 0; i < activeUsers.length; i++) {
				dataOutputStream.writeUTF(activeUsers[i] + " " + String.valueOf(sockets[i].getInetAddress()).replace("/", ""));
			}
		} catch (IOException ex) {
			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/*
	 * Send: sends a private message to the respective recipient who are logged in
	 */
	static void send(DataOutputStream dataOutputStream, DataInputStream dataInputStream, String message, String[] activeUsers,
			Socket clientSocket) {
		String[] recipient;
		boolean recipient_found = false;
		try {
			recipient = message.split(" ");
			if (recipient.length != 2) {
				dataOutputStream.writeUTF("syntax: SEND <target>");
			} else {
				for (int i = 0; i < activeUsers.length; i++) {
					if (activeUsers[i].equals(recipient[1])) {
						recipient_found = true;
					}
				}
				int userQuery = -1;
				for (int i = 0; i < numClients; i++) {
					if (sockets[i] == clientSocket) {
						userQuery = i;
						break;
					}
				}
				dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
				if (activeUsers[userQuery].equals("")) {
					dataOutputStream.writeUTF("401 You are not logged in, please log in first.");
					dataOutputStream.writeUTF("");
				} else if (recipient_found == false) {
					dataOutputStream.writeUTF("420 either the user does not exist or is not logged in.");
				} else {
					dataOutputStream.writeUTF("200 OK \n Please type message to target.");
					message = dataInputStream.readUTF();
					dataOutputStream.writeUTF("200 OK");
					for (int i = 0; i < numClients; i++) {
						if (activeUsers[i].equals(recipient[1])) {
							dataOutputStream = new DataOutputStream(sockets[i].getOutputStream());
							dataOutputStream.writeUTF("(PM) " + activeUsers[userQuery] + ": " + message);
							System.out.println(activeUsers[userQuery] + " -> " + activeUsers[i] + ": " + message);
						}
					}

				}
			}
		} catch (IOException ex) {
			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/*
	 * userLogout: Logs out the user if they are logged in and prints an error if they
	 * are not logged in
	 */
	static String[] userLogout(DataOutputStream dataOutputStream, String[] activeUsers, Socket clientSocket) {
		int userQuery = -1;
		for (int i = 0; i < numClients; i++) {
			if (sockets[i] == clientSocket) {
				userQuery = i;
				break;
			}
		}
		try {
			dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
			if (activeUsers[userQuery].equals(""))
				dataOutputStream.writeUTF("405 You are not logged in.");
			else {
				dataOutputStream.writeUTF("200 OK");
				System.out.println("user " + activeUsers[userQuery] + " logged out");
				activeUsers[userQuery] = "";
			}

		} catch (IOException ex) {
			System.out.println("User logged out due to Exception" + ex.getLocalizedMessage());
			activeUsers[userQuery] = "";
		}
		return activeUsers;
	}

	/*
	 * saveUserMessage: If the user is logged in, takes their input as a new motd.
	 * Prints an error if they are not logged in.
	 */
	static String saveUserMessage(DataInputStream dataInputStream, DataOutputStream dataOutputStream, String[] messageOfTheDay, int messageCount, String message,
			String[] activeUsers, Socket clientSocket) {
		try {
			int userQuery = -1;
			for (int i = 0; i < numClients; i++) {
				if (sockets[i] == clientSocket) {
					userQuery = i;
					break;
				}
			}
			dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
			if (activeUsers[userQuery].equals("")) {
				dataOutputStream.writeUTF("401 You are not logged in, please log in first.");
				dataOutputStream.writeUTF("");
			} else {
				dataOutputStream.writeUTF("200 OK");
				dataOutputStream.writeUTF("Please input new Message of the day to be added.");
				message = dataInputStream.readUTF();
				if (messageCount >= 20) {
					dataOutputStream.writeUTF("429 Limit exceed for storing message: maxLimit=20 messages");
				} else {
					dataOutputStream.writeUTF("200 OK");
					Writer wr = new FileWriter("messageDir.txt", true);
					BufferedWriter br = new BufferedWriter(wr);
					br.write(message);
					br.newLine();
					br.close();
				}
			}
		} catch (IOException ex) {
			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
		}
		return message;
	}

	/*
	 * Shutdown: If user calling this command is root user, shuts down the server by
	 * closing all sockets/streams and ending the application
	 */
	static boolean shutdown(DataOutputStream dataOutputStream, boolean keep_going, String[] activeUsers, Socket clientSocket) {
		try {
			int userQuery = -1;
			for (int i = 0; i < numClients; i++) {
				if (sockets[i] == clientSocket) {
					userQuery = i;
					break;
				}
			}
			dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
			if (activeUsers[userQuery].equals("root")) {
				dataOutputStream.writeUTF("200 OK");
				serverContinue = false;
				keep_going = false;
			} else {
				dataOutputStream.writeUTF("402 User not allowed to execute this command.");
				serverContinue = true;
				keep_going = true;
			}
		} catch (IOException ex) {
			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
		}
		return serverContinue;
	}

	/*
	 * Quit : Prints an acknowledgement message to the user before they close the
	 * socket
	 */
	static void quit(DataOutputStream dataOutputStream, Socket clientSocket) {
		int userQuery = -1;
		for (int i = 0; i < numClients; i++) {
			if (sockets[i] == clientSocket) {
				userQuery = i;
				break;
			}
		}
		try {
			dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
			dataOutputStream.writeUTF("200 OK");
			dataOutputStream.writeUTF("Thank you for using the server.");
		} catch (IOException ex) {
			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public static void main(String args[]) throws IOException {
		String line;
		sockets = new Socket[5];
		Scanner inFS;
		try {
			Arrays.fill(activeUsers, "");
			inFS = new Scanner(new File("C:\\Users\\khano\\eclipse-workspace\\CN\\P1\\src\\messageDir"));
			while ((inFS.hasNextLine())) {
				line = inFS.nextLine();
				messageOfTheDay[messageCount] = line;
				messageCount++;
			}
			inFS.close();
			for (int i = 0; i < messageCount; i++) {
				System.out.println(messageOfTheDay[i]);
			}
		} catch (FileNotFoundException ex) {
			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
		}

		try {
			Socket socket;
			ServerSocket server;
			server = new ServerSocket(SERVER_PORT);
			while (serverContinue) {
				socket = server.accept();
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				InetAddress inetAddress = socket.getInetAddress();
				if (numClients == 5) {
					System.out.println(
							Arrays.toString(inetAddress.getAddress()) + " attempted to connect to a full server.");
					out.writeInt(1);
				} else {
					out.writeInt(0);
					new Thread(new ChatHandler(socket)).start();
					numClients++;
				}
			}

		} catch (IOException e) {
			System.out.println(e);
		}
	}

	static class ChatHandler implements Runnable {
		final private Socket clientSocket;
		boolean keep_going = true;
		int active_motd;
		DataInputStream dataInputStream;
		DataOutputStream dataOutputStream;
		String line;
		InetAddress inetAddress;

		ChatHandler(Socket socket) {
			clientSocket = socket;
			sockets[numClients] = clientSocket;
			inetAddress = clientSocket.getInetAddress();
		}

		@Override
		public void run() {
			try {
				dataInputStream = new DataInputStream(clientSocket.getInputStream());
				dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
				for (int i = 0; i < numClients; i++) {
					if (sockets[i] != clientSocket) {
						dataOutputStream = new DataOutputStream(sockets[i].getOutputStream());
						dataOutputStream.writeUTF(inetAddress.getCanonicalHostName() + " has connected to the server.");
					}
				}
				System.out.println(inetAddress.getHostAddress() + " has connected to the server.");
				while ((line = dataInputStream.readUTF()) != null && keep_going == true) {
					if (line.startsWith("LOGIN", 0)) {
						activeUsers = loginUser(dataOutputStream, activeUsers, line, clientSocket);
					} else if (line.startsWith("SEND", 0)) {
						send(dataOutputStream, dataInputStream, line, activeUsers, clientSocket);
					} else {
						switch (line) {
						case "MSGGET":
							active_motd = sendMessageOfTheDay(dataOutputStream, messageOfTheDay, active_motd, messageCount, clientSocket);
							break;
						case "LOGOUT":
							activeUsers = userLogout(dataOutputStream, activeUsers, clientSocket);
							break;
						case "MSGSTORE":
							messageOfTheDay[messageCount] = saveUserMessage(dataInputStream, dataOutputStream, messageOfTheDay, messageCount, line, activeUsers,
									clientSocket);
							messageCount++;
							break;
						case "SHUTDOWN":
							keep_going = shutdown(dataOutputStream, keep_going, activeUsers, clientSocket);
							if (keep_going == false) {
								System.out.println("root user called SHUTDOWN.");
								for (int i = 0; i < numClients; i++) {
									dataOutputStream = new DataOutputStream(sockets[i].getOutputStream());
									dataOutputStream.writeUTF("Server shutting down...");
									dataOutputStream.flush();

								}
								System.exit(2);
							}
							break;
						case "QUIT":
							quit(dataOutputStream, clientSocket);
							break;
						case "WHO":
							who(dataOutputStream, clientSocket);
							break;
						default:
							int userQuery = -1;
							for (int i = 0; i < numClients; i++) {
								if (sockets[i] == clientSocket) {
									userQuery = i;
									break;
								}
							}
							if (!activeUsers[userQuery].equals("")) {
								for (int i = 0; i < numClients; i++) {
									if (sockets[i] != clientSocket) {
										System.out.println(i);
										dataOutputStream = new DataOutputStream(sockets[i].getOutputStream());
										dataOutputStream.writeUTF(activeUsers[userQuery] + ": " + line);
									} else {
										dataOutputStream = new DataOutputStream(sockets[i].getOutputStream());
										dataOutputStream.writeUTF("");
									}
								}
								System.out.println(activeUsers[userQuery] + ": " + line);
							} else {
								for (int i = 0; i < numClients; i++) {
									if (sockets[i] != clientSocket) {
										dataOutputStream = new DataOutputStream(sockets[i].getOutputStream());
										dataOutputStream.writeUTF("User" + userQuery + ": " + line);
									} else {
										dataOutputStream = new DataOutputStream(sockets[i].getOutputStream());
										dataOutputStream.writeUTF("You: " + line);
									}
								}
								System.out.println("User" + userQuery + ": " + line);
							}
						}
					}
				}
				clientSocket.close();
				numClients--;
			} catch (IOException ex) {
				System.out.println("User disconnected.");
				for (int i = 0; i < numClients; i++) {
					if (sockets[i] == clientSocket) {
						if (!activeUsers[i].equals("")) {
							activeUsers = userLogout(dataOutputStream, activeUsers, clientSocket);
						}
						numClients--;
						for (int j = i; j < numClients; j++) {
							sockets[j] = sockets[j + 1];
						}
						break;
					}
				}
			}

		}

	}
}
