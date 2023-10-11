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
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class FtpClientMockFtpServerIntegrationTester {
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
        if (file1.delete()) {
            System.out.println("File deleted successfully");
        }
        File file2 = new File("C:/garymar2007/ftp-folder-monitor/temp/feedfile2.txt");
        if (file2.delete()) {
            System.out.println("File deleted successfully");
        }
    }

    /**
     * Test monitoring in ftp server folder
     * @throws IOException
     */
    @Test
    public void testIfNoNewFilesInFTPServerFolderShouldDoingNothing() throws IOException {
        FTPFile[] files = ftpClient.listFiles("");
        assertThat(files.length).isEqualTo(2);

        Long closestTimeStamp = 0L;
        lastDownloadTimeStamp = Instant.now().getEpochSecond() * 1000;
        for(FTPFile f : files) {
            Long fileLastModifiedTimeStamp = f.getTimestamp().getTimeInMillis();
            if (closestTimeStamp < fileLastModifiedTimeStamp) {
                closestTimeStamp = fileLastModifiedTimeStamp;
            }
            if(fileLastModifiedTimeStamp >= lastDownloadTimeStamp) {
                System.out.println("The lastDownloadTimeStramp is : " + lastDownloadTimeStamp);
                System.out.println(String.format("%s is the latest file to be downloaded", f.getName()));
            }else {
                System.out.println("The lastDownloadTimeStramp is : " + lastDownloadTimeStamp);
                System.out.println(String.format("%s is the old file not to be downloaded", f.getName()));

                assertThat(new File("temp/" + f.getName())).doesNotExist();
            }
        }
        if(closestTimeStamp != 0L) {
            lastDownloadTimeStamp = closestTimeStamp;
            System.out.println("The new lastDownloadTimeStramp is : " + lastDownloadTimeStamp);
        }

        cleanUpFolder();
    }

    /**
     * Test monitoring in ftp server folder
     * @throws IOException
     */
    @Test
    public void testIfNewFilesInFTPServerFolderShouldDownloadFiles() throws IOException {
        FTPFile[] files = ftpClient.listFiles("");
        assertThat(files.length).isEqualTo(2);

        Long closestTimeStamp = 0L;
        lastDownloadTimeStamp = Instant.now().getEpochSecond() * 1000 - 24 * 60 * 60 * 1000;
        for(FTPFile f : files) {
            Long fileLastModifiedTimeStamp = f.getTimestamp().getTimeInMillis();
            if (closestTimeStamp < fileLastModifiedTimeStamp) {
                closestTimeStamp = fileLastModifiedTimeStamp;
            }
            if(fileLastModifiedTimeStamp >= lastDownloadTimeStamp) {
                System.out.println("The lastDownloadTimeStramp is : " + lastDownloadTimeStamp);
                System.out.println(String.format("%s is the latest file to be downloaded", f.getName()));
                ftpClient.downloadFile("/data/" + f.getName(), "temp/" + f.getName());

                assertThat(new File("temp/" + f.getName())).exists();
            } else {
                System.out.println("The lastDownloadTimeStramp is : " + lastDownloadTimeStamp);
                System.out.println(String.format("%s is the old file not to be downloaded", f.getName()));

                assertThat(new File("temp/" + f.getName())).doesNotExist();
            }
        }
        if(closestTimeStamp != 0L) {
            lastDownloadTimeStamp = closestTimeStamp;
            System.out.println("The new lastDownloadTimeStramp is : " + lastDownloadTimeStamp);
        }

        cleanUpFolder();
    }

    /**
     * Test monitoring in ftp server folder
     * @throws IOException
     */
    @Test
    public void testIfNewFileAndOldFileInFTPServerFolderShouldDownloadNewFileOnly() throws IOException {
        FTPFile[] files = ftpClient.listFiles("");
        assertThat(files.length).isEqualTo(2);

        Long closestTimeStamp = 0L;
        lastDownloadTimeStamp = Instant.now().getEpochSecond();
        for(FTPFile f : files) {
            Long fileLastModifiedTimeStamp = f.getTimestamp().getTimeInMillis();
            if (closestTimeStamp < fileLastModifiedTimeStamp) {
                closestTimeStamp = fileLastModifiedTimeStamp;
            }
            if(fileLastModifiedTimeStamp >= lastDownloadTimeStamp) {
                System.out.println("The lastDownloadTimeStramp is : " + lastDownloadTimeStamp);
                System.out.println(String.format("%s is the latest file to be downloaded", f.getName()));
                ftpClient.downloadFile("/data/" + f.getName(), "temp/" + f.getName());

                assertThat(new File("temp/" + f.getName())).exists();
            } else {
                System.out.println("The lastDownloadTimeStramp is : " + lastDownloadTimeStamp);
                System.out.println(String.format("%s is the old file not to be downloaded", f.getName()));

                assertThat(new File("temp/" + f.getName())).doesNotExist();
            }
        }
        if(closestTimeStamp != 0L) {
            lastDownloadTimeStamp = closestTimeStamp;
            System.out.println("The new lastDownloadTimeStramp is : " + lastDownloadTimeStamp);
        }

        cleanUpFolder();
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
