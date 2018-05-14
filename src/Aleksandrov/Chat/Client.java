package Aleksandrov.Chat;

import java.io.IOException;
import java.net.Socket;

public class Client {
    private Connection connection;
    private volatile boolean clientConnected;

    private String getServerAddress() {
        ConsoleHelper.writeMessage("insert server address:");
        return ConsoleHelper.readString();
    }

    private int getServerPort() {
        ConsoleHelper.writeMessage("insert server port:");
        return ConsoleHelper.readInt();
    }

    private String getUserName() {
        ConsoleHelper.writeMessage("insert your name:");
        return ConsoleHelper.readString();
    }


    private void sendTextMessage(String text) {
        try{
            Message message = new Message(MessageType.TEXT, text);
            connection.send(message);
        } catch (IOException e) {
            ConsoleHelper.writeMessage("sending error");
            clientConnected = false;
        }
    }
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
        private void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }
        protected void informAboutAddingNewUser(String userName){
            ConsoleHelper.writeMessage("New user " + userName + " joined the chat");
        }
        private void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage("User " + userName + " leaved the chat");
        }
        private void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }

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
