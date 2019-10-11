package com.moquality.android;

public class TestConstants {
    static final int SOCKET_IO_START = 100;
    public static final int SOCKET_IO_CONNECTED= 200;
    static final int SOCKET_IO_STOP = 102;
    static final int SOCKET_IO_DISCONNECTED = 202;
    public static final int SOCKET_IO_UPDATE_USERS = 103;
    public static final int SOCKET_IO_RECEIVE_MSG = 104;
    static final int SOCKET_IO_MSG_RECEIVED = 204;
    public static final int SOCKET_IO_SEND_MSG = 105;
    public static final int SOCKET_MSG_SENT = 205;

    static final String SOCKET_EVENT_STATUS = "status";
    static final String SOCKET_EVENT_RETURN = "return";
    static final String SOCKET_EVENT_CALL = "call";

    static final int DEFAULT_MODE = 0;
    static final int REFLECT_MODE = 1;

    static final String CHAT_SERVER_URL = "https://sio.moquality.com";
    static final String SOCKET_IO_NAMESPACE = "/connect/default";
}
