package domain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ConnectionManager {

	private String hostServer;
    private static final int PORT = 12353;

    private PrintWriter salida;
    private BufferedReader entrada;
    private Socket socket;
    private boolean isConnected = false;

    public ConnectionManager(String hostServer) {
        this.hostServer = hostServer;
    }

    public String getServerIP() {
        return hostServer;
    }

    public boolean connect() {
        try {
            socket = new Socket(hostServer, PORT);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);
            isConnected = true;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void sendLogin(String userID, String password) {
        if (isConnected) {
            salida.println("LOGIN" +","+ userID + "," + password);
        }
    }

    public BufferedReader getEntrada() {
        return entrada;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void sendMessage(String message) {
        if (isConnected && salida != null) {
            salida.println(message);
        }
    }

    public String receiveMessage() {
        try {
            if (isConnected && entrada != null) {
                return entrada.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

   
    public void close() {
        try {
            isConnected = false;
            if (salida != null) salida.close();
            if (entrada != null) entrada.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    
    }
}