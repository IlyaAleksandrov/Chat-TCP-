package Aleksandrov.Chat;

import java.io.IOException;
import java.net.Socket;

/**
 The main class of the client.
 The client, at the beginning of its work, asks the user for the address and port of the server,
 connects to the specified address, gets a name request from the server, asks the user for the name,
 sends the user name to the server, wait for the server to accept the name.
 After that, the client can exchange text messages with the server.
 Messaging will occur in two threads running in parallel.
 */

public class Client {
    private Connection connection;
    /**
     * Field-flag, it will be set to true if the client is connected to the server or false otherwise.
     */
    private volatile boolean clientConnected;

    /**
     * requests input of the server address from the user and return the entered value.
     * The address can be a string containing ip, if the client and the server are running on different machines
     * or 'localhost', if the client and the server are running on the same machine.
     * @return
     */
    private String getServerAddress() {
        ConsoleHelper.writeMessage("insert server address:");
        return ConsoleHelper.readString();
    }

    /**
     * requests input of the server port and return it.
     * @return
     */
    private int getServerPort() {
        ConsoleHelper.writeMessage("insert server port:");
        return ConsoleHelper.readInt();
    }

    /**
     * requests and return a user name
     * @return
     */
    private String getUserName() {
        ConsoleHelper.writeMessage("insert your name:");
        return ConsoleHelper.readString();
    }

    /**
     * creates a new text message using the transmitted text and sends it to the server via the connection.
     * @param text
     */
    private void sendTextMessage(String text) {
        try{
            Message message = new Message(MessageType.TEXT, text);
            connection.send(message);
        } catch (IOException e) {
            ConsoleHelper.writeMessage("sending error");
            clientConnected = false;
        }
    }

    /**
     * Method creates a helper stream SocketThread, waits until it establishes a connection with the server,
     * and then in a cycle reads messages from the console and send them to the server.
     * The condition for logging out of the loop will be the disconnection of the client or the user entering
     * the 'exit' command.
     */
    public void action() {
        SocketThread socketThread = new SocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this) {
            try {
                wait();
                if(clientConnected) {
                    ConsoleHelper.writeMessage("Connection established. Insert 'exit' to quit");
                }
                else
                    ConsoleHelper.writeMessage("Error while client running...");
                String message;
                while (clientConnected) {
                    message = ConsoleHelper.readString();
                    if (message.equals("exit")) {
                        break;
                    }
                    sendTextMessage(message);
                }
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("error");
            }
        }

    }

    public static void main(String[] args) {
        Client client = new Client();
        client.action();
    }



    public class SocketThread extends Thread {
        /**
         * outputs message to the console.
         * @param message
         */
        private void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }

        /**
         * outputs to the console information that the participant with the name userName joined the chat.
         * @param userName
         */
        protected void informAboutAddingNewUser(String userName){
            ConsoleHelper.writeMessage("New user " + userName + " joined the chat");
        }

        /**
         * outputs to the console information that the participant with the name userName leaved the chat.
         * @param userName
         */
        private void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage("User " + userName + " leaved the chat");
        }

        /**
         * Sets the value of the clientConnected field of the external Client object according to the passed parameter.
         * Notifies (awaken the waiting) the main thread of the Client class.
         * @param clientConnected
         */
        private void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }

        /**
         * Method in the loop, receives messages using the connection connection.
         * If the type of the received message NAME_REQUEST  (the server has requested a name),
         * requests the user name with the getUserName() method,
         * creates a new message with the type MessageType.USER_NAME
         * and the name entered, sends the message to the server.
         * If the type of message received NAME_ACCEPTED (the server took the name),
         * then the server accepted the client's name, it is necessary to inform the main thread about it,
         * it is waiting for it. Do this with the notifyConnectionStatusChanged () method, passing it to true.
         * Then get out of the method.
         * If received a message with some other type, throw the IOException ("Unexpected MessageType") exception.
         * @throws IOException
         * @throws ClassNotFoundException
         */
        private void clientHandshake() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (MessageType.NAME_REQUEST.equals(message.getType())) {
                    connection.send(new Message(MessageType.USER_NAME, getUserName()));
                } else if (MessageType.NAME_ACCEPTED.equals(message.getType())) {
                    notifyConnectionStatusChanged(true);
                    return;
                } else
                    throw new IOException("Unexpected Aleksandrov.Chat.MessageType");
            }
        }


        /**
         * This method implements the main processing cycle of server messages.
         * Receives a message from the server using the connection connection.
         * If this is a text message (type MessageType.TEXT), process it using the processIncomingMessage() method.
         * If this message is of the MessageType.USER_ADDED type, process it with the informAboutAddingNewUser() method.
         * If this message is of type MessageType.USER_REMOVED, process it with informAboutDeletingNewUser() method.
         * If the client receives a message of some other type, throw the IOException exception.
         * The loop will be terminated automatically if an error occurs (an exception is thrown) or
         * the thread in which the loop is running will be terminated.
         * @throws IOException
         * @throws ClassNotFoundException
         */
        private void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                String text = message.getData();
                if (MessageType.TEXT.equals(message.getType())) {
                    processIncomingMessage(text);
                }
                else if (MessageType.USER_ADDED.equals(message.getType())) {
                    informAboutAddingNewUser(text);
                }
                else if (MessageType.USER_REMOVED.equals(message.getType())) {
                    informAboutDeletingNewUser(text);
                }
                else
                    throw new IOException("Unexpected Aleksandrov.Chat.MessageType");
            }
        }

        /**
         * Requests the server address and port using the getServerAddress() and getServerPort() methods.
         * Creates a new object of class java.net.Socket, using the data obtained in the previous paragraph.
         * Creates a Connection object using the socket.
         * Calls the method that implements the "handshake" of the client with the server (clientHandshake()).
         * @see super.clientHandshake()
         * Calls the method that implements the main loop of server message processing.
         * If an exception occurs, tells the main thread about the problem using notifyConnectionStatusChanged
         * and false as a parameter.
         */
        public void run(){
            try {
                Socket socket = new Socket(getServerAddress(), getServerPort());
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();

            } catch (IOException e) {
                notifyConnectionStatusChanged(false);
            } catch (ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }
        }
    }
}
