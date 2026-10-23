package br.com.minhagibiteca.digital.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.springframework.stereotype.Component;

@Component
public class OneDriveTokenCipher {
    private final OneDriveSettings settings;
    public OneDriveTokenCipher(OneDriveSettings settings){this.settings=settings;}
    public String encrypt(UUID owner,String value){
        try{byte[] iv=new byte[12];new SecureRandom().nextBytes(iv);Cipher c=cipher(Cipher.ENCRYPT_MODE,owner,iv);byte[] encrypted=c.doFinal(value.getBytes(StandardCharsets.UTF_8));byte[] all=new byte[12+encrypted.length];System.arraycopy(iv,0,all,0,12);System.arraycopy(encrypted,0,all,12,encrypted.length);return Base64.getEncoder().encodeToString(all);}
        catch(Exception e){throw failure();}
    }
    public String decrypt(UUID owner,String value){try{byte[] all=Base64.getDecoder().decode(value);if(all.length<29)throw new IllegalArgumentException();return new String(cipher(Cipher.DECRYPT_MODE,owner,Arrays.copyOf(all,12)).doFinal(Arrays.copyOfRange(all,12,all.length)),StandardCharsets.UTF_8);}catch(Exception e){throw failure();}}
    private Cipher cipher(int mode,UUID owner,byte[] iv)throws Exception{Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(mode,new SecretKeySpec(Base64.getDecoder().decode(settings.key()),"AES"),new GCMParameterSpec(128,iv));c.updateAAD(("onedrive:"+owner).getBytes(StandardCharsets.UTF_8));return c;}
    private br.com.minhagibiteca.digital.domain.OneDriveException failure(){return new br.com.minhagibiteca.digital.domain.OneDriveException("Não foi possível acessar as credenciais protegidas do OneDrive. Confira a chave da instalação e reconecte a conta.");}
}
