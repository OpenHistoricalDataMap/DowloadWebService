import Server.CustomObjects.CommandReturn;
import Server.StaticVariables;
import org.apache.commons.net.ftp.FTPFile;

import java.io.*;

import static Server.StaticVariables.*;

public class Playground {

    public static void main(String[] args) {
        FtpClient client = new FtpClient();
        client.connect("141.45.146.200", 5000, "ohdmOffViewer", "H!3r0glyph Sat3llite Era$er");
        FTPFile[] list = new FTPFile[0];

        try {
            list = client.getDirList("");
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < list.length; i++) {
            FTPFile[] fileList = new FTPFile[0];
            try {
                fileList = client.getFileList(list[i].getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Dir : " + list[i].getName() + "\nFiles: " + "\n");
            for (FTPFile f : fileList) {
                System.out.println(f.getName() + " | " + f.getSize());
            }
        }
    }
}
