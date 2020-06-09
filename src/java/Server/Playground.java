package Server;

import Server.FileService.SFTPService.SftpClient.RemoteDirectory;
import Server.FileService.SFTPService.SftpClient.RemoteFile;
import Server.FileService.SFTPService.SftpClient.SftpClient;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.jcraft.jsch.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class Playground {

    public static void main(String[] args) {
        SftpClient client = new SftpClient("ohm.f4.htw-berlin.de", 5002, "ohdmOffViewer", "H!3r0glyph Sat3llite Era$er");
        System.out.println(client.connect());
        client.closeConnection();
    }
}
