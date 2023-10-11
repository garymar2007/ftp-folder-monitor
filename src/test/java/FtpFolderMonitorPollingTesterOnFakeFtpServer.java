import com.gary.FtpClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class FtpFolderMonitorPollingTesterOnFakeFtpServer {
    private FakeFtpServer fakeFtpServer;

    private FtpClient ftpClient;

    private Long lastDownloadTimeStamp;

    private FileSystem fileSystem;

    /**
     * Setup the ftp server folder structure:
     * /data/
     *      feedfile1.txt: lastModifiedTimeStamp - 2023/10/10 00:00:00 am
     *      feedfile2.txt: lastModifiedTimeStamp - 2023/10/10 00:00:00 am
     * @throws IOException
     * @throws InterruptedException
     */
    @Before
    public void setup() throws IOException, InterruptedException {
        fakeFtpServer = new FakeFtpServer();
        fakeFtpServer.addUserAccount(new UserAccount("user", "password", "/data"));

        fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/data"));
        fileSystem.add(new FileEntry("/data/feedfile1.txt", "This is feed file 1"));
        fileSystem.add(new FileEntry("/data/feedfile2.txt", "This is feed file 2"));
        fakeFtpServer.setFileSystem(fileSystem);
        fakeFtpServer.setServerControlPort(0);

        fakeFtpServer.start();

        ftpClient = new FtpClient("localhost", fakeFtpServer.getServerControlPort(), "user", "password");
        ftpClient.open();
    }

    @After
    public void teardown() throws IOException {
        ftpClient.close();
        fakeFtpServer.stop();
    }

    private void cleanUpFolder() {
        File file1 = new File("C:/garymar2007/ftp-folder-monitor/temp/feedfile1.txt");
        if(file1.delete()){
            System.out.println("File deleted successfully");
        }
        File file2 = new File("C:/garymar2007/ftp-folder-monitor/temp/feedfile2.txt");
        if(file2.delete()){
            System.out.println("File deleted successfully");
        }
    }

    /**
     * Test monitoring in ftp server folder to download any new files
     * @throws IOException
     */
    @Test
    public void testSimulateScheduledPolling() throws IOException, InterruptedException, URISyntaxException {
        int loop = 5;

        while(loop > 0) {
            loop--;
            Long lastDownloadTimeStamp = resetLastDownloadTimeStamp(loop);

            List<FTPFile> filesToBeDownloaded = ftpClient.getLatestFeedfiles(lastDownloadTimeStamp);
            if(filesToBeDownloaded.isEmpty()) {
                System.out.println("There is no new feed file in FTP server folder");
            } else {
                for(FTPFile f : filesToBeDownloaded) {
                    ftpClient.downloadFile("/data/" + f.getName(), "temp/" + f.getName());
                }
            }

            verifyResult(loop, filesToBeDownloaded);

            Thread.sleep(5000);
        }

        cleanUpFolder();
    }

    private Long resetLastDownloadTimeStamp(int loop) throws IOException, URISyntaxException {
        switch(loop) {
            case 4:
                System.out.println("This loop to reset the lastDownloadTimeStamp to current timestamp!");
                return Instant.now().getEpochSecond() * 1000;
            case 3:
                System.out.println("This loop to reset the lastDownloadTimeStamp to one day before!");
                return Instant.now().getEpochSecond() * 1000 - 24 *60 *60 *1000;
            case 2:
                System.out.println("This loop to reset the lastDownloadTimestamp to be after the first file creation " +
                        "timestamp and before the second file creation timestamp!");
                uploadFileToFtpServer();
                return Instant.now().getEpochSecond() * 1000 - 10 * 60 * 1000;
            default:
                System.out.println("Waiting...");
                break;
        }
        return 0L;
    }

    private void uploadFileToFtpServer() throws IOException, URISyntaxException {
        File file = new File(getClass().getClassLoader().getResource("ftp/buz.txt").toURI());
        file.setLastModified(Instant.now().toEpochMilli());
        ftpClient.putFileToPath(file, "/data/buz.txt");

        assertThat(fakeFtpServer.getFileSystem().exists("/data/buz.txt")).isTrue();
    }


    private void verifyResult(int loop, List<FTPFile> files) {
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
