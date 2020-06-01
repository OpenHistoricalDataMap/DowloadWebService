package Server.FTPService.FTPCli;

import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;

public class FTPCliTest {

    public static void main(String[] args) {
        FtpClient client = new FtpClient();
        client.connect("141.45.146.200", 5000, "ohdmOffViewer", "H!3r0glyph Sat3llite Era$er");
        FTPFile[] list = new FTPFile[0];
        try {
            list = client.getFileList("");
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (FTPFile f : list) {
            System.out.println(f.getName());
        }

        try {
            client.downloadFile("sachsen.map", "sachsen.map");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
