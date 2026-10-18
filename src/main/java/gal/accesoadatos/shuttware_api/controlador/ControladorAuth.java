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

    //Endpoint para registrar un nuevo usuario
    @PostMapping("/auth/registro")
    public Map<String, Object> registro(@RequestBody Map<String, String> datos) {
        String email = datos.get("email");
        String contrasenia = datos.get("contrasenia");
        String nombre = datos.get("nombre");

        //Validar que los datos no estén vacíos
        if (email == null || contrasenia == null || nombre == null) {
            return Map.of("ok", false, "mensaje", "Faltan datos");
        }

        //Intentar insertar el nuevo usuario en la base de datos
        try {
            jdbc.update(
                    "INSERT INTO usuario (email, contrasenia, rol, nombre, fecha_alta) VALUES (?, ?, 'cliente', ?, CURDATE())",
                    email, contrasenia, nombre);
            return Map.of("ok", true, "mensaje", "Registrado");
        } catch (Exception e) {
            return Map.of("ok", false, "mensaje", "El email ya existe");
        }
    }

    //Endpoint para iniciar sesión
    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> datos) {
        String email = datos.get("email");
        String contrasenia = datos.get("contrasenia");

        
        try {
            //Intentar obtener el usuario de la base de datos
            Map<String, Object> usuario = jdbc.queryForMap(
                    "SELECT id_usuario, nombre, rol FROM usuario WHERE email = ? AND contrasenia = ? AND rol = 'cliente'",
                    email, contrasenia);
            //Si se encuentra el usuario, devolverlo
            return Map.of("ok", true, "usuario", usuario);
        } catch (Exception e) {
            return Map.of("ok", false, "mensaje", "Credenciales incorrectas");
        }
    }
}
