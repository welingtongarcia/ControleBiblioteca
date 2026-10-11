package br.com.minhagibiteca.auth.application;

import br.com.minhagibiteca.auth.domain.UserAccount;
import br.com.minhagibiteca.auth.domain.UserRole;
import br.com.minhagibiteca.auth.infrastructure.UserAccountRepository;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRegistrationService {
    private final UserAccountRepository users;
    private final PasswordEncoder passwords;
    private final Validator validator;
    public UserRegistrationService(UserAccountRepository users, PasswordEncoder passwords, Validator validator) {
        this.users = users; this.passwords = passwords; this.validator = validator;
    }

    @Transactional
    public void register(Command command) {
        if (command == null || !validator.validate(command).isEmpty() || !passwordWithinByteLimit(command.password()))
            throw new IllegalArgumentException("Confira o e-mail e use uma senha de 12 a 64 caracteres, com até 72 bytes.");
        if (users.findByEmail(command.email()).isPresent()) throw new RegistrationUnavailableException();
        try {
            users.saveAndFlush(UserAccount.create(command.email(), passwords.encode(command.password()), UserRole.USER));
        } catch (DataIntegrityViolationException conflict) {
            throw new RegistrationUnavailableException();
        }
    }

    public static boolean passwordWithinByteLimit(String value) {
        return value == null || value.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    // Não usar record: o toString automático poderia expor a senha em logs.
    public static final class Command {
        @NotBlank @Email @Size(max = 320) private final String email;
        @NotBlank @Size(min = 12, max = 64) private final String password;
        public Command(String email, String password) { this.email = UserAccount.normalizeEmail(email); this.password = password; }
        public String email() { return email; }
        public String password() { return password; }
    }
    public static class RegistrationUnavailableException extends RuntimeException {
        public RegistrationUnavailableException() { super("Não foi possível criar uma conta com este e-mail. Confira os dados ou tente entrar."); }
    }
}
