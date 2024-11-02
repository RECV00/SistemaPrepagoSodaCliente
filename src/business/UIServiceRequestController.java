package business;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import domain.ConnectionManager;
import domain.Dishe;

import java.util.List;

public class UIServiceRequestController {

	private ConnectionManager connectionManager;
    private String userID;
    
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
    private Button bRegresar;
    @FXML
    private Button bAgregarAlimento;

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
        colSolicitar.setCellValueFactory(new PropertyValueFactory<>("select"));

        dishesList = FXCollections.observableArrayList();
        tableDishes.setItems(dishesList);
        cbDiaReservacion.getItems().addAll("Lunes", "Martes", "Miércoles", "Jueves", "Viernes");
    }
    
 
    private void setupTableAndData() {
        requestDishList(); // Llama a requestDishList para cargar inicialmente la lista de platillos si hay selecciones válidas
    }

    private void requestDishList() {
        // Verifica si hay un día seleccionado y si se ha seleccionado un horario
        if (cbDiaReservacion.getValue() != null && Tipo.getSelectedToggle() != null) {
            loadDishes(); // Llama a loadDishes solo si ambas selecciones son válidas
        }
    }

    private void loadDishes() {
        String diaSeleccionado = cbDiaReservacion.getValue();
        String horarioSeleccionado = rbDesayuno.isSelected() ? "Desayuno" : rbAlmuerzo.isSelected() ? "Almuerzo" : null;

        if (diaSeleccionado == null || horarioSeleccionado == null) {
            return; // No enviar solicitud si no hay selección
        }

        connectionManager.sendMessage("LOAD_DISHES" +","+ diaSeleccionado + "," + horarioSeleccionado);
        listenForServerMessages(); // Escuchar mensajes del servidor después de enviar la solicitud
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
            if ("DISH_LIST".equals(parts[0])) {
                dishesList.clear();
                for (int i = 1; i < parts.length; i += 2) {
                    String name = parts[i];
                    double price = Double.parseDouble(parts[i + 1]);
                    dishesList.add(new Dishe(name, price, new CheckBox()));
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
        StringBuilder request = new StringBuilder("PURCHASE," + userID); // Incluye el userID al inicio del mensaje

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
}