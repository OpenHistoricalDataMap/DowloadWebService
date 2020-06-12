package Server;

import Server.ControllerEndpoint.ControllerEndpoint;
import Server.CustomObjects.Coords;
import Server.CustomObjects.LogEntry;
import Server.CustomObjects.QueryRequest;
import Server.LogService.Logger;
import Server.FileService.SFTPService.SftpService;
import Server.WebService.ServiceNew;
import com.google.gson.Gson;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static Server.CustomObjects.LogType.ERROR;
import static Server.CustomObjects.LogType.INFO;

@SpringBootApplication
@RestController
public class SpringClass {

    private static String initFile = "init.txt";

    public static ServiceNew serviceInstance;
    private static Thread serviceThread;

    // depricated
    // private static FTPService ftpInstance;
    // private static Thread ftpThread;

    public static SftpService sftpInstance;
    private static Thread sftpThread;

    private static ApplicationContext cntxt;

    private String TAG = "Spring-Thread";

    /**
     * setup for initial start and full restarts
     * @throws IOException
     */
    private static void set_up() throws IOException {
        // just standard inits for the Variables used in this Project
        // most of them are read from the init.txt
        // but more of that in the OHDM wiki

        new StaticVariables(initFile);
        StaticVariables.init();
        StaticVariables.createStdFilesAndDirs();

        // Logger is a singleton Class due to the availability need
        // the Logger is a System in itself
        new Logger(StaticVariables.logDefaultDir, StaticVariables.maxLogFileSize, StaticVariables.logTerminalOutput);
        Logger.instance.start();

        // the service instance, which just goes through a couple of lists and sets things to what they are
        // you could actually call it an "Controller" of some sorts... but I didn't bother changing the name
        // TODO: maybe later
        serviceInstance = new ServiceNew();
        serviceThread = new Thread(serviceInstance);
        serviceThread.start();

        // the ftp service, which allows the Android App to download the .map files
        // depricated
        /*ftpInstance = new FTPService(StaticVariables.ftpPort, StaticVariables.ftpServiceUserPropertiesFile,  StaticVariables.standardUserName, StaticVariables.standardUserPassword, StaticVariables.ftpServiceMapDir, StaticVariables.ftpDefaultDir);
        ftpThread = new Thread(ftpInstance);
        ftpThread.start();*/

        // the sftp service, which allows the Android App to download the .map files
        sftpInstance = new SftpService(StaticVariables.sftpPort, StaticVariables.standardUserName, StaticVariables.standardUserPassword, StaticVariables.sftpDefaultKeyFile, StaticVariables.sftpServiceMapDir, StaticVariables.msgPath);
        sftpThread = new Thread(sftpInstance);
        sftpThread.start();
    }

    public static void main(String[] args) throws IOException {
        set_up();

        // and here starts the Spring Application with the server port set to the
        // before given Port in Server.StaticVariables
        System.getProperties().put("server.port", StaticVariables.webPort);
        cntxt = SpringApplication.run(SpringClass.class, args);

        ControllerEndpoint ce = new ControllerEndpoint(StaticVariables.msgPath);
        Thread ce_thread = new Thread(ce);
        ce_thread.start();
    }

    public static String reloadWebService() throws IOException {
        StaticVariables.init();
        StaticVariables.createStdFilesAndDirs();
        ArrayList<QueryRequest> bufferSave = serviceInstance.getBUFFER_LIST();
        serviceInstance.stopThread();
        serviceInstance = new ServiceNew(bufferSave, 5);
        serviceThread = new Thread(serviceInstance);
        serviceThread.start();
        return "webservice reloaded | saved pending Requests in Buffer = " + bufferSave.size();
    }

    public static String reloadLog() throws IOException {
        StaticVariables.init();
        StaticVariables.createStdFilesAndDirs();
        Logger.instance.stopThread();
        List<LogEntry> bufferSave = Logger.instance.getBUFFER_LIST();
        new Logger(StaticVariables.logDefaultDir, StaticVariables.maxLogFileSize, StaticVariables.logTerminalOutput);
        Logger.instance.setBUFFER_LIST(bufferSave);
        Logger.instance.start();
        return "Logger reloaded | saved pending LogEntries in Buffer = " + bufferSave.size();
    }

    public static String restart() throws IOException {
        SpringApplication.exit(cntxt, new ExitCodeGenerator() {
            @Override
            public int getExitCode() {
                return 0;
            }
        });
        set_up();
        System.getProperties().put("server.port", StaticVariables.webPort);
        cntxt = SpringApplication.run(SpringClass.class);
        return "restarted service -  look into daemon-log to see init";
    }

