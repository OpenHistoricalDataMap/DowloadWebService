import Server.CustomObjects.LogType;
import Server.FTPService.FTPCli.FtpClient;
import Server.LogService.Logger;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Playground {
    static String currentWritingDir;
    static String logDefaultDir;
    static SimpleDateFormat dirFormatter = new SimpleDateFormat("yyyy-MM-dd");
    static String currentWritingFile;
    static String baseFile;
    static SimpleDateFormat fileFormatter = new SimpleDateFormat("HH-mm-ss z");


    public static void main(String[] args) {
        logDefaultDir = "";
        baseFile = "LOG";

        currentWritingDir = logDefaultDir + dirFormatter.format(System.currentTimeMillis() - 86400000 );
        currentWritingFile = currentWritingDir + "/" + baseFile.replace(".txt", "") + "["+ fileFormatter.format(System.currentTimeMillis() - 86400000) +"].txt";

        swapToNextDayDirIfNecessary();
    }

    private static void swapToNextDayDirIfNecessary() {
        Date date = new Date(System.currentTimeMillis());

        if (!currentWritingDir.equals(logDefaultDir + dirFormatter.format(date))) {
            File nwd = new File(logDefaultDir + dirFormatter.format(date));

            if (!nwd.exists())
                nwd.mkdir();

            currentWritingDir = "./" + nwd.getName();

            currentWritingFile = currentWritingDir + "/" + baseFile.replace(".txt", "") + "["+ fileFormatter.format(System.currentTimeMillis()) +"].txt";;

            File file = new File(currentWritingFile);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
