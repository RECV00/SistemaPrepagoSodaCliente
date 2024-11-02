package business;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.BufferedReader;
import java.io.IOException;

import domain.ConnectionManager;

public class UILoginClientController {

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
        String serverIP = tfServerIP.getText().trim();

        if (userID.isEmpty() || password.isEmpty() || serverIP.isEmpty()) {
            updateResponse("Por favor, complete todos los campos.");
            return;
        }

        // Intentar establecer la conexión y enviar credenciales en un hilo separado.
        new Thread(() -> {
            connectionManager = new ConnectionManager(serverIP);
            if (connectionManager.connect()) {
                // Enviar las credenciales al servidor.
                connectionManager.sendLogin(userID, password);
                // Escuchar la respuesta del servidor.
                listenForServerMessages();
            } else {
                updateResponse("Error de conexión con el servidor.");
            }
        }).start();
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
            updateResponse("Error al recibir datos del servidor.");
        } finally {
            // Asegúrate de cerrar la conexión si se sale del bucle.
            connectionManager.close();
        }
    }

    private void processServerMessage(String mensajeServidor) {
        Platform.runLater(() -> {
            String[] parts = mensajeServidor.split(",");
            if (parts.length > 0) {
                switch (parts[0]) {
                    case "SUCCESS":
                        if (parts.length > 1) {
                            openServiceRequestWindow(parts[1]);
                        }
                        break;
                    case "FAILURE":
                        updateResponse("Autenticación fallida: " + (parts.length > 1 ? parts[1] : "Error desconocido"));
                        break;
                    default:
                        updateResponse("Respuesta desconocida del servidor: " + mensajeServidor);
                        break;
                }
            }
        });
    }

    private void openServiceRequestWindow(String userID) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/presentation/UIServiceRequest.fxml"));
            Parent root = loader.load();
            UIServiceRequestController controller = loader.getController();
            controller.initializeData(userID, connectionManager);

            Stage stage = new Stage();
            stage.setTitle("Service Request");
            stage.setScene(new Scene(root));
            stage.show();

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