    @RequestMapping("/")
    public String webServiceStatus() {

        StringBuilder sb = new StringBuilder();
        sb.append("<head> <title> WebService status</title> </head>");

        sb.append("<p> Service Running = " + serviceInstance.isRunning() + "</p>");
        sb.append("<p> WatcherThread currently working = " + serviceInstance.isActive() + "</p>");
        sb.append("<p> sftp Service running = " + sftpInstance.isRunning + "</p>");

        sb.append("<p> current Buffer list length = " + serviceInstance.getBUFFER_LIST().size() + "</p>");
        sb.append("<p> current Worker list length = " + serviceInstance.getWORKER_LIST().size() + "</p>");
        sb.append("<p> current Error list length = " + serviceInstance.getERROR_LIST().size() + "</p>");
        sb.append("<p> current Done list length = " + serviceInstance.getDONE_LIST().size() + "</p>");

        sb.append("<h3> BUFFER LIST: </h3>");
        int i = 0;
        for (QueryRequest r: serviceInstance.getBUFFER_LIST()) {
            sb.append("<p> | ---------------------------------------------------------------------<br>");
            sb.append(" | Request-ID : " + r.getRequestedByID() + "<br>");
            sb.append(" | Map Name :" + r.getMapName() +"<br>");
            sb.append(" | ----------------------------------------------------------------------<br>");
            sb.append(" | - Position : " + i +"<br>");
            sb.append(" | - Date : " + r.getDate() +"<br>");
            sb.append(" | - Coords : <br>" + r.getPrintableCoordsString() +"<br>");
            sb.append(" | ---------------------------------------------------------------------</p>");
            i++;
        }

        sb.append("<h3> WORKER LIST:</h3>");
        i = 0;
        for (QueryRequest r: serviceInstance.getWORKER_LIST()) {
            sb.append("<p> | ---------------------------------------------------------------------<br>");
            sb.append(" | Request-ID : " + r.getRequestedByID() + "<br>");
            sb.append(" | Map Name : " + r.getMapName()  +"<br>");
            sb.append(" | ----------------------------------------------------------------------<br>");;
            sb.append(" | - Position : " + i  +"<br>");
            sb.append(" | - Date : " + r.getDate()  +"<br>");
            sb.append(" | - Coords : <br>" + r.getPrintableCoordsString()  +"<br>");
            sb.append(" | - Status : " + r.getStatus().toString()  +"<br>");
            sb.append(" | ---------------------------------------------------------------------</p>");
            i++;
        }

        sb.append("<h3> ERROR LIST:</h3>");
        i = 0;
        for (QueryRequest r: serviceInstance.getERROR_LIST()) {
            sb.append("<p> | ---------------------------------------------------------------------<br>");
            sb.append(" | - Request-ID : " + r.getRequestedByID() + "<br>");
            sb.append(" | - ErrorMessage : " + r.getErrorMessage()  +"<br>");
            sb.append(" | - Date : " + r.getDate()  +"<br>");
            sb.append(" | - Coords : <br>" + r.getPrintableCoordsString()  +"<br>");
            sb.append(" | ---------------------------------------------------------------------</p>");
            i++;
        }

        sb.append("<h3> DONE LIST:</h3>");
        i = 0;
        for (QueryRequest r: serviceInstance.getDONE_LIST()) {
            sb.append("<p> | ---------------------------------------------------------------------<br>");
            sb.append(" | Request-ID : " + r.getRequestedByID() + "<br>");
            sb.append(" | Map Name : " + r.getMapName() + "<br>");
            sb.append(" | ----------------------------------------------------------------------<br>");
            sb.append(" | - Date : " + r.getDate() + "<br>");
            sb.append(" | - Coords : \n" + r.getPrintableCoordsString() + "<br>");
            // sb.append(" | - Link to download .map file : <a href=\"sftp:"+ StaticVariables.standardUserName + ":" + StaticVariables.standardUserPassword + "@141.45.146.200:5002/" + r.getMapName()+ ".map\">direct link</a> <br>");
            sb.append(" | ---------------------------------------------------------------------</p>");
            i++;
        }

        return sb.toString();
    }

