package com.carloscaceres.tienda.config;

/**
 * URL y credenciales del gestor seleccionado.
 * toString() se redefine para que la contraseña nunca acabe en un log por accidente.
 */
public record DatosConexion(String url, String usuario, String contrasena) {

    @Override
    public String toString() {
        return "DatosConexion[url=" + url + ", usuario=" + usuario + ", contrasena=****]";
    }
}
