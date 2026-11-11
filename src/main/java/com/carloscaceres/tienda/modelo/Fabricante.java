package com.carloscaceres.tienda.modelo;

public class Fabricante {

    private int id;
    private String nombre;
    private String pais;

    public Fabricante(String nombre, String pais) {
        this(0, nombre, pais);
    }

    public Fabricante(int id, String nombre, String pais) {
        this.id = id;
        this.nombre = nombre;
        this.pais = pais;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public String getPais() { return pais; }

    @Override
    public String toString() {
        return "Fabricante[id=" + id + ", nombre=" + nombre + ", pais=" + pais + "]";
    }
}