    @RequestMapping(value = "/request")
    //10.12345 45.3132, 10.9 55.34534535, 15.4646456 55, 15 44.3535365, 10.12345 45.3132
    public String request(@RequestParam(value = "name", defaultValue = "testRequest") String mapname,
                          @RequestParam(value = "coords", defaultValue = "10,45_10,55_15,55_15,45_10,45") String coords,
                          @RequestParam(value = "date", defaultValue = "2016-12-31") String date) {

        List<Coords> coordinates = new ArrayList<>();
        QueryRequest q = null;
        try {
            String[] coordsS = coords.split("_");
            float x;
            float y;

            try {
                for (String s : coordsS) {
                    x = Float.parseFloat(s.split(",")[0]);
                    y = Float.parseFloat(s.split(",")[1]);
                    coordinates.add(new Coords(x, y));
                }
            } catch (Exception e) {
                Logger.instance.addLogEntry(ERROR, TAG, e.toString());
            }

            // TODO : add id system
            q = new QueryRequest(coordinates, date, mapname, (int) CustomPRNG.random(), StaticVariables.osmDir, StaticVariables.mapDir, StaticVariables.renderingParameterFilePath, StaticVariables.ohdmConverterFilePath, StaticVariables.javaJdkPath, StaticVariables.jdbcDriverFilePath);

            Logger.instance.addLogEntry(INFO, TAG,"given Data: " + mapname + " | Date: " + date + " | coords: \n" + q.getPrintableCoordsString());

            if (!coordinates.get(0).toString().equals(coordinates.get(coordinates.size() - 1).toString())) {
                Logger.instance.addLogEntry(INFO, TAG, "fixing coordinates of " + q.getMapName());
                coordinates.add(coordinates.get(0));
            }

            // coord_string = str([str(x) for x in coordinates]).replace("'", "").replace("[", "").replace("]", "")

            q.setMapName(sanitize_mapName(mapname));

            Logger.instance.addLogEntry(INFO, TAG, "sanitized mapName: " + q.getMapName());

            if (map_exist(q.getMapName()))
                return "Map already exists";

            //if (serviceInstance.queueAlreadyContainsMapWithGivenName(q.getMapName()))
            //return "Map is already being created";

            // TODO: check if map was already requested, but an error was thrown
            /*if queue_map(coord_string, date, mapname) == "Error";
            return "Error while requesting map", 500*/

            serviceInstance.add(q);

            //TODO: add id system
            return String.valueOf(q.getRequestedByID());

            //except ValueError as e:
            //# abort(400)
            //raise e
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequestMapping("/test")
    public String test() {
        return ("<head> <title> Running ? </title> </head> <body> <h1> Am I running? </h1> <p> i guess i do </p> </body>");
    }

    @RequestMapping("/log")
    public String log(@RequestParam(value = "log", defaultValue = "")String selectedLog) {
        BufferedReader br;
        if (selectedLog.equals("daemon")) {
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(new File("daemonLog.txt"))));
            } catch (FileNotFoundException e) {
                return "couldn't read logger";
            }
        } else {
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(Logger.instance.currentWritingFile)));
            } catch (FileNotFoundException e) {
                return "couldn't read logger | " + Logger.instance.currentWritingFile;
            }
        }

        String output = "";
        try {
            while (br.ready())
                output += br.readLine() + "<br>";
        } catch (IOException e) {
            return e.getMessage();
        }

        return output;
    }

    @RequestMapping("/workingStatus")
    public String workingStatis(@RequestParam(value = "id", defaultValue = "") String id) {
        if (id.equals(""))
            return "gives back everything with status";
        else {
            //String[][] outputArray = getInformations(id);
            //return new Gson().toJson(outputArray);
            return "gives back specific status things with id " + id;
        }
    }

    /**
     * performs sanitization on Map-name. allows only the characters in the alphabet, drops anything else.
     *
     * @param nme map Name
     * @return sanitized value or "noname" if no char is left
     */
    public String sanitize_mapName(String nme) {
        String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-";

        if (nme.endsWith("osm"))
            nme = nme.split("osm")[0];

        char[] alSplit = alphabet.toCharArray();
        char[] sToTest = nme.toCharArray();

        StringBuilder cleanName = new StringBuilder();

        for (char s : sToTest) {
            boolean usableChar = false;
            for (char al : alSplit) {
                if (s == al)
                    usableChar = true;
            }
            if (usableChar)
                cleanName.append(s);
        }

        if (cleanName.toString().equals(""))
            cleanName.append("noname");

        return cleanName.toString().trim();
    }

    /**
     * checks if the map already exists ( is searching in std. OSM_DIR and MAP_DIR )
     *
     * @param nme map Name
     * @return true if existent, false if not
     */
    public boolean map_exist(String nme) {
        File fOSM = new File(StaticVariables.osmDir + nme);
        File fMAP = new File(StaticVariables.mapDir + nme);

        if (!(fOSM.exists() && !fOSM.isDirectory()))
            if (!(fMAP.exists() && !fOSM.isDirectory()))
                return false;

            else return true;
        else return true;

    }
}
