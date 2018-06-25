package Aleksandrov.Chat;

/**
 * This is enum, which is responsible for the type of messages sent between the client and the server.
 */

public enum MessageType {
    NAME_REQUEST,
    USER_NAME,
    NAME_ACCEPTED,
    TEXT,
    USER_ADDED,
    USER_REMOVED
}
