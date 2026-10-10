package br.com.minhagibiteca.backup.infrastructure;

import br.com.minhagibiteca.backup.application.InvalidBackupException;
import java.io.*;
import org.junit.jupiter.api.Test;
import java.util.zip.*;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

class BackupArchiveTest {
    @Test void boundedCopyRejectsExpansionInsteadOfTruncating() throws Exception {
        var out=new ByteArrayOutputStream();assertThat(BackupArchive.boundedCopy(new ByteArrayInputStream(new byte[8]),out,8)).isEqualTo(8);
        assertThatThrownBy(()->BackupArchive.boundedCopy(new ByteArrayInputStream(new byte[9]),new ByteArrayOutputStream(),8)).isInstanceOf(InvalidBackupException.class).hasMessageContaining("limite");
    }
    @Test void coverNamesCannotBePathsOrExecutableFiles() {
        String name="b6d7ceab-cf28-4f47-8390-faefb1d839da-54f6b7ae-b99d-4d81-ac3e-6f1b6aefcddd.png";
        assertThat(BackupArchive.coverName(name)).isTrue();assertThat(BackupArchive.coverName("../"+name)).isFalse();assertThat(BackupArchive.coverName(name+".exe")).isFalse();assertThat(BackupArchive.coverName("C:\\capas\\"+name)).isFalse();
    }
    @Test void compressedBombAndDuplicateZipEntriesAreRejected() throws Exception {
        var out=new ByteArrayOutputStream();
        try(var zip=new ZipOutputStream(out)){zip.putNextEntry(new ZipEntry("dados.json"));byte[] zeros=new byte[8192];for(int i=0;i<3201;i++)zip.write(zeros);zip.closeEntry();}
        var archive=new BackupArchive("target/unused-covers");
        assertThat(out.size()).isLessThan(100_000);
        assertThatThrownBy(()->archive.read(new ByteArrayInputStream(out.toByteArray()))).isInstanceOf(InvalidBackupException.class).hasMessageContaining("limite");
        out.reset();try(var zip=new ZipOutputStream(out)){for(var name:new String[]{"dados.json","outro.json"}){zip.putNextEntry(new ZipEntry(name));zip.write("{}".getBytes(StandardCharsets.UTF_8));zip.closeEntry();}}
        byte[] bytes=out.toByteArray(),from="outro.json".getBytes(StandardCharsets.UTF_8),to="dados.json".getBytes(StandardCharsets.UTF_8);
        for(int i=0;i<=bytes.length-from.length;i++){boolean match=true;for(int j=0;j<from.length;j++)if(bytes[i+j]!=from[j])match=false;if(match)System.arraycopy(to,0,bytes,i,to.length);}
        assertThatThrownBy(()->archive.read(new ByteArrayInputStream(bytes))).isInstanceOf(InvalidBackupException.class).hasMessageContaining("repetidos");
    }
}
