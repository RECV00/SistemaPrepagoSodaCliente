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
    private TableColumn<List<String>, String> colQuantity;
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

        // Configura las columnas de la tabla
        colDishes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().get(0)));
        colPrice.setCellValueFactory(data -> new SimpleStringProperty("$" + data.getValue().get(1)));

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
                        checkBoxes.set(index, checkBox); // Actualiza el CheckBox correspondiente
                        setGraphic(checkBox);
                    }
                }
            }
        });

        // Configura la columna de cantidad
        colQuantity.setCellFactory(col -> new TableCell<List<String>, String>() {
            private final TextField textField = new TextField();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    textField.setText(item);
                    setGraphic(textField);
                    textField.setOnKeyReleased(e -> {
                        commitEdit(textField.getText()); // Commitear el nuevo valor al editar
                    });
                }
            }

            @Override
            public void commitEdit(String newValue) {
                super.commitEdit(newValue);
                if (getTableRow() != null && getTableRow().getItem() != null) {
                    List<String> rowData = getTableRow().getItem();
                    rowData.set(3, newValue); // Actualiza la cantidad en la lista de datos
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
        String diaSeleccionado = cbDayReservation.getValue();
        String horarioSeleccionado = rbBreakfrast.isSelected() ? "Desayuno" : "Almuerzo";

        connectionManager.sendMessage(LOAD_DISHES_REQUEST + "," + diaSeleccionado + "," + horarioSeleccionado);
        listenForDishListResponse(); // Escucha solo la respuesta de la lista de platillos
    }

    private void listenForDishListResponse() {
        new Thread(() -> {
            String mensajeServidor;
            while ((mensajeServidor = connectionManager.receiveMessage()) != null) {
                processDishListResponse(mensajeServidor);
            }
            Platform.runLater(() -> showAlert("Conexión con el servidor finalizada."));
        }).start();
    }

    private void processDishListResponse(String mensajeServidor) {
        Platform.runLater(() -> {
            String[] parts = mensajeServidor.split(",");
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
            String state = "Pendiente"; // Estado inicial
            List<String> dishData = new ArrayList<>();
            dishData.add(name);
            dishData.add(price);
            dishData.add(state);
            dishesList.add(dishData); // Agregar el List<String> a la lista observable
            checkBoxes.add(new CheckBox()); // Añadir un CheckBox correspondiente a la fila
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
                String dishName = dishData.get(0); // Nombre del platillo
                String dishPrice = dishData.get(1).replace("$", ""); // Precio del platillo sin el símbolo de dólar
                
                // Obtener la cantidad ingresada
                TextField quantityField = (TextField) tableDishes.getColumns().get(2).getCellObservableValue(i).getValue();
                String quantityText = quantityField.getText();
                int quantity = quantityText.isEmpty() ? 1 : Integer.parseInt(quantityText); // Default a 1 si está vacío
                
                // Calcular el total para este platillo
                double dishTotal = quantity * Double.parseDouble(dishPrice);

                // Agregar nombre, cantidad y total a la solicitud
                request.append(",").append(dishName).append(",").append(quantity).append(",").append(dishTotal);
                total += dishTotal; // Sumar al total general
            }
        }

        if (!hasSelection) {
            showAlert("Por favor, seleccione al menos un alimento.");
            return;
        }

        // Agregar total general a la solicitud
        request.append(",").append(total);
        connectionManager.sendMessage(request.toString());
        listenForOrderStatusResponse(); // Escuchar la respuesta de estado de la orden
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
            String state = parts[i + 1]; // Obtener el estado completo
            updateDishState(name, state);
        }
    }

    private void updateDishState(String name, String state) {
        for (List<String> dishData : dishesList) {
            if (dishData.get(0).equals(name)) { // Verifica si el nombre coincide
                dishData.set(2, state); // Actualiza el estado en la lista
                break;
            }
        }
        tableDishes.refresh(); // Refresca la tabla para mostrar el nuevo estado
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