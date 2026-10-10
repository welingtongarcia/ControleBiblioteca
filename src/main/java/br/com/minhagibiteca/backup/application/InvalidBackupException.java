package br.com.minhagibiteca.backup.application;

public class InvalidBackupException extends RuntimeException {
    public InvalidBackupException(String message) { super(message); }
    public InvalidBackupException(String message, Throwable cause) { super(message, cause); }
}
