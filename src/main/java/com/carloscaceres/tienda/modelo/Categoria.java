package com.carloscaceres.tienda.modelo;

/** Agrupación comercial (Gaming, Ofertas...). Un producto puede pertenecer a varias (N:M). */
public class Categoria {

    private int id;
    private String nombre;
    private String descripcion;

    public Categoria(String nombre, String descripcion) {
        this(0, nombre, descripcion);
    }

    public Categoria(int id, String nombre, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }

    @Override
    public String toString() {
        return "Categoria[id=" + id + ", nombre=" + nombre + "]";
    }
}
