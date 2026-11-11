package com.carloscaceres.tienda.modelo;

/**
 * Socket de procesador (AM5, LGA1700...). Se llama SocketCpu y no Socket
 * para no confundirlo con java.net.Socket.
 */
public class SocketCpu {

    private int id;
    private String nombre;

    public SocketCpu(String nombre) {
        this(0, nombre);
    }

    public SocketCpu(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }

    @Override
    public String toString() {
        return "SocketCpu[id=" + id + ", nombre=" + nombre + "]";
    }
}
