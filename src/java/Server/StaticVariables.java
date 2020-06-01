package Server;

import java.io.File;
import java.io.IOException;

public class StaticVariables {
    public static final String defaultInitFile = "./init.txt";
    // ---------------------------- will be read from File ----------------------------//
    public static String logDefaultDir = "./log/";
    public static String mapsDefaultDir = "./maps/";
    public static String ftpDefaultDir = "./ftp/";

    public static boolean logTerminalOutput = true;

    public static int maxLogFileSize = 512; // in kilobytes

    public static int webPort = 5001;
    public static int ftpPort = 5000;

    // default values !!!! ALWAYS CHANGE IN FILE !!!!
    public static String standardUserName = "ohdmOffViewer";
    public static String standardUserPassword = "H!3r0glyph Sat3llite Era$er";
    // -------------------------------------------------------------------------------//
    public static String ftpLogFile;
    public static String webServiceLogFile;

    public static String osmDir;
    public static String mapDir;
    public static String ohdmDir = "";

    public static String ftpServiceMapDir = mapDir;
    public static String ftpServiceUserPropertiesFile = ftpDefaultDir + "userList.properties";



    public static void init() {
        giveStandardValues();
        /* if (!new File(defaultInitFile).exists()) {
            giveStandardValues();
            return;
        } */

        /*BufferedReader dis = null;
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

         */
    }

    private static void giveStandardValues() {

        logDefaultDir = "./log/";
        mapsDefaultDir = "./maps/";
        ftpDefaultDir = "./ftp/";

        webPort = 5001;
        ftpPort = 5000;

        standardUserName = "ohdmOffViewer";
        standardUserPassword = "H!3r0glyph Sat3llite Era$er";
    }

    private static void assignValue(String[] s) {
        switch (s[0]) {
            case "logDefaultDir": logDefaultDir = s[1].trim(); break;

            case "mapsDefaultDir": mapsDefaultDir = s[1].trim(); break;

            case "ftpDefaultDir": ftpDefaultDir = s[1].trim(); break;

            case "webPort": try {
                    webPort = Integer.parseInt(s[1].trim());
                } catch (NumberFormatException e) {
                    webPort = 5001;
                }break;

            case "ftpPort": try {
                    ftpPort = Integer.parseInt(s[1].trim());
                } catch (NumberFormatException e) {
                    ftpPort = 5000;
                }break;

            case "standardUserName": standardUserName = s[1].trim();break;

            case "standardUserPassword": standardUserPassword = s[1].trim();break;

            default: System.out.println("couldn't find " + s[0] + " in list | Value = " + s[1]);
        }
    }

    public static void createStdFilesAndDirs() throws IOException {
        new File(logDefaultDir).mkdir();
        new File(ftpDefaultDir).mkdir();
        new File(mapsDefaultDir).mkdir();

        osmDir = mapsDefaultDir + "osm";
        mapDir = mapsDefaultDir + "map";
        ohdmDir = "";


        ftpServiceMapDir = mapDir;
        ftpServiceUserPropertiesFile = ftpDefaultDir + "userList.properties";

        new File(osmDir).mkdir();
        new File(mapDir).mkdir();

        new File(ftpServiceUserPropertiesFile).createNewFile();
    }
}