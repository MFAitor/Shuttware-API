package gal.accesoadatos.shuttware_api.controlador;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
 
import java.util.List;
import java.util.Map;
 
@RestController
public class ControladorHueco {
 
    private final JdbcTemplate jdbc;
 
    public ControladorHueco(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
 
    //Huecos libres de hoy en adelante. Con desdeId solo devuelve los de id mayor (para detectar nuevos)
    @GetMapping("/huecos/libres")
    public List<Map<String, Object>> libres(@RequestParam(required = false) Integer desdeId) {
        int desde = desdeId == null ? 0 : desdeId;
        return jdbc.queryForList(
                "SELECT id_hueco, fecha, estado FROM hueco WHERE estado = 'libre' AND fecha >= CURDATE() AND id_hueco > ? ORDER BY fecha",
                desde);
    }
}