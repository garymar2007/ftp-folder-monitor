package com.gary;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class FtpFolderMonitorScheduler {
    private SftpClient sftpClient = new SftpClient("user", "123456", "localhost");


    @Scheduled(fixedRate=5000)
    public void monitorFtpFolder() throws SftpException, IOException {
        Long lastDownloadTimestamp = Instant.now().toEpochMilli() - 5 * 60 * 1000;
        List<ChannelSftp.LsEntry> filesToBeDownloaded = sftpClient.getLatestFeedfiles(lastDownloadTimestamp);

        if(!filesToBeDownloaded.isEmpty()) {
            for(ChannelSftp.LsEntry e : filesToBeDownloaded) {
                String destFilePathName = "temp/" + e.getFilename();
                sftpClient.downloadFile(e.getFilename(), destFilePathName);
                File downloadedFile = new File(destFilePathName);
                if(downloadedFile.exists() && downloadedFile.isFile() && destFilePathName.contains("zip")){
                    System.out.println("Unzipping feed file!");
                    File unzippedFile = unzipFile(destFilePathName);
                    if(unzippedFile != null && unzippedFile.exists()) {
                        System.out.println("A new feed file has been downloaded and unzipped successfully!");
                        downloadedFile.delete();
                        System.out.println("The downloaded file has been deleted successfully!");
                    }
                }
            }
        }
        else {
            System.out.println("There is no new feed files in the FTP server folder!");
        }
    }

    private File unzipFile(String filePathName) throws IOException {
        File destDir = new File("temp/feedfile");
        File newFile = null;
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(filePathName));
        ZipEntry zipEntry = zis.getNextEntry();
        while(zipEntry != null) {
            newFile = newFile(destDir, zipEntry);
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            zipEntry = zis.getNextEntry();
        }

        return newFile;
    }

    private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if(!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
