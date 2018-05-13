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
            System.out.println("Отправка не удалась");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Введите порт для подключения:");
        try (ServerSocket serverSocket = new ServerSocket(ConsoleHelper.readInt())) {
            ConsoleHelper.writeMessage("Ожидание подключения...");
            while (true) {
                Handler handler = new Handler(serverSocket.accept());
                handler.start();
            }
        } catch (Exception e) {
            ConsoleHelper.writeMessage("Ошибка. Сокет закрыт.");
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
                connection.send(new Message(MessageType.NAME_REQUEST, "Введите имя"));
                Message messageReceive = connection.receive();
                if(messageReceive.getType() != MessageType.USER_NAME || messageReceive.getData().isEmpty())
                    continue;
                name = messageReceive.getData();
                if(connectionMap.containsKey(name))
                    continue;
                connectionMap.put(name, connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED, "имя принято"));
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
                    ConsoleHelper.writeMessage("Ошибка. Неправильный тип сообщения.");
            }
        }

        public void run() {
            ConsoleHelper.writeMessage("Установлено новое соединение: " + socket.getRemoteSocketAddress());
            String name = null;
            try (Connection connection = new Connection(socket)){
                name = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, name));
                this.sendListOfUsers(connection, name);
                this.serverMainLoop(connection, name);
            } catch (IOException e) {
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом");
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом");
                e.printStackTrace();
            }
            finally {
                if (name != null)
                    connectionMap.remove(name);
                    sendBroadcastMessage(new Message(MessageType.USER_REMOVED, name));
                    ConsoleHelper.writeMessage("Соединение с удаленным адресом закрыто");
            }
        }
    }
}
