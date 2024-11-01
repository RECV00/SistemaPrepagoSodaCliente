package business;

import javafx.fxml.FXML;

import javafx.scene.control.Button;

import javafx.scene.control.TextField;

import java.io.File;

import javafx.event.ActionEvent;

import javafx.scene.control.ComboBox;

import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.PasswordField;

public class UIRegisterUsersClientController {
	 @FXML
	    private TextField tfUserID;
	    @FXML
	    private PasswordField tfPassword;
	    @FXML
	    private ComboBox<String> cbType;
	    @FXML
	    private ImageView photoPreview;

	    private String photoPath;

	    @FXML
	    private void initialize() {
	        cbType.getItems().addAll("Personal", "Estudiante"); // Ejemplo de opciones
	    }

	    @FXML
	    private void handleSelectPhoto() {
	        FileChooser fileChooser = new FileChooser();
	        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg"));
	        File file = fileChooser.showOpenDialog(null);
	        
	        if (file != null) {
	            photoPath = file.getAbsolutePath();
	            // Cargar la imagen en photoPreview
	            photoPreview.setImage(new javafx.scene.image.Image(file.toURI().toString()));
	        }
	    }

	    @FXML
	    private void handleRegister() {
	        // Cierra la ventana después de capturar los datos
	        Stage stage = (Stage) tfUserID.getScene().getWindow();
	        stage.close();
	    }

	    public String getUserID() {
	        return tfUserID.getText().trim();
	    }

	    public String getPassword() {
	        return tfPassword.getText();
	    }

	    public String getUserType() {
	        return cbType.getValue();
	    }

	    public String getPhotoPath() {
	        return photoPath;
	    }
}
