package com.gary;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class FtpFolderMonitorScheduler {
    private SftpClient sftpClient = new SftpClient("user", "123456", "localhost");


    @Scheduled(fixedRate=5000)
    public void monitorFtpFolder() {
      // sftpClient.
    }
}
