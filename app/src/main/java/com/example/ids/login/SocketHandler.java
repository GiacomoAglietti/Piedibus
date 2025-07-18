package com.example.ids.login;


import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import kotlin.jvm.Synchronized;

public class SocketHandler{
    private static SocketHandler socketHandler = null;
    private static Socket socket;

    private static final String ipAddress="192.168.136.115";

    private final String URI = "http://" + ipAddress + ":3000";

    private SocketHandler(){}

    public static SocketHandler getInstance(){
        if (socketHandler==null)
            socketHandler = new SocketHandler();
        return socketHandler;
    }

    @Synchronized
    public void setSocket(){
        try{
                socket = IO.socket(URI);
        }catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Synchronized
    public Socket getSocket(){
        return socket;
    }

    @Synchronized
    public void establishConnection(){
        socket.connect();
    }


    @Synchronized
    public void closeConnection() {
        socket.disconnect();
        socket.off();
        socket = null;
        socketHandler = null;
    }

}
