package Server;

import java.io.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.StringTokenizer;

public class StaticVariables {

    public StaticVariables(String defaultInitFile) {
        StaticVariables.defaultInitFile = defaultInitFile;
    }


    public static String defaultInitFile = "./init.txt";
    // ---------------------------- will be read from File ----------------------------//
    public static String logDefaultDir = "log/";
    public static String mapsDefaultDir = "maps/";
    public static String sftpDefaultDir = "sftp/";
    public static String msgPath = "msgs/";
    public static String idSavePath = "ids/";

    public static boolean logTerminalOutput = true;
    public static int maxLogFileSize = 512; // in kilobytes

    public static int webPort = 5001;
    //public static int ftpPort = 5000;
    public static int sftpPort = 5002;

    // default values !!!! ALWAYS CHANGE IN FILE !!!!
    public static String standardUserName = "";
    public static String standardUserPassword = "";
    // -------------------------------------------------------------------------------//

    public static String osmDir;
    public static String mapDir;
    public static String ohdmDir = "";
    public static String specificLogDir;

    public static String sftpServiceMapDir = mapDir;
    public static String sftpDefaultKeyFile = sftpDefaultDir + "hostKeySave.ser";

    public static String renderingParameterFilePath = "db_rendering.txt";
    public static String ohdmConverterFilePath = "OHDMConverter.jar";
    public static String jdbcDriverFilePath = "postgresql-42.1.1.jar";

    public static String javaJdkPath = "java";

    // ------------------------------------------------------------------------------- //
        public static final long oneHourInMs = 3600000;
    // ------------------------------------------------------------------------------- //

    public static void init() throws IOException {
        if (!new File(defaultInitFile).exists())
         throw new IOException("couldn't find init.txt File in the execution directory !!!!");

         if (!new File(defaultInitFile).exists()) {
            //giveStandardValues();
            return;
        }

        BufferedReader dis = null;
        try {
            dis = new BufferedReader(new InputStreamReader(new FileInputStream(new File(defaultInitFile))));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // readString
        String readString = "";
        try {
            while (dis.ready()) readString += dis.readLine() + "\n";
        } catch (IOException e) {
            e.printStackTrace();
        }

        // cleanup String
        String usableLines = "";
        for (String s : readString.split("\n"))
            if (!(s.startsWith("//") || s.startsWith("[") || s.isEmpty())) usableLines += s + "\n";

        // split names and values
        String[][] valuesSplit = new String[usableLines.split("\n").length][2];
        for (int i = 0; i < usableLines.split("\n").length; i++) {
            valuesSplit[i][0] = usableLines.split("\n")[i].split("=")[0].trim();
            valuesSplit[i][1] = usableLines.split("\n")[i].split("=")[1].trim();
        }

        for (String[] s :
                valuesSplit) {
            assignValue(s);
        }
    }

    private static void assignValue(String[] s) {
        switch (s[0]) {
            case "logDefaultDir": logDefaultDir = s[1].trim(); break;

            case "mapsDefaultDir": mapsDefaultDir = s[1].trim(); break;

            case "sftpDefaultDir": sftpDefaultDir = s[1].trim(); break;

            case "webPort": try {
                webPort = Integer.parseInt(s[1].trim());
            } catch (NumberFormatException e) {
                webPort = 5001;
            }break;

            case "sftpPort": try {
                sftpPort = Integer.parseInt(s[1].trim());
            } catch (NumberFormatException e) {
                sftpPort = 5002;
            }break;

            case "standardUserName": standardUserName = s[1].trim();break;

            case "standardUserPassword": standardUserPassword = s[1].trim();break;

            case "logTerminalOutput": logTerminalOutput = Boolean.parseBoolean(s[1].trim());

            case "renderingParameterFilePath": renderingParameterFilePath = s[1].trim(); break;

            case "ohdmConverterFilePath" : ohdmConverterFilePath = s[1].trim(); break;

            case "jdbcDriverFilePath" : jdbcDriverFilePath = s[1].trim(); break;

            case "javaJdkPath" : javaJdkPath = s[1].trim(); break;

            case "msgPath" : msgPath = s[1].trim(); break;

            case "idSavePath" : idSavePath = s[1].trim(); break;

            case "maxLogFileSize": try {
                    maxLogFileSize = Integer.parseInt(s[1].trim());
                } catch (NumberFormatException e) {
                    maxLogFileSize = 512;
                }

            default:
                System.out.println("[INIT-INFO] - couldn't find " + s[0] + " in list | Value = " + s[1]);
        }
    }
    public static void createStdFilesAndDirs() throws IOException {
        new File(logDefaultDir).mkdir();
        new File(sftpDefaultDir).mkdir();
        new File(mapsDefaultDir).mkdir();

        osmDir = mapsDefaultDir + "osm";
        mapDir = mapsDefaultDir + "map";
        ohdmDir = "";
        specificLogDir = mapsDefaultDir + "req";

        sftpServiceMapDir = mapDir;
        sftpDefaultKeyFile = sftpDefaultDir + "hostKeySave.ser";

        new File(osmDir).mkdir();
        new File(mapDir).mkdir();
        new File(specificLogDir).mkdir();

        new File(sftpDefaultKeyFile).createNewFile();
    }

    public static String formatDateTimeDif(LocalDateTime time1, LocalDateTime time2) {

        LocalDateTime tempDateTime = LocalDateTime.from(time1);

        long days = tempDateTime.until(time2, ChronoUnit.DAYS );
        tempDateTime = tempDateTime.plusDays( days );

        long hours = tempDateTime.until(time2, ChronoUnit.HOURS );
        tempDateTime = tempDateTime.plusHours( hours );

        long minutes = tempDateTime.until(time2, ChronoUnit.MINUTES );
        tempDateTime = tempDateTime.plusMinutes( minutes );

        long seconds = tempDateTime.until(time2, ChronoUnit.SECONDS );

        String returner = "";

        if ((int) (days / 10) == 0) returner += "0" + days + "days "; else returner += days + "days ";
        if ((int) (hours / 10) == 0) returner += "0" + hours + ":"; else returner += hours + ":";
        if ((int) (minutes / 10) == 0) returner += "0" + minutes + ":"; else returner += minutes + ":";
        if ((int) (seconds / 10) == 0) returner += "0" + seconds + ""; else returner += seconds + "";

        return returner;
    }
}
