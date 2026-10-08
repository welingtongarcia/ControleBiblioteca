package br.com.minhagibiteca.auth.infrastructure;

import br.com.minhagibiteca.auth.application.BootstrapAdministratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BootstrapAdministratorConfiguration.BootstrapAdministratorProperties.class)
public class BootstrapAdministratorConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapAdministratorConfiguration.class);

    @Bean
    ApplicationRunner bootstrapAdministrator(
            BootstrapAdministratorProperties properties,
            BootstrapAdministratorService bootstrapAdministratorService,
            UserAccountRepository userAccountRepository) {
        return arguments -> {
            if (userAccountRepository.count() > 0) {
                return;
            }
            if (properties.email().isBlank() && properties.password().isBlank()) {
                LOGGER.warn("Nenhum usuário foi criado: configure BOOTSTRAP_ADMIN_EMAIL e BOOTSTRAP_ADMIN_PASSWORD.");
                return;
            }
            boolean created = bootstrapAdministratorService.createFirstAdministrator(properties.email(), properties.password());
            if (created) {
                LOGGER.info("Usuário administrador inicial criado.");
            }
        };
    }

    @ConfigurationProperties(prefix = "app.bootstrap-admin")
    public record BootstrapAdministratorProperties(String email, String password) {
        public BootstrapAdministratorProperties {
            email = email == null ? "" : email;
            password = password == null ? "" : password;
        }
    }
}
