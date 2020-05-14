package FTPClient;

import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;

public class TestMain {
    static FtpClientImpl client = new FtpClientImpl();

    public static void main(String[] args) {
        client.connect("localhost", 5000, "ohdmOffViewer", "H!3r0glyph Sat3llite Era$er");
        FTPFile[] files = new FTPFile[0];
        try {
            files = client.getFileList();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("File list : \n");
        for (FTPFile f :
                files) {
            System.out.println(f.getName());
        }

        String downloadableFile = files[0].getName();
        try {
            client.downloadFile(downloadableFile, downloadableFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        client.closeConnection();
    }
}
