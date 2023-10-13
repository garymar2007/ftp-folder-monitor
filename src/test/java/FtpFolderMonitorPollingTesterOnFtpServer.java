import com.gary.FtpClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockftpserver.fake.filesystem.FileSystem;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class FtpFolderMonitorPollingTesterOnFtpServer {

    private FtpClient ftpClient;

    private Long lastDownloadTimeStamp;

    private FileSystem fileSystem;

    private final String DOWNLOAD_FOLDER="C:\\garymar2007\\ftp-folder-monitor\\download\\";

    /**
     * Setup the ftp server folder structure:
     * /data/
     *      feedfile1.txt: lastModifiedTimeStamp - 2023/10/10 00:00:00 am
     *      feedfile2.txt: lastModifiedTimeStamp - 2023/10/10 00:00:00 am
     * @throws IOException
     * @throws InterruptedException
     */
    @Before
    public void setup() throws IOException, URISyntaxException {
        ftpClient = new FtpClient("localhost", 21, "user", "123456");
        ftpClient.open();

        prepareFiles();
    }

    private void prepareFiles() throws URISyntaxException, IOException {
        uploadFile("feedfile1.txt");
        uploadFile("feedfile2.txt");
    }


    private void uploadFile(String fileName) throws URISyntaxException, IOException {
        File file = new File(getClass().getClassLoader().getResource("ftp/" + fileName).toURI());
        ftpClient.putFileToPath(file, "/data/" + fileName);
    }

    @After
    public void teardown() throws IOException {
        ftpClient.getFtp().deleteFile("/data/feedfile1.txt");
        ftpClient.getFtp().deleteFile("/data/feedfile2.txt");
        ftpClient.getFtp().deleteFile("/data/buz.txt");
        ftpClient.close();

        cleanUpFolder();
    }

    private void cleanUpFolder() {
        Arrays.stream(Objects.requireNonNull(new File(DOWNLOAD_FOLDER)
                .listFiles())).forEach(File::delete);
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
                    ftpClient.downloadFile("/data/" + f.getName(), "download/" + f.getName());
                }
            }

            verifyResult(loop, filesToBeDownloaded);

            Thread.sleep(5000);
        }
    }

    private Long resetLastDownloadTimeStamp(int loop) throws IOException, URISyntaxException, InterruptedException {
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
                uploadFileToFtpServer();
                return Instant.now().getEpochSecond() * 1000 - 2 * 60 *1000;
            default:
                System.out.println("Waiting...");
                break;
        }
        return Instant.now().getEpochSecond() * 1000;
    }

    private void uploadFileToFtpServer() throws IOException, URISyntaxException {
        File file = new File(getClass().getClassLoader().getResource("ftp/buz.txt").toURI());
        ftpClient.putFileToPath(file, "/data/buz.txt");
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
