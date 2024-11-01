package business;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label; // Para mostrar mensajes
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.BufferedReader;
import java.io.IOException;

import domain.ConnectionManager;

public class UILoginController {

    private ConnectionManager connectionManager;

    @FXML
    private TextField tfID;
    @FXML
    private PasswordField tfPassword;
    @FXML
    private TextField tfVisiblePassword;
    @FXML
    private TextField tfServerIP; 
    @FXML
    private Button bTogglePassword;
    @FXML
    private Button bLogin;
   
    private boolean isPasswordVisible = false;

    // Etiqueta para mostrar mensajes de respuesta
    @FXML
    private Label lblResponse;

    @FXML
    private void initialize() {
        tfVisiblePassword.textProperty().bindBidirectional(tfPassword.textProperty());
    }

    @FXML
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        tfVisiblePassword.setVisible(isPasswordVisible);
        tfVisiblePassword.setManaged(isPasswordVisible);
        tfPassword.setVisible(!isPasswordVisible);
        tfPassword.setManaged(!isPasswordVisible);
        bTogglePassword.setText(isPasswordVisible ? "🐄" : "👁");
    }

    @FXML
    private void onLogin() {
        String userID = tfID.getText().trim();
        String password = tfPassword.getText();
        String serverIP = tfServerIP.getText().trim(); // Obtiene la IP ingresada

        if (userID.isEmpty() || password.isEmpty() || serverIP.isEmpty()) {
            updateResponse("Por favor, complete todos los campos.");
            return;
        }

        connectionManager = new ConnectionManager(serverIP); // Pasa la IP al ConnectionManager
        if (connectionManager.connect()) {
            connectionManager.sendLogin(userID, password);
            new Thread(this::listenForServerMessages).start();
        } else {
            updateResponse("Error de conexión con el servidor.");
        }
    }

    private void listenForServerMessages() {
        BufferedReader entrada = connectionManager.getEntrada();
        String mensajeServidor;
        try {
            while ((mensajeServidor = entrada.readLine()) != null) {
                processServerMessage(mensajeServidor);
            }
        } catch (IOException e) {
            System.out.println("Error al recibir datos del servidor: " + e.getMessage());
        }
    }

    private void processServerMessage(String mensajeServidor) {
        Platform.runLater(() -> {
            String[] parts = mensajeServidor.split(",");

            if (parts.length > 0) {
                String status = parts[0];
                if ("SUCCESS".equals(status)) {
                    openServiceRequestWindow(parts[1]);
                } else {
                    updateResponse("Autenticación fallida: " + parts[1]);
                }
            }
        });
    }

    private void openServiceRequestWindow(String userID) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/presentation/UIServiceRequest.fxml"));
            Parent root = loader.load();

            // Obtener el controlador de la ventana de solicitud de servicio
            UIServiceRequestController controller = loader.getController();
            // Pasar el userID y el serverIP al controlador
            controller.initializeData(userID, connectionManager.getServerIP());
            // Mostrar la nueva ventana
            Stage stage = new Stage();
            stage.setTitle("Service Request");
            stage.setScene(new Scene(root));
            stage.show();
            // Cerrar la ventana de inicio de sesión actual
            Stage currentStage = (Stage) bLogin.getScene().getWindow();
            currentStage.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error al abrir la ventana de solicitud de servicio.");
        }
    }

    public void updateResponse(String message) {
        Platform.runLater(() -> {
            lblResponse.setText(message);
            System.out.println(message);
        });
    }

    @FXML
    protected void finalize() throws Throwable {
        connectionManager.close();
        super.finalize();
    }
}