import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

public class Client {
	private boolean loggedIn, registed, inChat, runProgramm = true;
	private int chatUDPPort, chatPartnerUDPPort, expectedSerialNumber = 1;
	private FileOutputStream fileOutputStream;
	private String user, requestfrom = "";
	private BufferedWriter serverWriter;
	private BufferedReader serverReader;
	private DatagramSocket udpSocket;
	private Socket socket;
	private serialnumber sn = serialnumber.getInstance();

	public Client() {
		try {
			socket = new Socket("localhost", 27999);
			serverWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			System.out.println("-welcome to Server!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void endSession() {
		try {
			if (user != null) {
				serverWriter.write("END:" + user + "\n");
				serverWriter.flush();
			} else {
				serverWriter.write("END:noUser\n");
				serverWriter.flush();
			}
			serverReader.close();
			serverWriter.close();
			socket.close();
			this.inChat = true;
			System.out.println("-Disconnected from the Server!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void askToChatWith() throws IOException, InterruptedException {
		onlineList();
		BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("-Who do you want to Chat with? <no> to Cancle, or type a Username:");
		String with = inputReader.readLine();
		if (!with.toUpperCase().trim().equals("NO")) {
			serverWriter.write("REQUEST:" + user + ":" + with + "\n");
			serverWriter.flush();
		}
	}

	public void onlineList() throws IOException, InterruptedException {
		serverWriter.write("WHO:" + user + "\n");
		serverWriter.flush();
		Thread.sleep(500);
	}

	public boolean login() throws IOException, InterruptedException {
		BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
		boolean checkUsername = true, checkPassword = true;
		String username = "", password = "";
		while (checkUsername) {
			System.out.println("-Enter your Username:");
			username = inputReader.readLine();
			if (!username.trim().equals(""))
				checkUsername = false;
		}
		while (checkPassword) {
			System.out.println("-Enter your Password:");
			password = inputReader.readLine();
			if (!password.trim().equals(""))
				checkPassword = false;
		}
		serverWriter.write("LOGIN:" + username + ":" + password + "\n");
		serverWriter.flush();
		Thread.sleep(500);
		if (loggedIn == true) {
			user = username;
			return true;
		}
		return false;
	}

	public boolean registerUser() throws IOException, InterruptedException {
		BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
		boolean checkUsername = true, checkPassword1 = true, checkPassword2 = true;
		String username = "", password1 = "", password2 = "";
		registed = false;
		while (checkUsername) {
			System.out.println("-Please type in the Username that you want to have:");
			username = inputReader.readLine();
			if (!username.trim().equals(""))
				checkUsername = false;
		}
		while (checkPassword1) {
			System.out.println("-Please type in your new Password:");
			password1 = inputReader.readLine();
			if (!password1.trim().equals(""))
				checkPassword1 = false;
		}
		while (checkPassword2) {
			System.out.println("-Please type in your new Password again:");
			password2 = inputReader.readLine();
			if (!password2.trim().equals(""))
				checkPassword2 = false;
		}
		if (!password1.equals(password2)) {
			System.out.println("-register Failed, You entered two diffrent passwords!");
			return false;
		}
		serverWriter.write("REGISTER:" + username + ":" + password1 + "\n");
		serverWriter.flush();
		Thread.sleep(500);
		if (registed == true) {
			return true;
		}
		return false;
	}

	public void commands(boolean loggedIn) {
		if (!loggedIn) {
			System.out.println("-Here are your options to type in: \n" +
					" register -> create a User\n" +
					" login    -> log in\n" +
					" end      -> quit");
		} else {
			System.out.println("-Here are your options\n" +
					" who  -> get the Onlinelist\n" +
					" chat -> ask someone to chat with you\n" +
					" end  -> quit");
		}
	}

	public boolean run(Client client) throws IOException, InterruptedException {
		final BufferedReader inputreader = new BufferedReader(new InputStreamReader(System.in));
		client.commands(loggedIn);
		while (runProgramm) {
			String input = inputreader.readLine();
			switch (input.toUpperCase()) {
				case "END":
					runProgramm = false;
					if (!inChat)
						client.endSession();
					break;
				case "REGISTER":
					if (!loggedIn && !inChat) {
						boolean lauf = true;
						while (lauf) {
							if (client.registerUser()) {
								client.commands(loggedIn);
								lauf = false;
							}
						}
					}
					break;
				case "LOGIN":
					if (!loggedIn && !inChat) {
						boolean lauf = true;
						while (lauf) {
							if (client.login()) {
								client.commands(loggedIn);
								lauf = false;
							}
						}
					}
					break;
				case "WHO":
					if (!inChat) {
						client.onlineList();
						client.commands(loggedIn);
					}
					break;
				case "CHAT":
					if (!inChat) {
						client.askToChatWith();
					}
					break;
				case "Y":
					if (!inChat) {
						if (!requestfrom.equals("")) {
							serverWriter.write("CONNECT:" + user + ":" + requestfrom + ":YES\n");
							serverWriter.flush();
							requestfrom = "";
						} else {
							System.out.println("-No Request!");
							client.commands(loggedIn);
						}
					}
					break;
				case "N":
					if (!inChat) {
						if (!requestfrom.equals("")) {
							serverWriter.write("CONNECT:" + user + ":" + requestfrom + ":NO\n");
							serverWriter.flush();
							requestfrom = "";
							client.commands(loggedIn);
						} else {
							System.out.println("-No Request!");
							client.commands(loggedIn);
						}
					}
					break;
				case "SEND FILE":
					if (inChat) {
						client.sendFile(inputreader, chatPartnerUDPPort);
					}
					break;
				default:
					if (!inChat) {
						System.out.println("-Unknown Command");
						client.commands(loggedIn);
					} else {
						if (!input.equals("")) {
							client.sendText(chatPartnerUDPPort, input);
						} else {
							System.out.println("No empty messages please, try it again!");
						}
					}
			}
		}
		return runProgramm;
	}

	private void sendFile(BufferedReader inputreader, int port) throws IOException {
		System.out.println("-Enter the file path:");
		String path = inputreader.readLine();
		File file = new File(path);
		if (!file.exists() || file.isDirectory()) {
			System.out.println("-File not found!");
			return;
		}
		FileInputStream fileInputStream = new FileInputStream(file);
		byte[] sendData = new byte[1024];
		// start to send File
		System.out.println("-Sending the File ...");
		String line = "File:newFile:new" + path;
		sendData = line.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"),
				port);
		udpSocket.send(sendPacket);
		// send the file itself
		int bytesRead;
		while ((bytesRead = fileInputStream.read(sendData)) != -1) {
			byte[] header = String.valueOf(sn.getValue()).getBytes();
			byte[] packetData = new byte[header.length + bytesRead];
			System.arraycopy(header, 0, packetData, 0, header.length);
			System.arraycopy(sendData, 0, packetData, header.length, bytesRead);
			sendPacket = new DatagramPacket(packetData, packetData.length, InetAddress.getByName("localhost"), port);
			udpSocket.send(sendPacket);
		}
		// tell client filesending has finished
		System.out.println("-Done.");
		line = "File:EndFile:new" + path;
		sendData = line.getBytes();
		sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("localhost"), port);
		udpSocket.send(sendPacket);
		fileInputStream.close();
	}

	public void sendText(int port, String msg) throws IOException {
		byte[] sendData;
		String sentence = "Text:" + msg;
		sendData = sentence.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData,
				sendData.length, InetAddress.getByName("localhost"), port);
		udpSocket.send(sendPacket);
	}

	public void handleRecieve(DatagramPacket receivePacket, Client client) throws IOException {
		String response = new String(receivePacket.getData()).trim();
		switch (response.split(":")[0]) {
			case "Text":
				String textbody = response.split(":")[1];
				if (textbody.equals("DiscoNNecTed!")) {
					client.sendText(chatPartnerUDPPort, "DiscoNNecT!");
					Thread.currentThread().interrupt();
					System.out.println("-Client is Disconnected!");
					System.exit(0);
				} else if (textbody.equals("DiscoNNect!")) {
					System.out.println("-Client is Disconnected!");
				} else {
					System.out.println("Received: " + textbody);
				}
				break;
			case "File":
				switch (response.split(":")[1]) {
					case "newFile":
						String savePath = response.split(":")[2];
						client.fileOutputStream = new FileOutputStream(savePath);
						break;
					case "EndFile":
						client.fileOutputStream.close();
						System.out.println("-File is recieved (" + response.split(":")[2] + ")!");
				}
				break;
			default:
				byte[] header = Arrays.copyOfRange(receivePacket.getData(), 0,
						String.valueOf(expectedSerialNumber).length());
				int sn = Integer.parseInt(new String(header).trim());
				if (sn == expectedSerialNumber) {
					client.fileOutputStream.write(receivePacket.getData(), header.length,
							receivePacket.getLength() - header.length);
					expectedSerialNumber++;
				} else {
					System.out.println("-Packet lose by Recieving!");
					expectedSerialNumber = sn + 1;
				}
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		Client client = new Client();
		Thread readerThread = new Thread(() -> {
			String response;
			try {
				while (!Thread.currentThread().isInterrupted()) {
					if (!client.inChat) {
						if ((response = client.serverReader.readLine()) != null) {
							switch (response.split(":")[0]) {
								case ("Login successful"):
									System.out.println("-Login was successful successfully !");
									client.loggedIn = true;
									break;
								case "Succsessful registred":
									System.out.println("-register was successful successfully !");
									client.registed = true;
									break;
								case "REQUEST":
									System.out.println("-This User wants to chat with you (" + response.split(":")[1]
											+ "), Do you want to accept the chat request?(Y/N):");
									client.requestfrom = response.split(":")[1];
									break;
								case "DENIED":
									System.out.println("-Your Chat Request was Denied\n");
									client.commands(client.loggedIn);
									break;
								case "ACCEPTED":
									System.out.println("-Your Chat Request to was Accepted\n");
									client.chatUDPPort = Integer.parseInt(response.split(":")[1]);
									client.chatPartnerUDPPort = Integer.parseInt(response.split(":")[2]);
									client.udpSocket = new DatagramSocket(client.chatUDPPort);
									client.endSession();
									break;
								case "ADRESS":
									client.chatUDPPort = Integer.parseInt(response.split(":")[2]);
									client.chatPartnerUDPPort = Integer.parseInt(response.split(":")[1]);
									client.udpSocket = new DatagramSocket(client.chatUDPPort);
									client.endSession();
									break;
								default:
									System.out.println(response);
									break;
							}
						}
					} else {
						byte[] receiveData = new byte[1024];
						DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
						client.udpSocket.receive(receivePacket);
						client.handleRecieve(receivePacket, client);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		readerThread.start();
		if (!client.run(client)) {
			readerThread.interrupt();
			if (client.chatPartnerUDPPort != 0) {
				client.sendText(client.chatPartnerUDPPort, "DiscoNNecTed!");
			}
		}
	}
}