import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ThreadedServer {
    private ServerSocket serverSocket;
    Portnumber pn = Portnumber.getInstance();
    private HashMap<String, String> userdata;
    private HashMap<String, Socket> onlineListe = new HashMap<String, Socket>();
    private String filename = "..\\RN_BS_BLATT4\\src\\registeredPersons.txt";
    // Linux : "src/registeredPersons.txt"

    private ThreadedServer() {
        try {
            this.serverSocket = new ServerSocket(27999);
            createUserdata();
            System.out.println("Started the Server");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void run_forever() {
        while (true) {
            try {
                final Socket socket = this.serverSocket.accept();
                System.out.println("Client has connected: " + socket.getInetAddress());

                Thread clientThread = new Thread(() -> {
                    try {
                        handleClient(socket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                clientThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleClient(Socket socket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] data = line.split(":");
            String action = data[0].trim();
            if (action.equals("END")) {
                handleEndAction(line, reader, writer, socket);
                break;
            }
            switch (action) {
                case "REGISTER":
                    performRegisterUser(line, writer);
                    break;
                case "LOGIN":
                    performLogin(line, writer, socket);
                    break;
                case "WHO":
                    whoIsOnline(line, writer);
                    break;
                case "REQUEST":
                    performChatrequest(line, writer);
                    break;
                case "CONNECT":
                    respndToChatrequest(line, writer);
                    break;
                default:
                    handleInvalidAction(action, writer);
                    break;
            }
        }
    }

    private void handleEndAction(String line, BufferedReader reader, BufferedWriter writer, Socket socket)
            throws IOException {
        System.out.println(line);
        if (!line.split(":")[1].trim().equals("noUser")) {
            onlineListe.remove(line.split(":")[1].trim());
        }
        CloseSession(reader, writer, socket);
    }

    private void handleInvalidAction(String action, BufferedWriter writer) {
        try {
            String errorMessage = "Invalid action: " + action;
            writer.write(errorMessage);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void performRegisterUser(String line, final BufferedWriter writer) throws IOException {
        System.out.println(line);
        String[] data = line.split(":");
        String username = data[1];
        String password = data[2];

        if (userdata.containsKey(username)) {
            writer.write("-register Failed,Username already exists\n");
            writer.flush();
        } else {
            userdata.put(username, password);

            try {
                Writer output;
                output = new FileWriter(filename, true); 
                output.append("\n" + username + ":" + password);
                output.close();
            } catch (IOException ioex) {
                System.out.println("Failure with append");
            }
            writer.write("Succsessful registred\n");
            writer.flush();
        }
    }

    private void performLogin(String line, final BufferedWriter writer, Socket socket) throws IOException {
        System.out.println(line);
        String[] data = line.trim().split(":");
        String username = data[1];
        String password = data[2];
        if (onlineListe.containsKey(username)) {
            writer.write("-User is already logged in\n");
            writer.flush();
        } else if (!(userdata.containsKey(username) && userdata.get(username).equals(password))) {
            System.out.println("User not found or wrong password!");
            writer.write("-Login failed, Please try again\n");
            writer.flush();
        } else {
            onlineListe.put(username, socket);
            writer.write("Login successful" + "\n");
            writer.flush();
        }
    }

    private void performChatrequest(String line, final BufferedWriter writer) throws IOException {
        System.out.println(line);
        String[] data = line.split(":");
        String with = data[2].trim();
        String thisUser = data[1].trim();
        if(!with.equals(thisUser)){
             Socket chatPartnerSocket = onlineListe.get(with);
            if (chatPartnerSocket != null && !chatPartnerSocket.isClosed()) {
                BufferedWriter client2Writer = new BufferedWriter(
                        new OutputStreamWriter(chatPartnerSocket.getOutputStream()));
                client2Writer.write("REQUEST:" + thisUser + "\n");
                client2Writer.flush();
            } else {
                String errorMessage = "-Chat partner is not online or unavailable";
                writer.write(errorMessage);
                writer.newLine();
                writer.flush();
            }
        }
        else{
            String errorMessage = "You cant chat with yourself";
                writer.write(errorMessage);
                writer.newLine();
                writer.flush();
        }
       
    }

    private void respndToChatrequest(String line, final BufferedWriter writer) throws IOException {
        System.out.println(line);
        String[] data = line.split(":");
        String thisUser = data[1].trim();
        String to = data[2].trim();
        String answer = data[3].trim();
        Socket chatPartner2Socket = onlineListe.get(to);
        if (chatPartner2Socket != null && !chatPartner2Socket.isClosed()) {
            BufferedWriter client2Writer = new BufferedWriter(
                    new OutputStreamWriter(chatPartner2Socket.getOutputStream()));
            switch (answer) {
                case "YES":
                    int port1 = this.pn.getValue();
                    int port2 = this.pn.getValue();
                    client2Writer.write("ACCEPTED:" + port1 + ":" + port2 + "\n");
                    client2Writer.flush();
                    onlineListe.remove(thisUser);
                    writer.write("ADRESS:" + port1 + ":" + port2 + "\n");
                    writer.flush();
                    onlineListe.remove(to);
                    break;
                case "NO":
                    client2Writer.write("DENIED:" + "\n");
                    client2Writer.flush();
                    break;
            }
        } else {
            String errorMessage = "-Chat partner is not online or unavailable";
            writer.write(errorMessage);
            writer.newLine();
            writer.flush();
        }
    }

    private void whoIsOnline(String line, final BufferedWriter writer) {
        System.out.println(line);
        String curUser = line.split(":")[1].trim();
        String ausgabe = "-These Users are online:";
        if (onlineListe.size() == 1)
            ausgabe = "-No other User is Online";
        else {
            int count = 0;
            for (String s : onlineListe.keySet()) {
                if (!s.equals(curUser)) {
                    ausgabe += s;
                    count++;
                    if (count < onlineListe.size() - 1) {
                        ausgabe += ",";
                    }
                }
            }
        }
        try {
            writer.write(ausgabe);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createUserdata() {
        userdata = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    String username = parts[0];
                    String password = parts[1];
                    userdata.put(username, password);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void CloseSession(BufferedReader reader, BufferedWriter writer, Socket socket) throws IOException {
        reader.close();
        writer.close();
        socket.close();
        System.out.println("Client has disconnected: " + socket.getInetAddress());
        Thread.currentThread().interrupt();
    }

    public static void main(String[] args) {
        ThreadedServer ts = new ThreadedServer();
        ts.run_forever();
    }
}