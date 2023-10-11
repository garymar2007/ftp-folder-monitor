package com.gary;

import lombok.Data;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Component
public class FtpClient {
    private String server;
    private int port;
    private String user;
    private String password;
    private FTPClient ftp;

    // constructor
    public FtpClient(String server, int port, String user, String password) {
        this.server = server;
        this.port = port;
        this.user = user;
        this.password = password;
    }
    public void open() throws IOException {
        ftp = new FTPClient();
        FTPClientConfig conf = new FTPClientConfig();
        conf.setServerTimeZoneId("UTC");
        ftp.configure(conf);

        ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));

        ftp.connect(server, port);
        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            throw new IOException("Exception in connecting to FTP Server");
        }

        ftp.login(user, password);
    }

    public void close() throws IOException {
        ftp.disconnect();
    }

    public FTPFile[] listFiles(String path) throws IOException {
        return ftp.listFiles(path);
        /**
         * Only return file names
         */
//        return Arrays.stream(files)
//                .map(FTPFile::getName)
//                .collect(Collectors.toList());
    }

    public void downloadFile(String source, String destination) throws IOException {
        FileOutputStream out = new FileOutputStream(destination);
        ftp.retrieveFile(source, out);
    }

    public void putFileToPath(File file, String path) throws IOException {
        ftp.storeFile(path, new FileInputStream(file));
    }

    public List<FTPFile> getLatestFeedfiles(Long lastDownloadTimeStamp) throws IOException {
        FTPFile[] files = ftp.listFiles("/data");
        List<FTPFile> filesToBeDownloaded = new ArrayList<>();
        for(FTPFile f : files) {
            Long fileLastModifiedTimeStamp = f.getTimestamp().getTimeInMillis();
            if(fileLastModifiedTimeStamp > lastDownloadTimeStamp) {
                filesToBeDownloaded.add(f);
            }
        }
        return filesToBeDownloaded;
    }
}
