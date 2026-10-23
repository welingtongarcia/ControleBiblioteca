package br.com.minhagibiteca.digital.domain;

/** Mensagem segura para o usuário: nunca carrega respostas de token ou cabeçalhos. */
public class OneDriveException extends RuntimeException {
    public OneDriveException(String message){super(message);}
}
