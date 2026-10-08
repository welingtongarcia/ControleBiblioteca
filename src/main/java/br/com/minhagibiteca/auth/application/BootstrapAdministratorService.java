package br.com.minhagibiteca.auth.application;

import br.com.minhagibiteca.auth.domain.UserAccount;
import br.com.minhagibiteca.auth.domain.UserRole;
import br.com.minhagibiteca.auth.infrastructure.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BootstrapAdministratorService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapAdministratorService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public boolean createFirstAdministrator(String email, String rawPassword) {
        if (userAccountRepository.count() > 0) {
            return false;
        }
        if (UserAccount.normalizeEmail(email).isBlank() || rawPassword == null || rawPassword.length() < 12) {
            throw new IllegalStateException("O bootstrap requer e-mail e senha com ao menos 12 caracteres.");
        }

        userAccountRepository.save(UserAccount.create(email, passwordEncoder.encode(rawPassword), UserRole.ADMIN));
        return true;
    }
}
