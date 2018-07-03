package aleksandrov.chat;

import java.io.Serializable;

/**
 * This class responsible for forwarded messages
 */

public class Message implements Serializable {
    private final MessageType type;
    private final String data;

    /**
     * constructor initializes final fields
     * @param type
     * @param data
     */
    public Message(MessageType type, String data) {
        this.type = type;
        this.data = data;
    }

    /**
     * method returns type of the message
     * @return
     */
    public MessageType getType() {
        return type;
    }

    /**
     * method returns data of the message
     * @return
     */
    public String getData() {
        return data;
    }
}
