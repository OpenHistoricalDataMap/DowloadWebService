package Server;

import java.io.*;

public class StaticVariables {
    public static final String defaultInitFile = "./init.txt";
    // ---------------------------- will be read from File ----------------------------//
    public static String logDefaultDir;
    public static String mapsDefaultDir;
    public static String ftpDefaultDir;

    public static boolean doesFTPServiceLog;
    public static boolean doesWebServiceLog;

    public static int webPort;
    public static int ftpPort;

    // default values !!!! ALWAYS CHANGE IN FILE !!!!
    public static String standardUserName = "ohdmOffViewer";
    public static String standardUserPassword = "H!3r0glyph Sat3llite Era$er";
    // -------------------------------------------------------------------------------//
    public static final String ftpLogFile = logDefaultDir + "/ftpLog.txt";
    public static final String webServiceLogFile = logDefaultDir + "/webServiceLog.txt";

    public static final String osmDir = mapsDefaultDir + "/osm";
    public static final String mapDir = mapsDefaultDir + "/map";
    public static final String ohdmDir = "";

    public static final String ftpServiceMapDir = mapDir;
    public static final String ftpServiceUserPropertiesFile = ftpDefaultDir + "/userList.properties";


    public static void init() {
        if (!new File(defaultInitFile).exists()) {
            giveStandardValues();
            return;
        }

        BufferedReader dis = null;
        try { dis = new BufferedReader(new InputStreamReader(new FileInputStream(new File(defaultInitFile)))); }
        catch (FileNotFoundException e) { e.printStackTrace(); }

        // readString
        String readString = "";
        try { while (dis.ready()) readString += dis.readLine() + "\n"; }
        catch (IOException e) { e.printStackTrace(); }

        // cleanup String
        String usableLines = "";
        for (String s: readString.split("\n")) if (!(s.startsWith("//") || s.startsWith("[") || s.isEmpty())) usableLines += s + "\n";

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

    private static void giveStandardValues() {
        doesFTPServiceLog = true;
        doesWebServiceLog = true;

        logDefaultDir = "./log";
        mapsDefaultDir = "./maps";
        ftpDefaultDir = "./ftp";

        webPort = 5001;
        ftpPort = 5000;

        standardUserName = "ohdmOffViewer";
        standardUserPassword = "H!3r0glyph Sat3llite Era$er";
    }

    private static void assignValue(String[] s) {
        switch (s[0]) {
            case "logDefaultDir": logDefaultDir = s[1]; break;

            case "mapsDefaultDir": mapsDefaultDir = s[1]; break;

            case "ftpDefaultDir": ftpDefaultDir = s[1]; break;

            case "doesFTPServiceLog": doesFTPServiceLog = Boolean.parseBoolean(s[1]); break;

            case "doesWebServiceLog": doesWebServiceLog = Boolean.parseBoolean(s[1]); break;

            case "webPort": try { webPort = Integer.parseInt(s[1]); } catch (NumberFormatException e) { webPort = 5001; } break;

            case "ftpPort": try { ftpPort = Integer.parseInt(s[1]); } catch (NumberFormatException e) { ftpPort = 5000; } break;

            case "standardUserName": standardUserName = s[1]; break;

            case "standardUserPassword": standardUserPassword = s[1]; break;

            default:
                System.err.println("couldn't find " + s[0] + " in list | Value = " + s[1]);
        }
    }
}
