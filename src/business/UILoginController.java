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
    private Button bTogglePassword;
    @FXML
    private Button bLogin;
    @FXML
    private Button bRegister;

    private boolean isPasswordVisible = false;

    // Etiqueta para mostrar mensajes de respuesta
    @FXML
    private Label lblResponse;

    @FXML
    private void initialize() {
        // Sincronizar el texto entre los dos campos al iniciar
        tfVisiblePassword.textProperty().bindBidirectional(tfPassword.textProperty());
        connectionManager = new ConnectionManager();
        connectionManager.connect(); // Intentar conectarse al servidor
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

        if (userID.isEmpty() || password.isEmpty()) {
            updateResponse("Por favor, complete todos los campos.");
            return;
        }

        connectionManager.sendLogin(userID, password);
        new Thread(() -> listenForServerMessages()).start(); // Inicia un hilo para escuchar mensajes
    }

    private void listenForServerMessages() {
        BufferedReader entrada = connectionManager.getEntrada();
        String mensajeServidor;
        try {
            while ((mensajeServidor = entrada.readLine()) != null) {
                processServerMessage(mensajeServidor);
            }
        } catch (IOException e) {
            updateResponse("Error al recibir datos del servidor: " + e.getMessage());
        }
    }

    private void processServerMessage(String mensajeServidor) {
        Platform.runLater(() -> {
            String[] parts = mensajeServidor.split(",");

            if (parts.length > 0) {
                String status = parts[0];
                if ("SUCCESS".equals(status)) {
                    openServiceRequestWindow(parts[1]); // userID como parte del mensaje
                } else {
                    updateResponse("Autenticación fallida: " + parts[1]);
                }
            }
        });
    }

    private void openServiceRequestWindow(String userID) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/presentation/UIProfile.fxml"));
            Parent root = loader.load();

            // Obtener el controlador y pasar datos
            UIServiceRequestController controller = loader.getController();
            //controller.initializeData(userID);

            Stage stage = new Stage();
            stage.setTitle("Service Request");
            stage.setScene(new Scene(root));
            stage.show();

            // Cerrar ventana de inicio de sesión
            Stage currentStage = (Stage) bLogin.getScene().getWindow();
            currentStage.close();
        } catch (IOException e) {
            e.printStackTrace();
            updateResponse("Error al abrir la ventana de solicitud de servicio.");
        }
    }

    @FXML
    private void saveRegister() {
        String userID = tfID.getText().trim();
        String password = tfPassword.getText();

        if (userID.isEmpty() || password.isEmpty()) {
            updateResponse("Por favor, complete todos los campos.");
            return;
        }

        connectionManager.sendRegister(userID, password);
    }

    public void updateResponse(String message) {
        Platform.runLater(() -> {
            lblResponse.setText(message); // Mostrar el mensaje en la etiqueta
            System.out.println(message);
        });
    }

    @FXML
    protected void finalize() throws Throwable {
        connectionManager.close(); // Cerrar conexión al final
        super.finalize();
    }
}