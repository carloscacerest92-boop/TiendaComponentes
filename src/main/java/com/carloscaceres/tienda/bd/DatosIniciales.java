package com.carloscaceres.tienda.bd;

import com.carloscaceres.tienda.modelo.TipoProducto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Datos de partida de la tienda: siempre los mismos, para que cada ejecución sea reproducible.
 * <p>
 * Los productos referencian fabricante, socket y categorías por su NOMBRE, no por id:
 * los ids los genera la base de datos durante la carga y se resuelven en ese momento.
 * Incluye a propósito situaciones variadas: stock bajo el mínimo, un producto sin existencias
 * y otro "bajo pedido" (sin control de stock).
 */
final class DatosIniciales {

    private DatosIniciales() {
    }

    record FabricanteSemilla(String nombre, String pais) { }

    record CategoriaSemilla(String nombre, String descripcion) { }

    record ProductoSemilla(String sku, String nombre, TipoProducto tipo, String precio, String fabricante,
                           String socket, int stock, int stockMinimo, boolean controlaStock,
                           List<String> categorias) {
        BigDecimal precioDecimal() {
            // Se construye desde String: new BigDecimal(129.9) arrastraría la imprecisión del double.
            return new BigDecimal(precio);
        }
    }

    record DireccionSemilla(String calle, String ciudad, String codigoPostal, String provincia,
                            boolean principal) { }

    record ClienteSemilla(String nombre, String apellidos, String email, String telefono,
                          List<DireccionSemilla> direcciones) { }

    static List<FabricanteSemilla> fabricantes() {
        return List.of(
                new FabricanteSemilla("AMD", "Estados Unidos"),
                new FabricanteSemilla("Intel", "Estados Unidos"),
                new FabricanteSemilla("ASUS", "Taiwán"),
                new FabricanteSemilla("MSI", "Taiwán"),
                new FabricanteSemilla("Gigabyte", "Taiwán"),
                new FabricanteSemilla("Corsair", "Estados Unidos"),
                new FabricanteSemilla("Kingston", "Estados Unidos"),
                new FabricanteSemilla("Samsung", "Corea del Sur"),
                new FabricanteSemilla("Western Digital", "Estados Unidos"),
                new FabricanteSemilla("Seasonic", "Taiwán"),
                new FabricanteSemilla("NZXT", "Estados Unidos"),
                new FabricanteSemilla("Noctua", "Austria"),
                new FabricanteSemilla("be quiet!", "Alemania"));
    }

    static List<String> sockets() {
        return List.of("AM4", "AM5", "LGA1700", "LGA1851");
    }

    static List<CategoriaSemilla> categorias() {
        return List.of(
                new CategoriaSemilla("Gaming", "Componentes orientados a juegos"),
                new CategoriaSemilla("Ofimática", "Equipos de trabajo y uso diario"),
                new CategoriaSemilla("Workstation", "Edición, renderizado y cálculo intensivo"),
                new CategoriaSemilla("Gama alta", "Máximo rendimiento"),
                new CategoriaSemilla("Gama media", "Mejor relación calidad-precio"),
                new CategoriaSemilla("Ofertas", "Productos con precio rebajado"),
                new CategoriaSemilla("Bajo pedido", "Se sirven desde el proveedor, sin stock propio"));
    }

