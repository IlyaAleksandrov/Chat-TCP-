package aleksandrov.chat;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * The Connection class will act as a wrapper over the class java.net.Socket,
 * which will be able to serialize and deserialize objects type Message to the socket.
 */

public class Connection implements Closeable {
    /**
     * Fields for socket and it's streams
     */
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    /**
     * constructor initialise sockets input and output streams
     * @param socket
     * @throws IOException
     */
    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    /**
     * This method serializes the message into ObjectOutputStream
     * @param message
     * @throws IOException
     */
    public void send(Message message) throws IOException{
        synchronized (out) {
            out.writeObject(message);
        }
    }

    /**
     * This method de-serializes the message from ObjectOutputStream
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Message receive() throws IOException, ClassNotFoundException{
        synchronized (in) {
            return (Message) in.readObject();
        }
    }

    /**
     * This method closes all existed streams
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        socket.close();
        in.close();
        out.close();
    }
}
