module SistemaPrepagoSodaCliente {
	requires javafx.controls;
	requires javafx.fxml;
	requires java.desktop;
	requires javafx.graphics;
	requires javafx.base;
	
	opens business to javafx.graphics, javafx.fxml;
}
