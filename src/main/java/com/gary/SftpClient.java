package com.gary;

import com.jcraft.jsch.*;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Data
//@Component
public class SftpClient {
    private JSch jsch;
    private Session session= null;
    private Channel channel = null;
    private ChannelSftp channelSftp = null;

    private final int SFTPPORT = 22;

    public SftpClient(String userName, String password, String hostName) {
        jsch = new JSch();
        try{
            session = jsch.getSession(userName, hostName, SFTPPORT);
            session.setPassword(password);
            System.out.println("Session created!");

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            System.out.println("Shell channel connected...");
            channelSftp = (ChannelSftp)channel;

        } catch(JSchException e) {
            e.printStackTrace();
        }
    }

    public void uploadFile(String fileName) throws FileNotFoundException, URISyntaxException, SftpException {
        File file = new File(getClass().getClassLoader().getResource("ftp/" + fileName).toURI());
        FileInputStream fis = new FileInputStream(file);
        channelSftp.put(fis, "/data/" + file.getName());
    }

    public void downloadFile(String remoteFileName, String downloadFileName) throws SftpException {
        channelSftp.get(remoteFileName, downloadFileName);
    }

    public List<LsEntry> getLatestFeedfiles(Long lastDownloadTimeStamp) throws SftpException {
        //channelSftp.cd("data/");
        List<LsEntry> allFiles = channelSftp.ls("/data/");
        List<LsEntry> filesToBeDownloaded = new ArrayList<>();
        if(allFiles != null && !allFiles.isEmpty()) {
            for(LsEntry file : allFiles) {
                if(file.getFilename().equalsIgnoreCase(".") || file.getFilename().equalsIgnoreCase("..")) {
                    continue;
                }
                if(file != null) {
                    Long fileLastModifiedTimeStamp = new Date((long)file.getAttrs().getMTime() * 1000L).toInstant().toEpochMilli();
                    if(fileLastModifiedTimeStamp > lastDownloadTimeStamp) {
                        filesToBeDownloaded.add(file);
                    }
                }
            }
        }

        return filesToBeDownloaded;
    }

    public void disconnect(){
        channelSftp.disconnect();
        session.disconnect();
        channel.disconnect();
        channelSftp.exit();
    }

    public void cleanupFolder() throws SftpException {
        List<LsEntry> allFiles = channelSftp.ls("/data/");
        if(allFiles != null && !allFiles.isEmpty()) {
            for (LsEntry file : allFiles) {
                if (file.getFilename().equalsIgnoreCase(".") || file.getFilename().equalsIgnoreCase("..")) {
                    continue;
                }
                if (file != null) {
                    channelSftp.rm(file.getFilename());
                    System.out.println("Successfully removed file: " + file.getFilename());
                }
            }
        }
    }
}