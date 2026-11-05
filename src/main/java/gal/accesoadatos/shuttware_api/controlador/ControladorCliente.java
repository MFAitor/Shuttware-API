package gal.accesoadatos.shuttware_api.controlador;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ControladorCliente {

    private final JdbcTemplate jdbc;

    public ControladorCliente(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Endpoint para ver los datos de un cliente
    @GetMapping("/clientes/{id}")
    public Map<String, Object> ver(@PathVariable int id) {
        try {
            // No se devuelve la contraseña ni la nota interna
            Map<String, Object> cliente = jdbc.queryForMap(
                    "SELECT id_usuario, nombre, apellidos, email, telefono, url_instagram AS instagram, fecha_alta FROM usuario WHERE id_usuario = ? AND rol = 'cliente'",
                    id);
            return Map.of("ok", true, "cliente", cliente);
        } catch (EmptyResultDataAccessException e) {
            return Map.of("ok", false, "mensaje", "El cliente no existe");
        }
    }

    // Endpoint para modificar los datos de un cliente (solo los campos enviados)
    @PutMapping("/clientes/{id}")
    public Map<String, Object> modificar(@PathVariable int id, @RequestBody Map<String, String> datos) {
        String nombre = datos.get("nombre");
        String apellidos = datos.get("apellidos");
        String email = datos.get("email");
        String telefono = datos.get("telefono");
        String instagram = datos.get("instagram");

        // Si vienen, nombre y correo no pueden estar vacíos
        if ((nombre != null && nombre.isBlank()) || (email != null && email.isBlank())) {
            return Map.of("ok", false, "mensaje", "Nombre y correo no pueden quedar vacios");
        }

        // Validar que el teléfono sea numérico
        if (telefono != null && !telefono.isBlank() && !telefono.matches("\\d+")) {
            return Map.of("ok", false, "mensaje", "El telefono tiene que ser numerico");
        }

        // COALESCE mantiene el valor actual cuando el campo no se envía
        try {
            int filas = jdbc.update(
                    "UPDATE usuario SET nombre = COALESCE(?, nombre), apellidos = COALESCE(?, apellidos), email = COALESCE(?, email), telefono = COALESCE(?, telefono), url_instagram = COALESCE(?, url_instagram) WHERE id_usuario = ? AND rol = 'cliente'",
                    nombre, apellidos, email, telefono, instagram, id);
            if (filas == 0) {
                return Map.of("ok", false, "mensaje", "El cliente no existe");
            }
            return Map.of("ok", true, "mensaje", "Datos modificados");
        } catch (DuplicateKeyException e) {
            return Map.of("ok", false, "mensaje", "No se pudo modificar. El correo no puede estar repetido");
        }
    }

    //Endpoint que compara la contrasenia vieja con la que marca el usuario para poder cambiarla
    @PutMapping("/clientes/{id}/contrasena")
    public Map<String, Object> cambiarContrasena(@PathVariable int id, @RequestBody Map<String, String> datos) {
        String actual = datos.get("actual");
        String nueva = datos.get("nueva");

        if (actual == null || nueva == null || nueva.isBlank()) {
            return Map.of("ok", false, "mensaje", "Faltan datos");
        }

        try {
            // Comprobar que la contraseña actual es correcta
            Map<String, Object> usuario = jdbc.queryForMap(
                    "SELECT id_usuario FROM usuario WHERE id_usuario = ? AND contrasenia = ? AND rol = 'cliente'",
                    id, actual);

            // Actualizar
            jdbc.update(
                    "UPDATE usuario SET contrasenia = ? WHERE id_usuario = ?",
                    nueva, id);

            return Map.of("ok", true, "mensaje", "Contraseña cambiada");
        } catch (EmptyResultDataAccessException e) {
            return Map.of("ok", false, "mensaje", "La contraseña vieja no es correcta");
        } catch (Exception e) {
            return Map.of("ok", false, "mensaje", "Error al cambiar la contraseña");
        }
    }
}