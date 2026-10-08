package br.com.minhagibiteca.auth.application;

import br.com.minhagibiteca.auth.domain.UserAccount;
import br.com.minhagibiteca.auth.infrastructure.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public DatabaseUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        String normalizedEmail = UserAccount.normalizeEmail(username);
        return userAccountRepository.findByEmail(normalizedEmail)
                .map(UserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
    }
}
