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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

public class FtpClientIntegrationTester {
    private FakeFtpServer fakeFtpServer;

    private FtpClient ftpClient;

    private Long lastDownloadTimeStamp;
    @Before
    public void setup() throws IOException, InterruptedException {
        fakeFtpServer = new FakeFtpServer();
        fakeFtpServer.addUserAccount(new UserAccount("user", "password", "/data"));

        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/data"));
        lastDownloadTimeStamp = System.currentTimeMillis() - 24*60*60*1000;
        fileSystem.add(new FileEntry("/data/feedfile1.txt", "abcdef 1234567890"));
        Thread.sleep(2000);
        fileSystem.add(new FileEntry("/data/feedfile2.txt", "abcdef 1234567890"));
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

    /**
     * Test listing files in ftp server folder
     * @throws IOException
     */
    @Test
    public void givenRemoteFilesDetermineWhichOneToBeDownloadedBasedOnLastModifiedTimestamp() throws IOException {
        FTPFile[] files = ftpClient.listFiles("");
        assertThat(files.length).isEqualTo(2);
//        assertThat(files).contains("feedfile1.txt");
//        assertThat(files).contains("feedfile2.txt");
        Long closestTimeStamp = 0L;
        for(FTPFile f : files) {
            Long fileLastModifiedTimeStamp = f.getTimestamp().getTimeInMillis();
            if (closestTimeStamp < fileLastModifiedTimeStamp) {
                closestTimeStamp = fileLastModifiedTimeStamp;
            }
            if(fileLastModifiedTimeStamp >= lastDownloadTimeStamp) {
                System.out.println("The lastDownloadTimeStramp is : " + lastDownloadTimeStamp);
                System.out.println(String.format("%s is the latest file to be downloaded", f.getName()));
            }
        }
        if(closestTimeStamp != 0L) {
            lastDownloadTimeStamp = closestTimeStamp;
            System.out.println("The new lastDownloadTimeStramp is : " + lastDownloadTimeStamp);
        }
    }

    /**
     * Test downloading file from ftp server
     * @throws IOException
     */
    @Test
    public void givenRemoteFile_whenDownloading_thenItIsOnTheLocalFilesystem() throws IOException {
        ftpClient.downloadFile("/data/feedfile1.txt", "temp/downloaded_buz.txt");
        assertThat(new File("temp/downloaded_buz.txt")).exists();
        //new File("downloaded_buz.txt").delete(); // cleanup
    }

    /**
     * Test uploading file to ftp server
     * @throws URISyntaxException
     * @throws IOException
     */
//    @Test
//    public void givenLocalFile_whenUploadingIt_thenItExistsOnRemoteLocation()
//            throws URISyntaxException, IOException {
//
//        File file = new File(getClass().getClassLoader().getResource("baz.txt").toURI());
//        ftpClient.putFileToPath(file, "/ftp/buz.txt");
//        assertThat(fakeFtpServer.getFileSystem().exists("/ftp/buz.txt")).isTrue();
//    }
}
