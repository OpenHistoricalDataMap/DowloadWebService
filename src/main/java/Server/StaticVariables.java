package Server;

public class StaticVariables {
    public static final String logDefaultDir = "./log";

    public static final boolean doesFTPServiceLog = true;
    public static final boolean doesWebServiceLog = true;

    public static final String ftpLogFile = logDefaultDir + "/ftpLog.txt";
    public static final String webServiceLogFile = logDefaultDir + "/webServiceLog.txt";

    public static final String mapsDefalutDir = "./maps";
    public static final String osmDir = mapsDefalutDir + "/osm";
    public static final String mapDir = mapsDefalutDir + "/map";
    public static final String ohdmDir = "";

    public static final String ftpServiceMapDir = mapDir;
    public static final String ftpDefaultDir = "./ftp";
    public static final String ftpServiceUserPropertiesFile = ftpDefaultDir + "/userList.properties";

    public static final int ftpPort = 5000;

    // will later be read from a File instead of being HardCoded
    public static final String standardUserName = "ohdmOffViewer";
    public static final String standardUserPassword = "H!3r0glyph Sat3llite Era$er";

}
