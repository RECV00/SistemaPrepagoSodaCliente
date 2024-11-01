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

    @FXML
    private ComboBox<String> cbEstudiante;
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
    private ObservableList<String> estudiantesList;

    @FXML
    private void initialize() {
        connectionManager = new ConnectionManager();
        connectionManager.connect();

        // Configuración inicial de las columnas de la tabla
        colAlimento.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("price"));
        colSolicitar.setCellValueFactory(new PropertyValueFactory<>("select"));

        dishesList = FXCollections.observableArrayList();
        estudiantesList = FXCollections.observableArrayList();
        tableDishes.setItems(dishesList);
        cbEstudiante.setItems(estudiantesList);
        cbDiaReservacion.getItems().addAll("Lunes","Martes","Miércoles","Jueves","Viernes");

        // Solicitar lista de estudiantes al servidor
        requestStudentList();
    }

    private void requestStudentList() {
        // Enviar solicitud para obtener la lista de estudiantes
        connectionManager.sendMessage("GET_STUDENT_LIST");
        
        // Escuchar la respuesta del servidor en un hilo aparte
        new Thread(this::listenForServerMessages).start();
    }

    private void listenForServerMessages() {
        String mensajeServidor;
        while ((mensajeServidor = connectionManager.receiveMessage()) != null) {
            processServerMessage(mensajeServidor);
        }
    }

    private void processServerMessage(String mensajeServidor) {
        Platform.runLater(() -> {
            String[] parts = mensajeServidor.split(",");
            switch (parts[0]) {
                case "STUDENT_LIST":
                    estudiantesList.clear();
                    for (int i = 1; i < parts.length; i++) {
                        estudiantesList.add(parts[i]);
                    }
                    break;
                case "DISH_LIST":
                    dishesList.clear();
                    for (int i = 1; i < parts.length; i += 2) {
                        String name = parts[i];
                        double price = Double.parseDouble(parts[i + 1]);
                        dishesList.add(new Dishe(name, price, new CheckBox()));
                    }
                    break;
            }
        });
    }

    // Método para cargar los platos según el día y horario
    @FXML
    private void loadDishes() {
        String diaSeleccionado = cbDiaReservacion.getValue();
        String horarioSeleccionado = rbDesayuno.isSelected() ? "Desayuno" : "Almuerzo";
        
        connectionManager.sendReservationRequest(diaSeleccionado, horarioSeleccionado);
    }

    @FXML
    private void confirmPurchase() {
        String estudiante = cbEstudiante.getValue();
        List<Dishe> selectedDishes = dishesList.filtered(dish -> dish.getSelect().isSelected());

        if (estudiante == null || selectedDishes.isEmpty()) {
            showAlert("Por favor, seleccione un estudiante y al menos un alimento.");
            return;
        }

        double total = 0; // Variable para acumular el total de los platillos seleccionados
        StringBuilder request = new StringBuilder("PURCHASE," + estudiante);

        for (Dishe dish : selectedDishes) {
            request.append(",").append(dish.getName());
            total += dish.getPrice(); // Sumar el precio del platillo seleccionado
        }

        // Agregar el total a la solicitud
        request.append(",").append(total);

        connectionManager.sendMessage(request.toString());
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }
}