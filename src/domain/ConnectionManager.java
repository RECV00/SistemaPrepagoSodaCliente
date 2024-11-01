package domain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ConnectionManager {

    private static final String HOST_SERVER = "localhost";
    private static final int PORT = 12348;

    private PrintWriter salida;
    private BufferedReader entrada;
    private Socket socket;
    private boolean isConnected = false;

    public boolean connect() {
        try {
            socket = new Socket(HOST_SERVER, PORT);
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
            salida.println("LOGIN," + userID + "," + password);
        }
    }

    public void sendRegister(String userID, String password, String userType, String profilePhotoPath) {
        String message = "REGISTER," + userID + "," + password + "," + userType + "," + profilePhotoPath;
        salida.println(message);
    }

    public BufferedReader getEntrada() {
        return entrada;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
            isConnected = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}