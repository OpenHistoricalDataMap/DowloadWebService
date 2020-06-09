package Server;

import java.io.*;

class StaticVariables {

    public StaticVariables(String defaultInitFile) {
        StaticVariables.defaultInitFile = defaultInitFile;
    }

    public static String defaultInitFile = "./init.txt";
    // ---------------------------- will be read from File ----------------------------//
    public static String logDefaultDir = "./log/";
    public static String mapsDefaultDir = "./maps/";
    public static String sftpDefaultDir = "./sftp/";

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

    public static String sftpServiceMapDir = mapDir;
    public static String sftpDefaultKeyFile = sftpDefaultDir + "hostKeySave.ser";

    // -------------------------------------------------------------------------------//

    public static void init() throws IOException {
        if (!new File(defaultInitFile).exists())
         throw new IOException("couldn't find init.txt File in the execution directory !!!!");

         if (!new File(defaultInitFile).exists()) {
            giveStandardValues();
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

    private static void giveStandardValues() {

        logDefaultDir = "log/";
        mapsDefaultDir = "maps/";
        sftpDefaultDir = "sftp/";

        webPort = 5001;
        sftpPort = 5002;

        standardUserName = "ohdmOffViewer";
        standardUserPassword = "H!3r0glyph Sat3llite Era$er";
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

            case "maxLogFileSize":
                try {
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

        sftpServiceMapDir = mapDir;
        sftpDefaultKeyFile = sftpDefaultDir + "hostKeySave.ser";

        new File(osmDir).mkdir();
        new File(mapDir).mkdir();

        new File(sftpDefaultKeyFile).createNewFile();
    }
}
