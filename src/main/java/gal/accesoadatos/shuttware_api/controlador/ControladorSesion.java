package gal.accesoadatos.shuttware_api.controlador;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
 
import java.util.List;
import java.util.Map;
import java.util.Set;
 
@RestController
public class ControladorSesion {
 
    private final JdbcTemplate jdbc;
 
    public ControladorSesion(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
 
    //Endpoint para solicitar una sesión en un hueco libre
    //Si algo falla después de reservar el hueco, la transacción se deshace sola
    @PostMapping("/sesiones")
    @Transactional
    public Map<String, Object> solicitar(@RequestBody Map<String, Object> datos) {
        Object idUsuario = datos.get("idUsuario");
        Object idHueco = datos.get("idHueco");
        Object tipo = datos.get("tipo");
        Object lugar = datos.get("lugar");
        Object nota = datos.get("nota");
 
        //Validar que los datos obligatorios no estén vacíos
        if (idUsuario == null || idHueco == null || tipo == null) {
            return Map.of("ok", false, "mensaje", "Faltan datos");
        }
 
        //Pasar el hueco de libre a solicitado. Si ya no está libre, no se modifica ninguna fila
        int filas = jdbc.update(
                "UPDATE hueco SET estado = 'solicitado' WHERE id_hueco = ? AND estado = 'libre'",
                idHueco);
        if (filas == 0) {
            return Map.of("ok", false, "mensaje", "El hueco ya no esta disponible");
        }
 
        //Crear la sesión pendiente de confirmación
        jdbc.update(
                "INSERT INTO sesion (id_usuario, id_hueco, tipo, lugar, nota, estado) VALUES (?, ?, ?, ?, ?, 'pendiente')",
                idUsuario, idHueco, tipo, lugar, nota);
        return Map.of("ok", true, "mensaje", "Sesion solicitada");
    }
 
    //Endpoint para ver la lista de sesiones de un cliente
    @GetMapping("/sesiones")
    public List<Map<String, Object>> listar(@RequestParam int idUsuario) {
        return jdbc.queryForList(
                "SELECT s.id_sesion, h.fecha, s.tipo, s.estado FROM sesion s JOIN hueco h ON s.id_hueco = h.id_hueco WHERE s.id_usuario = ? ORDER BY h.fecha DESC",
                idUsuario);
    }
 
    //Endpoint para ver el detalle de una sesión de un cliente
    @GetMapping("/sesiones/{id}")
    public Map<String, Object> detalle(@PathVariable int id, @RequestParam int idUsuario) {
        try {
            //Se filtra también por cliente para que nadie vea sesiones de otro
            Map<String, Object> sesion = jdbc.queryForMap(
                    "SELECT s.id_sesion, h.fecha, s.tipo, s.lugar, s.nota, s.estado, s.precio_estimado, s.tipo_entrega, s.total_fotos, s.fotos_editadas FROM sesion s JOIN hueco h ON s.id_hueco = h.id_hueco WHERE s.id_sesion = ? AND s.id_usuario = ?",
                    id, idUsuario);
            return Map.of("ok", true, "sesion", sesion);
        } catch (EmptyResultDataAccessException e) {
            return Map.of("ok", false, "mensaje", "La sesion no existe");
        }
    }
 
    //Valores permitidos para el tipo de entrega
    private static final Set<String> TIPOS_ENTREGA = Set.of("link_descarga", "USB", "album", "caja_personalizada");
 
    //Estados en los que ya no tiene sentido modificar la sesión
    private static final Set<String> ESTADOS_CERRADOS = Set.of("entregada", "cancelada", "rechazada");
 
    //Endpoint para modificar el tipo de entrega y la nota de una sesión de un cliente
    @PutMapping("/sesiones/{id}")
    public Map<String, Object> modificar(@PathVariable int id, @RequestBody Map<String, Object> datos) {
        Object idUsuario = datos.get("idUsuario");
        String tipoEntrega = datos.get("tipoEntrega") == null ? null : datos.get("tipoEntrega").toString();
        String nota = datos.get("nota") == null ? null : datos.get("nota").toString();
 
        //Hace falta el cliente y al menos un campo a modificar
        if (idUsuario == null || (tipoEntrega == null && nota == null)) {
            return Map.of("ok", false, "mensaje", "Faltan datos");
        }
 
        //Validar el tipo de entrega
        if (tipoEntrega != null && !TIPOS_ENTREGA.contains(tipoEntrega)) {
            return Map.of("ok", false, "mensaje", "Tipo de entrega no valido");
        }
 
        //Validar la longitud de la nota (la columna admite 255 caracteres)
        if (nota != null && nota.length() > 255) {
            return Map.of("ok", false, "mensaje", "La nota no puede superar los 255 caracteres");
        }
 
        //Comprobar que la sesión es de este cliente y obtener su estado
        String estado;
        try {
            estado = jdbc.queryForObject(
                    "SELECT estado FROM sesion WHERE id_sesion = ? AND id_usuario = ?",
                    String.class, id, idUsuario);
        } catch (EmptyResultDataAccessException e) {
            return Map.of("ok", false, "mensaje", "La sesion no existe");
        }
 
        //No se puede modificar una sesión cerrada
        if (estado != null && ESTADOS_CERRADOS.contains(estado)) {
            return Map.of("ok", false, "mensaje", "No se puede modificar una sesion " + estado);
        }
 
        //COALESCE mantiene el valor actual cuando el campo no se envía
        jdbc.update(
                "UPDATE sesion SET tipo_entrega = COALESCE(?, tipo_entrega), nota = COALESCE(?, nota) WHERE id_sesion = ? AND id_usuario = ?",
                tipoEntrega, nota, id, idUsuario);
        return Map.of("ok", true, "mensaje", "Sesion modificada");
    }
}