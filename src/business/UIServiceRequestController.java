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
    private static final String ORDER_STATUS_RESPONSE = "ORDER_STATUS";
    @FXML
    private ComboBox<String> cbDayReservation;
    @FXML
    private RadioButton rbBreakfrast;
    @FXML
    private ToggleGroup Tipo;
    @FXML
    private RadioButton rbLunch;
    @FXML
    private TableView<List<String>> tableDishes;
    @FXML
    private TableColumn<List<String>, String> colDishes;
    @FXML
    private TableColumn<List<String>, String> colPrice;
    @FXML
    private TableColumn<List<String>, CheckBox> colSelection;
    @FXML
    private TableColumn<List<String>, Spinner<Integer>> colQuantity; 
    @FXML
    private TableColumn<List<String>, String> colState;
    @FXML
    private Button bBack;
    @FXML
    private Button bSendOrder;

    private ObservableList<List<String>> dishesList; // Cambiar a ObservableList de List<String>
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

        // Configuración de columnas de la tabla
        colDishes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(0)));
        colPrice.setCellValueFactory(data -> new SimpleStringProperty("$" + data.getValue().get(1)));
        
        // Configuración de la columna de selección
        colSelection.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            @Override
            protected void updateItem(CheckBox item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    int index = getIndex();
                    if (index >= 0 && index < dishesList.size()) {
                        checkBox.setSelected(checkBoxes.get(index).isSelected());
                        checkBoxes.set(index, checkBox);
                        setGraphic(checkBox);
                    }
                }
            }
        });

        // Configuración para colQuantity usando Spinner
        colQuantity.setCellFactory(col -> new TableCell<>() {
            private final Spinner<Integer> spinner = new Spinner<>(1, 100, 1);

            {
                spinner.setEditable(true);
                spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
                    if (getIndex() >= 0 && getIndex() < dishesList.size()) {
                        dishesList.get(getIndex()).set(3, String.valueOf(newValue)); // Actualizar cantidad
                    }
                });
            }

            @Override
            protected void updateItem(Spinner<Integer> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    int index = getIndex();
                    if (index >= 0 && index < dishesList.size()) {
                        setGraphic(spinner);
                        spinner.getValueFactory().setValue(Integer.parseInt(dishesList.get(index).get(3)));
                    }
                }
            }
        });

        colState.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(2)));

        cbDayReservation.getItems().addAll("Lunes", "Martes", "Miércoles", "Jueves", "Viernes");
        cbDayReservation.setOnAction(e -> loadDishes());
        Tipo.selectedToggleProperty().addListener((observable, oldValue, newValue) -> loadDishes());
    }

    private void setupTableAndData() {
        requestDishList();
    }

    private void requestDishList() {
        if (cbDayReservation.getValue() != null && Tipo.getSelectedToggle() != null) {
            loadDishes();
        }
    }

    private void loadDishes() {
        String selectedDay = cbDayReservation.getValue();
        String selectedMeal = rbBreakfrast.isSelected() ? "Desayuno" : "Almuerzo";

        connectionManager.sendMessage(LOAD_DISHES_REQUEST + "," + selectedDay + "," + selectedMeal);
        listenForDishListResponse();
    }

    private void listenForDishListResponse() {
        new Thread(() -> {
            String serverMessage;
            while ((serverMessage = connectionManager.receiveMessage()) != null) {
                processDishListResponse(serverMessage);
            }
            Platform.runLater(() -> showAlert("Conexión con el servidor finalizada."));
        }).start();
    }

    private void processDishListResponse(String serverMessage) {
        Platform.runLater(() -> {
            String[] parts = serverMessage.split(",");
            if (DISH_LIST_RESPONSE.equals(parts[0])) {
                updateDishesTable(parts);
            }
        });
    }


    private void updateDishesTable(String[] parts) {
        dishesList.clear();
        checkBoxes.clear();

        for (int i = 1; i < parts.length; i += 2) {
            String name = parts[i];
            String price = parts[i + 1];
            String state = " "; // Estado inicial
            List<String> dishData = new ArrayList<>();
            dishData.add(name);
            dishData.add(price);
            dishData.add(state);
            dishData.add("1"); // Cantidad inicial como "1"
            dishesList.add(dishData);
            checkBoxes.add(new CheckBox());
        }
    }
    
    @FXML
    private void confirmPurchase() {
        StringBuilder request = new StringBuilder(PURCHASE_REQUEST + "," + userID);
        double total = 0;
        boolean hasSelection = false;

        for (int i = 0; i < dishesList.size(); i++) {
            if (checkBoxes.get(i).isSelected()) {
                hasSelection = true;
                List<String> dishData = dishesList.get(i);
                String dishName = dishData.get(0);
                String dishPrice = dishData.get(1).replace("$", "");
                int quantity = Integer.parseInt(dishData.get(3));

                double dishTotal = quantity * Double.parseDouble(dishPrice);
                request.append(",").append(dishName).append(",").append(quantity).append(",").append(dishTotal);
                total += dishTotal;
            }
        }

        if (!hasSelection) {
            showAlert("Por favor, seleccione al menos un alimento.");
            return;
        }

       // request.append(",").append(total);
        connectionManager.sendMessage(request.toString());
        listenForOrderStatusResponse();
    }

    private void listenForOrderStatusResponse() {
        new Thread(() -> {
            String mensajeServidor;
            while ((mensajeServidor = connectionManager.receiveMessage()) != null) {
                processOrderStatusResponse(mensajeServidor);
            }
        }).start();
    }

    private void processOrderStatusResponse(String mensajeServidor) {
        Platform.runLater(() -> {
            String[] parts = mensajeServidor.split(",");
            if (ORDER_STATUS_RESPONSE.equals(parts[0])) {
                updateDishStatesFromOrderStatus(parts);
            }
        });
    }

    private void updateDishStatesFromOrderStatus(String[] parts) {
        for (int i = 1; i < parts.length; i += 2) {
            String name = parts[i];
            String state = parts[i + 1];
            updateDishState(name, state);
        }
    }

    private void updateDishState(String name, String state) {
        for (List<String> dishData : dishesList) {
            if (dishData.get(0).equals(name)) {
                dishData.set(2, state);
                break;
            }
        }
        tableDishes.refresh();
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/presentation/UILoginClient.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.show();
            Stage temp = (Stage) bBack.getScene().getWindow();
            temp.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}