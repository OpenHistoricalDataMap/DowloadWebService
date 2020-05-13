package Server;

public class StaticVariables {
    public static final String ftpLogFile = "ftpLog.txt";
    public static final String webServiceLogFile = "webServiceLog.txt";

    public static final String osmDir = "./maps/osm";
    public static final String mapDir = "./maps/map";
    public static final String ohdmDir = "";

    public static final String ftpServiceMapDir = mapDir;
    public static final String ftpServiceUserPropertiesFile = "./ftp/userList.properties";
    public static final int ftpPort = 5000;

    // will later be read from a File instead of being HardCoded
    public static final String standardUserName = "ohdmOffViewer";
    public static final String standardUserPassword = "H!3r0glyph Sat3llite Era$er";

}
