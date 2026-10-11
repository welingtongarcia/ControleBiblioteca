package br.com.minhagibiteca.auth;

import br.com.minhagibiteca.auth.application.UserRegistrationService;
import br.com.minhagibiteca.auth.domain.UserAccount;
import br.com.minhagibiteca.auth.domain.UserRole;
import br.com.minhagibiteca.auth.infrastructure.UserAccountRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserRegistrationServiceTest {
    ValidatorFactory factory;
    UserAccountRepository users;
    PasswordEncoder encoder;
    UserRegistrationService service;
    @BeforeEach void setup() {
        factory=Validation.buildDefaultValidatorFactory();users=mock(UserAccountRepository.class);encoder=mock(PasswordEncoder.class);
        service=new UserRegistrationService(users,encoder,factory.getValidator());
    }
    @AfterEach void close() { factory.close(); }

    @Test void createsOnlyEnabledOrdinaryAccountsWithNormalizedEmailAndHash() {
        when(encoder.encode("senha-exclusiva-123")).thenReturn("HASH");
        service.register(new UserRegistrationService.Command("  NOVA@EXAMPLE.COM ","senha-exclusiva-123"));
        var saved=org.mockito.ArgumentCaptor.forClass(UserAccount.class);
        verify(users).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("nova@example.com");
        assertThat(saved.getValue().getRole()).isEqualTo(UserRole.USER);
        assertThat(saved.getValue().isEnabled()).isTrue();
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("HASH");
    }
    @Test void rejectsInvalidInputsBeforeHashingOrSaving() {
        for(var command:new UserRegistrationService.Command[]{null,
                new UserRegistrationService.Command("inválido","senha-exclusiva-123"),
                new UserRegistrationService.Command("a".repeat(321)+"@example.com","senha-exclusiva-123"),
                new UserRegistrationService.Command("nova@example.com",null),
                new UserRegistrationService.Command("nova@example.com","curta"),
                new UserRegistrationService.Command("nova@example.com"," ".repeat(12)),
                new UserRegistrationService.Command("nova@example.com","a".repeat(65)),
                new UserRegistrationService.Command("nova@example.com","é".repeat(37))}) {
            assertThatThrownBy(()->service.register(command)).isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(users,encoder);
    }
    @Test void neverOverwritesExistingAccountsAndHandlesUniqueConstraintRaces() {
        var command=new UserRegistrationService.Command("nova@example.com","senha-exclusiva-123");
        when(users.findByEmail(command.email())).thenReturn(Optional.of(UserAccount.create(command.email(),"OLD_HASH",UserRole.ADMIN)));
        assertThatThrownBy(()->service.register(command)).isInstanceOf(UserRegistrationService.RegistrationUnavailableException.class);
        verifyNoInteractions(encoder);verify(users,never()).saveAndFlush(any());
        when(users.findByEmail(command.email())).thenReturn(Optional.empty());
        when(encoder.encode(command.password())).thenReturn("HASH");
        when(users.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique"));
        assertThatThrownBy(()->service.register(command)).isInstanceOf(UserRegistrationService.RegistrationUnavailableException.class)
                .hasMessageNotContaining(command.password());
    }
}
