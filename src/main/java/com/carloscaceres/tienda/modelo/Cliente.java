package com.carloscaceres.tienda.modelo;

import java.time.LocalDateTime;

public class Cliente {

    private int id;
    private final String nombre;
    private final String apellidos;
    private final String email;
    private final String telefono;
    private LocalDateTime fechaAlta;

    public Cliente(String nombre, String apellidos, String email, String telefono) {
        this(0, nombre, apellidos, email, telefono, null);
    }

    public Cliente(int id, String nombre, String apellidos, String email, String telefono,
                   LocalDateTime fechaAlta) {
        this.id = id;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.email = email;
        this.telefono = telefono;
        this.fechaAlta = fechaAlta;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public String getApellidos() { return apellidos; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public LocalDateTime getFechaAlta() { return fechaAlta; }

    public String getNombreCompleto() {
        return nombre + " " + apellidos;
    }

    @Override
    public String toString() {
        return "Cliente[id=" + id + ", nombre=" + getNombreCompleto() + ", email=" + email + "]";
    }
}