    static List<ProductoSemilla> productos() {
        return List.of(
                // Procesadores
                new ProductoSemilla("CPU-R7-9800X3D", "AMD Ryzen 7 9800X3D", TipoProducto.CPU, "479.00",
                        "AMD", "AM5", 12, 3, true, List.of("Gaming", "Gama alta")),
                new ProductoSemilla("CPU-R5-9600X", "AMD Ryzen 5 9600X", TipoProducto.CPU, "229.00",
                        "AMD", "AM5", 20, 5, true, List.of("Gaming", "Gama media")),
                new ProductoSemilla("CPU-R5-5600", "AMD Ryzen 5 5600", TipoProducto.CPU, "99.90",
                        "AMD", "AM4", 2, 4, true, List.of("Ofimática", "Gama media", "Ofertas")),
                new ProductoSemilla("CPU-I7-14700K", "Intel Core i7-14700K", TipoProducto.CPU, "389.00",
                        "Intel", "LGA1700", 8, 3, true, List.of("Gaming", "Workstation")),
                new ProductoSemilla("CPU-U9-285K", "Intel Core Ultra 9 285K", TipoProducto.CPU, "599.00",
                        "Intel", "LGA1851", 4, 2, true, List.of("Workstation", "Gama alta")),
                // Placas base
                new ProductoSemilla("MB-X870-TUF", "ASUS TUF Gaming X870-Plus WiFi", TipoProducto.PLACA_BASE, "259.90",
                        "ASUS", "AM5", 7, 2, true, List.of("Gaming")),
                new ProductoSemilla("MB-B650-TOMA", "MSI MAG B650 Tomahawk WiFi", TipoProducto.PLACA_BASE, "199.90",
                        "MSI", "AM5", 10, 3, true, List.of("Gaming", "Gama media")),
                new ProductoSemilla("MB-B550-AORUS", "Gigabyte B550 Aorus Elite V2", TipoProducto.PLACA_BASE, "129.90",
                        "Gigabyte", "AM4", 6, 2, true, List.of("Ofimática", "Ofertas")),
                new ProductoSemilla("MB-Z790-STRIX", "ASUS ROG Strix Z790-E Gaming WiFi", TipoProducto.PLACA_BASE, "449.00",
                        "ASUS", "LGA1700", 3, 2, true, List.of("Gaming", "Gama alta")),
                new ProductoSemilla("MB-Z890-CARBON", "MSI MPG Z890 Carbon WiFi", TipoProducto.PLACA_BASE, "529.00",
                        "MSI", "LGA1851", 2, 2, true, List.of("Workstation", "Gama alta")),
                // Tarjetas gráficas
                new ProductoSemilla("GPU-RTX5070-VEN", "MSI GeForce RTX 5070 Ventus 2X", TipoProducto.GPU, "619.00",
                        "MSI", null, 9, 3, true, List.of("Gaming", "Gama media")),
                new ProductoSemilla("GPU-RTX5090-AST", "ASUS ROG Astral GeForce RTX 5090", TipoProducto.GPU, "2899.00",
                        "ASUS", null, 1, 1, true, List.of("Gama alta", "Workstation")),
                new ProductoSemilla("GPU-RX9070XT-GOC", "Gigabyte Radeon RX 9070 XT Gaming OC", TipoProducto.GPU, "739.00",
                        "Gigabyte", null, 5, 2, true, List.of("Gaming", "Gama alta")),
                // Memoria
                new ProductoSemilla("RAM-D5-32-VEN", "Corsair Vengeance DDR5 32 GB 6000 MHz", TipoProducto.RAM, "129.90",
                        "Corsair", null, 25, 5, true, List.of("Gaming", "Gama media")),
                new ProductoSemilla("RAM-D5-64-FURY", "Kingston Fury Beast DDR5 64 GB 5600 MHz", TipoProducto.RAM, "219.00",
                        "Kingston", null, 6, 2, true, List.of("Workstation")),
                new ProductoSemilla("RAM-D4-16-FURY", "Kingston Fury Beast DDR4 16 GB 3200 MHz", TipoProducto.RAM, "44.90",
                        "Kingston", null, 3, 5, true, List.of("Ofimática", "Ofertas")),
                // Almacenamiento
                new ProductoSemilla("SSD-990PRO-2TB", "Samsung 990 PRO 2 TB NVMe", TipoProducto.ALMACENAMIENTO, "179.90",
                        "Samsung", null, 15, 4, true, List.of("Gaming", "Workstation")),
                new ProductoSemilla("SSD-SN850X-1TB", "WD Black SN850X 1 TB NVMe", TipoProducto.ALMACENAMIENTO, "89.90",
                        "Western Digital", null, 18, 4, true, List.of("Gaming", "Gama media")),
                // Fuentes de alimentación
                new ProductoSemilla("PSU-RM850X", "Corsair RM850x 850 W 80 Plus Gold", TipoProducto.FUENTE, "139.90",
                        "Corsair", null, 8, 2, true, List.of("Gaming")),
                new ProductoSemilla("PSU-FOCUS-GX1000", "Seasonic Focus GX-1000 1000 W", TipoProducto.FUENTE, "189.00",
                        "Seasonic", null, 0, 0, false, List.of("Gama alta", "Bajo pedido")),
                // Cajas
                new ProductoSemilla("CASE-H6-FLOW", "NZXT H6 Flow", TipoProducto.CAJA, "109.90",
                        "NZXT", null, 6, 2, true, List.of("Gaming")),
                // Refrigeración
                new ProductoSemilla("COOL-NHD15", "Noctua NH-D15 chromax.black", TipoProducto.REFRIGERACION, "129.00",
                        "Noctua", null, 4, 2, true, List.of("Workstation", "Gama alta")),
                new ProductoSemilla("COOL-PUREROCK2", "be quiet! Pure Rock 2", TipoProducto.REFRIGERACION, "44.90",
                        "be quiet!", null, 0, 3, true, List.of("Ofimática", "Ofertas")));
    }

    static List<ClienteSemilla> clientes() {
        return List.of(
                new ClienteSemilla("Lucía", "Martín Gómez", "lucia.martin@correo.es", "600111222", List.of(
                        new DireccionSemilla("Calle de Alcalá 120, 3º B", "Madrid", "28009", "Madrid", true),
                        new DireccionSemilla("Calle Mayor 15", "Alcalá de Henares", "28801", "Madrid", false))),
                new ClienteSemilla("Javier", "Ruiz Navarro", "javier.ruiz@correo.es", "600333444", List.of(
                        new DireccionSemilla("Avenida de España 45", "Getafe", "28903", "Madrid", true))),
                new ClienteSemilla("Marta", "Sánchez López", "marta.sanchez@correo.es", "611222333", List.of(
                        new DireccionSemilla("Calle de Colón 8", "Valencia", "46004", "Valencia", true),
                        new DireccionSemilla("Paseo de la Castellana 200", "Madrid", "28046", "Madrid", false))),
                new ClienteSemilla("Diego", "Fernández Castro", "diego.fernandez@correo.es", "622444555", List.of(
                        new DireccionSemilla("Calle Sierpes 30", "Sevilla", "41004", "Sevilla", true))),
                new ClienteSemilla("Elena", "Torres Prieto", "elena.torres@correo.es", "633555666", List.of(
                        new DireccionSemilla("Carrer de Balmes 77", "Barcelona", "08008", "Barcelona", true))),
                new ClienteSemilla("Pablo", "Moreno Gil", "pablo.moreno@correo.es", null, List.of(
                        new DireccionSemilla("Paseo de la Independencia 12", "Zaragoza", "50004", "Zaragoza", true))));
    }
}
