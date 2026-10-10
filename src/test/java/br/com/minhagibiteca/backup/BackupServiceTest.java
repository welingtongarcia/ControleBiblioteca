package br.com.minhagibiteca.backup;

import br.com.minhagibiteca.backup.application.*;
import br.com.minhagibiteca.backup.domain.BackupTables;
import br.com.minhagibiteca.backup.infrastructure.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BackupServiceTest {
    final BackupRepository repository=mock(BackupRepository.class);
    final BackupArchive archive=mock(BackupArchive.class);
    final PlatformTransactionManager transactions=mock(PlatformTransactionManager.class);
    final TransactionStatus status=new SimpleTransactionStatus();
    BackupService service(){when(transactions.getTransaction(any())).thenReturn(status);return new BackupService(repository,archive,transactions);}
    @Test void exportUsesOwnerScopedReadOnlyRepeatableReadTransaction() {
        var owner=UUID.randomUUID();var data=new ObjectMapper().createObjectNode();var download=mock(BackupArchive.Download.class);
        when(repository.schemaVersion()).thenReturn("19");when(repository.snapshot(owner)).thenReturn(data);when(archive.generate(owner,"19",data)).thenReturn(download);
        assertThat(service().generate(owner)).isSameAs(download);
        var definition=ArgumentCaptor.forClass(TransactionDefinition.class);verify(transactions).getTransaction(definition.capture());
        assertThat(definition.getValue().isReadOnly()).isTrue();assertThat(definition.getValue().getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        verify(transactions).commit(status);verify(repository).snapshot(owner);verify(archive).generate(owner,"19",data);verifyNoMoreInteractions(archive);
    }
    @Test void occupiedAccountRollsBackAfterLockAndClosesPackageWithoutInserts() {
        var mapper=new ObjectMapper();var source=UUID.randomUUID();var destination=UUID.randomUUID();var data=mapper.createObjectNode();
        for(var table:BackupTables.all()){data.putArray(table);when(repository.columns(table)).thenReturn(Set.of());}
        var manifest=mapper.createObjectNode().put("formatVersion",1).put("schemaVersion","19").put("applicationVersion","0.1.0-SNAPSHOT").put("generatedAt",Instant.now().toString()).put("sourceOwner",source.toString());manifest.putObject("files");
        var pack=mock(BackupArchive.Package.class);var stream=new ByteArrayInputStream(new byte[0]);
        when(archive.read(stream)).thenReturn(pack);when(pack.manifest()).thenReturn(manifest);when(pack.data()).thenReturn(data);when(pack.files()).thenReturn(Map.of());when(repository.schemaVersion()).thenReturn("19");when(repository.empty(destination)).thenReturn(false);
        assertThatThrownBy(()->service().restore(destination,stream)).isInstanceOf(InvalidBackupException.class).hasMessageContaining("conta vazia");
        var order=inOrder(repository);order.verify(repository).schemaVersion();for(var table:BackupTables.all())order.verify(repository).columns(table);order.verify(repository).lock(destination);order.verify(repository).empty(destination);
        verify(repository,never()).insertAll(anyString(),any());verify(transactions).rollback(status);verify(pack).close();
    }
    @Test void failedTransactionCompletionAlsoRemovesGeneratedTemporaryDownload() {
        var owner=UUID.randomUUID();var data=new ObjectMapper().createObjectNode();var download=mock(BackupArchive.Download.class);
        when(repository.schemaVersion()).thenReturn("19");when(repository.snapshot(owner)).thenReturn(data);when(archive.generate(owner,"19",data)).thenReturn(download);
        doThrow(new TransactionSystemException("Falha de conexão")).when(transactions).commit(status);
        assertThatThrownBy(()->service().generate(owner)).isInstanceOf(TransactionSystemException.class);verify(download).close();
    }
}
