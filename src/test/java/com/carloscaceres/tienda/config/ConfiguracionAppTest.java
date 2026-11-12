package com.carloscaceres.tienda.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracionAppTest {

    private Properties propiedadesValidas() {
        Properties propiedades = new Properties();
        propiedades.setProperty("database.engine", "h2");
        propiedades.setProperty("connection.mode", "POOL");
        propiedades.setProperty("simulation.mode", "NORMAL");
        propiedades.setProperty("h2.url", "jdbc:h2:mem:x");
        propiedades.setProperty("h2.user", "sa");
        return propiedades;
    }

    @Test
    @DisplayName("Configuración: elige el gestor y usa sus propiedades sin exponer la contraseña")
    void leeConfiguracionValida() {
        ConfiguracionApp config = ConfiguracionApp.desdePropiedades(propiedadesValidas());
        assertEquals(MotorBd.H2, config.motor());
        assertEquals(ModoConexion.POOL, config.modoConexion());
        assertEquals("jdbc:h2:mem:x", config.datosConexion().url());
        assertTrue(config.datosConexion().toString().contains("****"));
    }

    @Test
    @DisplayName("Configuración: un valor no admitido detiene el arranque con un mensaje claro")
    void rechazaValorNoValido() {
        Properties propiedades = propiedadesValidas();
        propiedades.setProperty("connection.mode", "POOLS");
        ConfiguracionException error = assertThrows(ConfiguracionException.class,
                () -> ConfiguracionApp.desdePropiedades(propiedades));
        assertTrue(error.getMessage().contains("connection.mode"));
    }
}
