package business;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import domain.ConnectionManager;

import java.io.IOException;
import java.util.ArrayList;
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
    private TableView<String> tableDishes;
    @FXML
    private TableColumn<String, String> colAlimento;
    @FXML
    private TableColumn<String, CheckBox> colSolicitar;
    @FXML
    private Button bBack;
    @FXML
    private Button bSendOrder;

    private ObservableList<String> dishesList;
    private List<CheckBox> checkBoxes;  

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
        dishesList = FXCollections.observableArrayList();
        checkBoxes = new ArrayList<>();
        tableDishes.setItems(dishesList);

        colAlimento.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()));
        colSolicitar.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            @Override
            protected void updateItem(CheckBox item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    int index = getIndex();
                    if (index >= 0 && index < checkBoxes.size()) {
                        setGraphic(checkBoxes.get(index));
                    }
                }
            }
        });

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
            loadDishes(); // Llama a loadDishes solo si ambas selecciones son válidas
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
            	System.out.print(mensajeServidor);
                processServerMessage(mensajeServidor);
            }
            Platform.runLater(() -> showAlert("Conexión con el servidor finalizada."));
        }).start();
    }
    private void processServerMessage(String mensajeServidor) {
        Platform.runLater(() -> {
            String[] parts = mensajeServidor.split(",");
            if (DISH_LIST_RESPONSE.equals(parts[0])) {
                dishesList.clear(); // Limpiar la lista actual de platos
                checkBoxes.clear(); // Limpiar la lista de CheckBoxes antes de agregar nuevos

                for (int i = 1; i < parts.length; i += 2) {
                    String name = parts[i];
                    String price = parts[i + 1];
                    String dishInfo = name + " - $" + price;
                    dishesList.add(dishInfo);
                    checkBoxes.add(new CheckBox()); // Añadir un CheckBox correspondiente
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
        StringBuilder request = new StringBuilder(PURCHASE_REQUEST + userID);
        double total = 0;
        boolean hasSelection = false;

        for (int i = 0; i < dishesList.size(); i++) {
            if (checkBoxes.get(i).isSelected()) {
                hasSelection = true;
                String[] dishDetails = dishesList.get(i).split(" - \\$");
                String name = dishDetails[0];
                double price = Double.parseDouble(dishDetails[1]);
                request.append(",").append(name);
                total += price;
            }
        }

        if (!hasSelection) {
            showAlert("Por favor, seleccione al menos un alimento.");
            return;
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