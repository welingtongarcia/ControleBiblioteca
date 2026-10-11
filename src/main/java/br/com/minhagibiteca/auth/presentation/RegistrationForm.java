package br.com.minhagibiteca.auth.presentation;

import br.com.minhagibiteca.auth.application.UserRegistrationService;
import br.com.minhagibiteca.auth.domain.UserAccount;
import jakarta.validation.constraints.*;

public class RegistrationForm {
    @NotBlank(message = "Informe seu e-mail.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 320, message = "O e-mail deve ter no máximo 320 caracteres.")
    private String email;
    @NotBlank(message = "Informe uma senha.")
    @Size(min = 12, max = 64, message = "A senha deve ter de 12 a 64 caracteres.")
    private String password;
    @NotBlank(message = "Confirme sua senha.")
    @Size(max = 64, message = "A confirmação deve ter no máximo 64 caracteres.")
    private String passwordConfirmation;
    @AssertTrue(message = "As senhas devem ser iguais.")
    public boolean isPasswordsMatching() { return password == null || passwordConfirmation == null || password.equals(passwordConfirmation); }
    @AssertTrue(message = "A senha deve ter até 72 bytes. Caracteres acentuados e emojis podem ocupar mais de um byte.")
    public boolean isPasswordWithinByteLimit() { return UserRegistrationService.passwordWithinByteLimit(password); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = UserAccount.normalizeEmail(email); }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPasswordConfirmation() { return passwordConfirmation; }
    public void setPasswordConfirmation(String value) { this.passwordConfirmation = value; }
    public void clearPasswords() { password = null; passwordConfirmation = null; }
}
