package Aleksandrov.Chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



public class Server {

    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void sendBroadcastMessage(Message message) {
        try{
            for (Connection connection: connectionMap.values())
                connection.send(message);
        }
        catch(IOException e){
            System.out.println("Sending fails");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Insert port for connection:");
        try (ServerSocket serverSocket = new ServerSocket(ConsoleHelper.readInt())) {
            ConsoleHelper.writeMessage("Connecting...");
            while (true) {
                Handler handler = new Handler(serverSocket.accept());
                handler.start();
            }
        } catch (Exception e) {
            ConsoleHelper.writeMessage("Error. Socket closed.");
            e.printStackTrace();
        }
    }

    private static class Handler extends Thread {

        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            String name;
            while (true){
                connection.send(new Message(MessageType.NAME_REQUEST, "Insert name:"));
                Message messageReceive = connection.receive();
                if(messageReceive.getType() != MessageType.USER_NAME || messageReceive.getData().isEmpty())
                    continue;
                name = messageReceive.getData();
                if(connectionMap.containsKey(name))
                    continue;
                connectionMap.put(name, connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED, "Name accepted"));
                break;
            }
            return name;
        }
        private void sendListOfUsers(Connection connection, String userName) throws IOException{
            for (String name: connectionMap.keySet()) {
                if (!name.equals(userName))
                    connection.send(new Message(MessageType.USER_ADDED, name));
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message messageReceive = connection.receive();
                if (messageReceive.getType() == MessageType.TEXT) {
                    Message newMessage = new Message(MessageType.TEXT, userName + ": " + messageReceive.getData());
                    sendBroadcastMessage(newMessage);
                }
                else
                    ConsoleHelper.writeMessage("Error. Wrong message type.");
            }
        }

        public void run() {
            ConsoleHelper.writeMessage("New connection established: " + socket.getRemoteSocketAddress());
            String name = null;
            try (Connection connection = new Connection(socket)){
                name = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, name));
                this.sendListOfUsers(connection, name);
                this.serverMainLoop(connection, name);
            } catch (IOException e) {
                ConsoleHelper.writeMessage("Error");
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Error");
                e.printStackTrace();
            }
            finally {
                if (name != null)
                    connectionMap.remove(name);
                    sendBroadcastMessage(new Message(MessageType.USER_REMOVED, name));
                    ConsoleHelper.writeMessage("Connection closed");
            }
        }
    }
}
