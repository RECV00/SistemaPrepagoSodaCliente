package business;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import domain.ConnectionManager;
import domain.Dishe;

import java.io.IOException;
import java.util.List;

public class UIServiceRequestController {

	private ConnectionManager connectionManager;
    private String userID;
    private static final String DISH_LIST_RESPONSE = "DISH_LIST";
    private static final String LOAD_DISHES_REQUEST = "LOAD_DISHES";
    private static final String PURCHASE_REQUEST = "PURCHASE";
    
    @FXML
    private ComboBox<String> cbDiaReservacion;
    @FXML
    private RadioButton rbDesayuno;
    @FXML
    private ToggleGroup Tipo;
    @FXML
    private RadioButton rbAlmuerzo;
    @FXML
    private TableView<Dishe> tableDishes;
    @FXML
    private TableColumn<Dishe, String> colAlimento;
    @FXML
    private TableColumn<Dishe, Double> colPrecio;
    @FXML
    private TableColumn<Dishe, CheckBox> colSolicitar;
    @FXML
    private Button bBack;
    @FXML
    private Button bSendOrder;

    private ObservableList<Dishe> dishesList;

    public void initializeData(String userID, ConnectionManager connectionManager) {
        this.userID = userID;
        this.connectionManager = connectionManager; // Asignar el ConnectionManager proporcionado
        initialize();  // Inicializar la interfaz gráfica

        new Thread(() -> {
            if (connectionManager.connect()) {
                Platform.runLater(this::setupTableAndData);
            } else {
                Platform.runLater(() -> showAlert("No se pudo conectar al servidor de SODA."));
            }
        }).start();
    }
    
    @FXML
    private void initialize() {
        colAlimento.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("price"));
        colSolicitar.setCellValueFactory(new PropertyValueFactory<>("select")); // Esto mostrará el CheckBox

        dishesList = FXCollections.observableArrayList();
        tableDishes.setItems(dishesList);
        cbDiaReservacion.getItems().addAll("Lunes", "Martes", "Miércoles", "Jueves", "Viernes");

        // Agregar listeners para cambios en el ComboBox y los RadioButtons
        cbDiaReservacion.setOnAction(e -> loadDishes());
        Tipo.selectedToggleProperty().addListener((observable, oldValue, newValue) -> loadDishes());
    }
    
 
    private void setupTableAndData() {
        requestDishList();
    }

    private void requestDishList() {
        if (cbDiaReservacion.getValue() != null && Tipo.getSelectedToggle() != null) {
            loadDishes();
        }
    }

    private void loadDishes() {
        String diaSeleccionado = cbDiaReservacion.getValue();
        String horarioSeleccionado = rbDesayuno.isSelected() ? "Desayuno" : "Almuerzo";

        connectionManager.sendMessage(LOAD_DISHES_REQUEST + "," + diaSeleccionado + "," + horarioSeleccionado);
        listenForServerMessages(); 
    }

    private void listenForServerMessages() {
        new Thread(() -> {
            String mensajeServidor;
            while ((mensajeServidor = connectionManager.receiveMessage()) != null) {
                processServerMessage(mensajeServidor);
            }
            Platform.runLater(() -> showAlert("Conexión con el servidor finalizada."));
        }).start();
    }

    private void processServerMessage(String mensajeServidor) {
        Platform.runLater(() -> {
            String[] parts = mensajeServidor.split(",");
            if (DISH_LIST_RESPONSE.equals(parts[0])) {
                try {
                    dishesList.clear(); 
                    for (int i = 1; i < parts.length; i += 2) {
                        String name = parts[i];
                        double price = Double.parseDouble(parts[i + 1]);
                        CheckBox select = new CheckBox(); 
                        dishesList.add(new Dishe(name, price, select));
                    }
                } catch (NumberFormatException e) {
                    showAlert("Error al procesar los precios de los platos.");
                } catch (ArrayIndexOutOfBoundsException e) {
                    showAlert("Respuesta del servidor en un formato inesperado.");
                }
            }
        });
    }
    @FXML
    private void SaveDishes() {
    	System.out.print("Eviando pedido");
    }
    @FXML
    private void confirmPurchase() {
        List<Dishe> selectedDishes = dishesList.filtered(dish -> dish.getSelect().isSelected());
        if (selectedDishes.isEmpty()) {
            showAlert("Por favor, seleccione al menos un alimento.");
            return;
        }
        double total = 0;
        StringBuilder request = new StringBuilder(PURCHASE_REQUEST + "," + userID);

        for (Dishe dish : selectedDishes) {
            request.append(",").append(dish.getName());
            total += dish.getPrice();
        }
        request.append(",").append(total);

        connectionManager.sendMessage(request.toString());
        showAlert("Compra confirmada con éxito. Total: $" + total);
    }

    @FXML
    private void closeConnectionOnExit() {
        if (connectionManager != null) {
            connectionManager.close();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    public void returnLogin() {
    	closeWindows();
    }
   	public void closeWindows() {
   		
   		try {
   			 FXMLLoader loader = new FXMLLoader (getClass().getResource("/presentation/UILoginClient.fxml"));
   	        Parent root = loader.load();
   			Scene scene = new Scene(root);		
   	        Stage stage = new Stage();
   	        stage.setScene(scene);
   	        stage.show();			        
   	        Stage temp = (Stage) bBack.getScene().getWindow();
   	        temp.close();
   		} catch (IOException e) {
   			// TODO Auto-generated catch block
   			e.printStackTrace();
   		}
   	}
}