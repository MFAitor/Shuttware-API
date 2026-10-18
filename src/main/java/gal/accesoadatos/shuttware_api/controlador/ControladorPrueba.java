package gal.accesoadatos.shuttware_api.controlador;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@RestController
public class ControladorPrueba {

    private final DataSource dataSource;

    public ControladorPrueba(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/probar")
    public String probar() {
        try (Connection conn = dataSource.getConnection()) {
            return "Conectado a shuttware. Base de datos: " + conn.getCatalog();
        } catch (Exception e) {
            return "Error de conexión: " + e.getMessage();
        }
    }
}