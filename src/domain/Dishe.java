package domain;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.CheckBox;

public  class Dishe {
	private String name;
    private double price;
    private CheckBox select;

    public Dishe(String name, double price, CheckBox select) {
        this.name = name;
        this.price = price;
        this.select = select;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public CheckBox getSelect() {
        return select;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setSelect(CheckBox select) {
        this.select = select;
    }
}
