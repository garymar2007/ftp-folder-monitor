import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import com.gary.SftpClient;
import com.jcraft.jsch.*;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

import static org.assertj.core.api.Assertions.assertThat;

public class SftpFileTransferLiveTest {

    private final String SFTPUSER = "user";
    private final String SFTPHOST = "localhost";

    private SftpClient sftpClient;

    private final String DOWNLOAD_FOLDER="C:\\garymar2007\\ftp-folder-monitor\\download\\";


    @Before
    public void setup() throws URISyntaxException, IOException, SftpException {
        sftpClient = new SftpClient(SFTPUSER, "123456", SFTPHOST);

        prepareFiles();
    }

    private void prepareFiles() throws URISyntaxException, IOException, SftpException {
        uploadFile("feedfile1.txt");
        uploadFile("feedfile2.txt");
    }


    private void uploadFile(String fileName) throws URISyntaxException, IOException, SftpException {
        sftpClient.uploadFile(fileName);
        System.out.println("File transfered successfully to SFTP server");
    }

    @After
    public void teardown() throws SftpException {
    //    sftpClient.cleanupFolder();
        sftpClient.disconnect();
    //    cleanUpFolder();
    }

    private void cleanUpFolder() {
        Arrays.stream(Objects.requireNonNull(new File(DOWNLOAD_FOLDER)
                .listFiles())).forEach(File::delete);
    }

    @Test
    public void testSimulateScheduledPolling() throws JSchException, SftpException, IOException, URISyntaxException, InterruptedException {
        int loop = 5;

        while(loop > 0) {
            loop--;
            Long lastDownloadTimeStamp = resetLastDownloadTimeStamp(loop);

            List<ChannelSftp.LsEntry> filesToBeDownloaded = sftpClient.getLatestFeedfiles(lastDownloadTimeStamp);
            if(filesToBeDownloaded.isEmpty()) {
                System.out.println("There is no new feed file in FTP server folder");
            } else {
                for(ChannelSftp.LsEntry file : filesToBeDownloaded) {
                    sftpClient.downloadFile("/data/" + file.getFilename(), "download/" + file.getFilename());
                }
            }

            verifyResult(loop, filesToBeDownloaded);

            Thread.sleep(5000);
        }
    }

    private Long resetLastDownloadTimeStamp(int loop) throws IOException, URISyntaxException, InterruptedException, SftpException {
        switch(loop) {
            case 4:
                System.out.println("This loop to reset the lastDownloadTimeStamp to current timestamp!");
                return Instant.now().getEpochSecond() * 1000;
            case 3:
                System.out.println("This loop to reset the lastDownloadTimeStamp to one day before!");
                return Instant.now().getEpochSecond() * 1000 - 24 *60 *60 *1000;
            case 2:
                System.out.println("Uploading another file with modified timestamp so that it could be picked up as new feed file");
                Thread.sleep(2 * 60 * 1000);
                sftpClient.uploadFile("buz.txt");
                return Instant.now().getEpochSecond() * 1000 - 2 * 60 *1000;
            default:
                System.out.println("Waiting...");
                break;
        }
        return Instant.now().getEpochSecond() * 1000;
    }

    private void verifyResult(int loop, List<ChannelSftp.LsEntry> files) {
        switch(loop) {
            case 4:
                assertThat(files.size()).isEqualTo(0);
                break;
            case 3:
                assertThat(files.size()).isEqualTo(2);
                break;
            case 2:
                assertThat(files.size()).isEqualTo(1);
                break;
            default:
                break;
        }
    }
}