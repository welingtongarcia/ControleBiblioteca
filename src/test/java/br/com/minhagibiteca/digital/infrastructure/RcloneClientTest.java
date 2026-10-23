package br.com.minhagibiteca.digital.infrastructure;

import br.com.minhagibiteca.digital.domain.OneDriveException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class RcloneClientTest {
    @TempDir Path temp;
    UUID owner = UUID.randomUUID();
    RcloneCommandRunner runner = mock(RcloneCommandRunner.class);
    RcloneClient client;
    @BeforeEach void setup() { client = new RcloneClient(new RcloneSettings(true,"rclone",temp.toString()), runner); }

    @Test void validatesRemoteAndFolderWithoutReceivingOrLoggingTokens() {
        when(runner.run(eq(owner),eq(List.of("listremotes","--long")))).thenReturn("onedrive: onedrive\n");
        when(runner.run(eq(owner),eq(List.of("lsjson","onedrive:","--stat","--no-mimetype")))).thenReturn("{\"IsDir\":true}");
        client.verify(owner);
        when(runner.run(eq(owner),eq(List.of("listremotes","--long")))).thenReturn("onedrive: local\nother: onedrive\n");
        assertThatThrownBy(()->client.verify(owner)).isInstanceOf(OneDriveException.class).hasMessageContaining("tipo OneDrive");
    }

    @Test void rejectsTraversalRemoteOverridesAndControlCharactersBeforeExecution() {
        for (String path : List.of("/etc", "r:/etc", "r:../x", "r:a/../b", "r:a//b", "r:a\\b", "r::local:x", "r:a\nb", "r:a/", "r:" + "a".repeat(255)))
            assertThatThrownBy(()->client.children(owner,path)).isInstanceOf(OneDriveException.class);
        verifyNoInteractions(runner);
    }

    @Test void parsesMetadataAndKeepsShellCharactersAsDataAndStableIdsAcrossMoves() {
        String json="[{\"ID\":\"stable\",\"Name\":\"HQ $(touch nope).PDF\",\"IsDir\":false,\"Size\":123,\"ModTime\":\"2026-10-23T12:00:00Z\"},{\"Name\":\"Livros\",\"IsDir\":true,\"Size\":-1}]";
        when(runner.run(eq(owner),anyList())).thenReturn(json);
        var a=client.children(owner,"root");
        assertThat(a.items()).hasSize(2);
        assertThat(a.items().getFirst().id()).isEqualTo("r:Livros");
        var file=a.items().getLast();
        assertThat(file.size()).isEqualTo(123);
        assertThat(file.modified()).isEqualTo(Instant.parse("2026-10-23T12:00:00Z"));
        assertThat(file.webUrl()).isNull();
        assertThat(client.children(owner,"r:Outra pasta").items().getLast().id()).isEqualTo(file.id());
        verify(runner).run(owner,List.of("lsjson","onedrive:Outra pasta","--no-mimetype"));
    }

    @Test void paginatesBrowserButReturnsFullListingToSynchronization() {
        String json="[" + String.join(",",Collections.nCopies(201,"{\"Name\":\"HQ.pdf\",\"IsDir\":false,\"Size\":1}")) + "]";
        when(runner.run(eq(owner),anyList())).thenReturn(json);
        assertThat(client.children(owner,"root").items()).hasSize(201);
        assertThat(client.browse(owner,"root",null).items()).hasSize(100);
        assertThat(client.browse(owner,"root","100").next()).isEqualTo("200");
        assertThat(client.browse(owner,"root","200").items()).hasSize(1);
        assertThatThrownBy(()->client.browse(owner,"root","-1")).isInstanceOf(OneDriveException.class);
    }

    @Test void rejectsMalformedOversizedAndInvalidMetadataWithoutExposingResponse() {
        for (String json:List.of("synthetic-secret", "null", "{}", "[{\"Name\":\"../bad\",\"Size\":1}]", "[{\"Name\":\"HQ.pdf\",\"Size\":-1}]", "[{\"Name\":\"HQ.pdf\",\"Size\":1,\"ModTime\":\"invalid\"}]", "["+String.join(",",Collections.nCopies(10001,"{}"))+"]")) {
            when(runner.run(eq(owner),anyList())).thenReturn(json);
            assertThatThrownBy(()->client.children(owner,"root")).isInstanceOf(OneDriveException.class).hasMessageNotContaining("synthetic-secret");
        }
    }

    @Test void separatesConfigurationFilesAndRejectsSymlinksAndDisabledMode() throws Exception {
        var settings=new RcloneSettings(true,"rclone",temp.toString());
        Files.writeString(temp.resolve(owner+".conf"),"synthetic");
        assertThat(settings.config(owner)).isEqualTo(temp.resolve(owner+".conf"));
        UUID other=UUID.randomUUID();
        assertThatThrownBy(()->settings.config(other)).isInstanceOf(OneDriveException.class);
        if (!System.getProperty("os.name").startsWith("Windows")) {
            Files.createSymbolicLink(temp.resolve(other+".conf"),temp.resolve(owner+".conf"));
            assertThatThrownBy(()->settings.config(other)).isInstanceOf(OneDriveException.class);
        }
        assertThatThrownBy(()->new RcloneSettings(false,"rclone",temp.toString()).config(owner)).isInstanceOf(OneDriveException.class);
    }

    @Test void executesRealSubprocessWithFixedFlagsNoShellAndClosedStdin() throws Exception {
        Files.writeString(temp.resolve(owner+".conf"),"synthetic");
        var real=fixtureRunner("args");
        String output=real.run(owner,List.of("lsjson","onedrive:HQ $(touch nope)","--no-mimetype"));
        assertThat(output).contains("onedrive:HQ $(touch nope)"+System.lineSeparator(),"--dry-run","Files.Read offline_access","--onedrive-disable-site-permission=false",temp.resolve(owner+".conf").toString());
        assertThatThrownBy(()->real.run(owner,List.of("delete","onedrive:"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void sanitizesSubprocessErrorsAndEnforcesOutputLimit() throws Exception {
        Files.writeString(temp.resolve(owner+".conf"),"synthetic");
        var real=fixtureRunner("error");
        assertThatThrownBy(()->real.run(owner,List.of("lsjson","onedrive:"))).isInstanceOf(OneDriveException.class).hasMessageNotContaining("synthetic-secret");
        var large=fixtureRunner("large");
        assertThatThrownBy(()->large.run(owner,List.of("lsjson","onedrive:"))).isInstanceOf(OneDriveException.class);
    }

    @Test void terminatesAnUnresponsiveSubprocess() throws Exception {
        Files.writeString(temp.resolve(owner+".conf"),"synthetic");
        var real=fixtureRunner("slow");
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(25),()->
                assertThatThrownBy(()->real.run(owner,List.of("lsjson","onedrive:"))).isInstanceOf(OneDriveException.class));
    }

    private RcloneCommandRunner fixtureRunner(String mode) {
        return new RcloneCommandRunner(new RcloneSettings(true,"synthetic-rclone",temp.toString())) {
            @Override ProcessBuilder processBuilder(List<String> args) {
                var command=new ArrayList<String>(List.of(Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString(),"-cp",System.getProperty("java.class.path"),Fixture.class.getName(),mode));
                command.addAll(args);
                var builder=new ProcessBuilder(command);
                builder.environment().put("RCLONE_CONFIG_ONEDRIVE_TYPE","synthetic-override");
                return builder;
            }
        };
    }

    public static class Fixture {
        public static void main(String[] args) throws Exception {
            if (System.in.read()!=-1) System.exit(9);
            switch(args[0]) {
                case "args" -> {
                    if(System.getenv().keySet().stream().anyMatch(k->k.toUpperCase(Locale.ROOT).startsWith("RCLONE_")))System.exit(8);
                    for(int i=1;i<args.length;i++)System.out.println(args[i]);
                }
                case "error" -> { System.err.println("synthetic-secret");System.exit(1); }
                case "large" -> System.out.write(new byte[16*1024*1024+1]);
                case "slow" -> Thread.sleep(30_000);
                default -> System.exit(7);
            }
        }
    }
}
