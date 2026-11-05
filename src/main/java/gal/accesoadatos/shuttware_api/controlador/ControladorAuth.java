package gal.accesoadatos.shuttware_api.controlador;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ControladorAuth {

    private final JdbcTemplate jdbc;

    public ControladorAuth(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping("/auth/registro")
    public Map<String, Object> registro(@RequestBody Map<String, String> datos) {
        String email = datos.get("email");
        String contrasenia = datos.get("contrasenia");
        String nombre = datos.get("nombre");
        String apellidos = datos.get("apellidos");
        String telefono = datos.get("telefono");
        String instagram = datos.get("url_instagram");

        // Validar campos obligatorios
        if (email == null || contrasenia == null || nombre == null || nombre.isBlank()) {
            return Map.of("ok", false, "mensaje", "Faltan datos obligatorios");
        }

        try {
            jdbc.update(
                    "INSERT INTO usuario (email, contrasenia, rol, nombre, apellidos, telefono, url_instagram, fecha_alta) "
                            +
                            "VALUES (?, ?, 'cliente', ?, ?, ?, ?, CURDATE())",
                    email,
                    contrasenia,
                    nombre,
                    apellidos, // puede ser null
                    telefono != null && !telefono.isBlank() ? Integer.parseInt(telefono.replaceAll("[^0-9]", ""))
                            : null,
                    instagram);
            return Map.of("ok", true, "mensaje", "Registrado");
        } catch (Exception e) {
            return Map.of("ok", false, "mensaje", "El email ya existe");
        }
    }

    // Endpoint para iniciar sesión
    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> datos) {
        String email = datos.get("email");
        String contrasenia = datos.get("contrasenia");

        try {
            // Intentar obtener el usuario de la base de datos
            Map<String, Object> usuario = jdbc.queryForMap(
                    "SELECT id_usuario, nombre, rol FROM usuario WHERE email = ? AND contrasenia = ? AND rol = 'cliente'",
                    email, contrasenia);
            // Si se encuentra el usuario, devolverlo
            return Map.of("ok", true, "usuario", usuario);
        } catch (Exception e) {
            return Map.of("ok", false, "mensaje", "Credenciales incorrectas");
        }
    }
}
