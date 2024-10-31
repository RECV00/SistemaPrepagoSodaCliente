package business;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class UILoginController {

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

    @FXML
    private void initialize() {
        // Sincronizar el texto entre los dos campos al iniciar
        tfVisiblePassword.textProperty().bindBidirectional(tfPassword.textProperty());
    }

    @FXML
    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Ocultar la contraseña
            tfVisiblePassword.setVisible(false);
            tfVisiblePassword.setManaged(false);
            tfPassword.setVisible(true);
            tfPassword.setManaged(true);
            bTogglePassword.setText("👁"); // Cambia el ícono del botón
            isPasswordVisible = false;
        } else {
            // Mostrar la contraseña
            tfVisiblePassword.setVisible(true);
            tfVisiblePassword.setManaged(true);
            tfPassword.setVisible(false);
            tfPassword.setManaged(false);
            bTogglePassword.setText("🐄"); // Cambia el ícono del botón
            isPasswordVisible = true;
        }
    }

    @FXML
    private void onLogin() {
        // Lógica para el inicio de sesión
    }

    @FXML
    private void saveRegister() {
        // Lógica para registrar
    }
}

