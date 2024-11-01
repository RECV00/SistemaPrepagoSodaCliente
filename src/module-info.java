module SistemaPrepagoSodaCliente {
	requires javafx.controls;
	requires javafx.fxml;
	requires java.desktop;
	
	opens business to javafx.graphics, javafx.fxml;
}
