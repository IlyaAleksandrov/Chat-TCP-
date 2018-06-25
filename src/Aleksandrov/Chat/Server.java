package Aleksandrov.Chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The main class of the server.
 */
public class Server {

    /**
     * map which stores all active connections
     */
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    /**
     * method sends message to all connections from connectionMap.
     * @param message
     */
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

    /**
     * Main method queries the server port using ConsoleHelper.
     * Creates a server socket java.net.ServerSocket, using the port from the previous point.
     * Displays a message that the server is running.
     * In an infinite loop, listens and accepts incoming socket connections of the newly created server socket.
     * Creates and runs a new Handler thread, passing a socket from the previous item to the constructor.
     * After creating the Handler flow, goes to the next step in the loop and listens for new connections.
     * Provides for closing the server socket in case of an exception.
     * @param args
     */
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

    /**
     * Class which will exchange messages with the client, while main thread is waiting for new connection
     */
    private static class Handler extends Thread {

        /**
         * socket for current connection
         */
        private Socket socket;

        /**
         * constructor initializes socket
         * @param socket
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * The method accepts the connection as a parameter, and returns the name of the new client.
         * Creates and sends a user name request command
         * Gets a customer response
         * Verifies that a command with a user name is received
         * Removes the name from the response, verifies that it is not empty
         * and the user with that name is not already connected
         * Adds a new user and connect to it in connectionMap
         * Sends a command to the client informing that his name has been accepted
         * If some test fails, re-requests the client's name
         * Returns the accepted name as the return value
         * @param connection
         * @return the name of the new client
         * @throws IOException
         * @throws ClassNotFoundException
         */
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

        /**
         * Method goes through the connectionMap and for each element gets the client's name.
         * Generates a command using the USER_ADDED message type and the received name.
         * Sends the generated command via connections.
         * @param connection
         * @param userName
         * @throws IOException
         */
        private void sendListOfUsers(Connection connection, String userName) throws IOException{
            for (String name: connectionMap.keySet()) {
                if (!name.equals(userName))
                    connection.send(new Message(MessageType.USER_ADDED, name));
            }
        }

        /**
         * this method is the main loop of processing of messages by the server.
         * Receives a client message
         * If the received message is text (type TEXT), then forms a new text message by concatenating:
         * client name, colon, space and message text.
         * Sends the generated message to all clients using the sendBroadcastMessage method.
         * If the received message is not text, displays an error message
         * @see super.sendBroadcastMessage()
         * @param connection
         * @param userName
         * @throws IOException
         * @throws ClassNotFoundException
         */
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

        /**
         * Main method of class Handler.
         * Displays a message that a new connection with a remote address is established,
         * obtained by using the getRemoteSocketAddress method.
         * Creates a Connection using the socket field.
         * Calls the method that implements the handshake with the client, keeping the name of the new client.
         * Sends to all chat participants information about the name of the joined party
         * (message with the type USER_ADDED).
         * Informs the new participant about existing participants.
         * Starts the main message processing cycle by the server.
         * Ensures that the connection is closed when an exception occurs.
         * Handles all exceptions of type IOException and ClassNotFoundException,
         * outputs to the console information that an error occurred while exchanging data with a remote address.
         * Outputs a message informing you that the connection to the remote address is closed.

         */
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
