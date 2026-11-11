package com.carloscaceres.tienda.modelo;

/** Dirección de envío. Un cliente puede tener varias (1:N); una puede marcarse como principal. */
public class Direccion {

    private int id;
    private int clienteId;
    private final String calle;
    private final String ciudad;
    private final String codigoPostal;
    private final String provincia;
    private final boolean principal;

    public Direccion(String calle, String ciudad, String codigoPostal, String provincia, boolean principal) {
        this(0, 0, calle, ciudad, codigoPostal, provincia, principal);
    }

    public Direccion(int id, int clienteId, String calle, String ciudad, String codigoPostal,
                     String provincia, boolean principal) {
        this.id = id;
        this.clienteId = clienteId;
        this.calle = calle;
        this.ciudad = ciudad;
        this.codigoPostal = codigoPostal;
        this.provincia = provincia;
        this.principal = principal;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }
    public String getCalle() { return calle; }
    public String getCiudad() { return ciudad; }
    public String getCodigoPostal() { return codigoPostal; }
    public String getProvincia() { return provincia; }
    public boolean isPrincipal() { return principal; }

    @Override
    public String toString() {
        return calle + ", " + codigoPostal + " " + ciudad + " (" + provincia + ")" + (principal ? " [principal]" : "");
    }
}
