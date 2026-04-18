package com.nexolab.listener;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.nexolab.dao.UserDAO;
import com.nexolab.model.RolSistema;
import com.nexolab.model.Sector;
import com.nexolab.model.TipoEstado;
import com.nexolab.model.Usuario;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.Date;
import java.util.logging.Logger;

@WebListener
public class AdminInitializer implements ServletContextListener {

    private static final Logger log = Logger.getLogger(AdminInitializer.class.getName());

    private static final String ADMIN_EMAIL    = System.getenv().getOrDefault("ADMIN_EMAIL",    "admin@nexolab.com");
    private static final String ADMIN_PASSWORD = System.getenv().getOrDefault("ADMIN_PASSWORD", "Admin@NexoLab1");
    private static final String ADMIN_NOMBRE   = "Administrador";
    private static final String ADMIN_APELLIDO = "Sistema";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        UserDAO userDAO = new UserDAO();

        if (userDAO.findAdminSistema() != null) {
            log.info("AdminInitializer: admin del sistema ya existe, se omite la creación.");
            return;
        }

        Usuario admin = new Usuario();
        admin.setNombre(ADMIN_NOMBRE);
        admin.setApellido(ADMIN_APELLIDO);
        admin.setEmail(ADMIN_EMAIL);
        admin.setPasswordHash(BCrypt.withDefaults().hashToString(12, ADMIN_PASSWORD.toCharArray()));
        admin.setPasswordSalt("BCRYPT");
        admin.setCargo("Administrador del Sistema");
        admin.setSector(Sector.SISTEMAS);
        admin.setTipoEstado(TipoEstado.DESCONECTADO);
        admin.setRolSistema(RolSistema.ADMIN_SISTEMA);
        admin.setFechaCreacion(new Date());

        userDAO.save(admin);
        log.info("AdminInitializer: admin del sistema creado con email " + ADMIN_EMAIL);
    }
}
