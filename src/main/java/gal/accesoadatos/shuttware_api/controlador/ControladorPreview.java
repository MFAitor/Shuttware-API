package gal.accesoadatos.shuttware_api.controlador;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.io.File;

import java.util.List;
import java.util.Map;

@RestController
public class ControladorPreview {

    private final JdbcTemplate jdbc;

    public ControladorPreview(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Endpoint para ver las sesiones de un cliente que tienen previews
    @GetMapping("/sesiones/con-previews")
    public List<Map<String, Object>> sesionesConPreviews(@RequestParam int idUsuario) {
        return jdbc.queryForList(
                "SELECT s.id_sesion, h.fecha, s.tipo, s.estado, (SELECT COUNT(*) FROM preview p WHERE p.id_sesion = s.id_sesion) AS num_previews FROM sesion s JOIN hueco h ON s.id_hueco = h.id_hueco WHERE s.id_usuario = ? AND EXISTS (SELECT 1 FROM preview p WHERE p.id_sesion = s.id_sesion) ORDER BY h.fecha DESC",
                idUsuario);
    }

    // Endpoint para ver las previews de una sesión de un cliente
    @GetMapping("/sesiones/{id}/previews")
    public Map<String, Object> listar(@PathVariable int id, @RequestParam int idUsuario) {
        // Comprobar que la sesión existe y es de este cliente
        Integer propia = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sesion WHERE id_sesion = ? AND id_usuario = ?",
                Integer.class, id, idUsuario);
        if (propia == null || propia == 0) {
            return Map.of("ok", false, "mensaje", "La sesion no existe");
        }

        // Devolver las previews ordenadas
        List<Map<String, Object>> previews = jdbc.queryForList(
                "SELECT id_preview, ruta, pie, orden FROM preview WHERE id_sesion = ? ORDER BY orden",
                id);
        return Map.of("ok", true, "previews", previews);
    }

    // Devuelve la imagen de una preview
    @GetMapping("/previews/imagen/{id}")
    public ResponseEntity<Resource> verImagen(@PathVariable int id) {
        Map<String, Object> preview = jdbc.queryForMap(
                "SELECT ruta FROM preview WHERE id_preview = ?", id);

        String ruta = (String) preview.get("ruta");
        File file = new File(ruta);

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }
}