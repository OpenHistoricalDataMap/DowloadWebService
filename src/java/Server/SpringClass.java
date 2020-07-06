package Server;

import Server.ControllerEndpoint.ControllerEndpoint;
import Server.CustomObjects.Coords;
import Server.CustomObjects.LogEntry;
import Server.CustomObjects.QueryRequest;
import Server.FileService.SFTPService.SftpService;
import Server.IDService.IDSystem;
import Server.LogService.Logger;
import Server.RequestService.RequestService;
import com.google.gson.Gson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static Server.CustomObjects.LogType.*;

@SpringBootApplication
@RestController
public class SpringClass {

    private static final String initFile = "init.txt";

    public static RequestService serviceInstance;
    private static Thread serviceThread;

    // depricated
    // private static FTPService ftpInstance;
    // private static Thread ftpThread;

    public static SftpService sftpInstance;
    public static Thread sftpThread;

    private static ApplicationContext cntxt;

    private static final String TAG = "Spring-Thread";

    private static SpringClass instance;

    /**
     * setup for initial start and full restarts
     * @throws IOException IOException
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

        /*------------------ new Services and Systems need to be established after the Logger!!!!! -------------------*/

        // set up for ID System
        // this isn't really a System of some sort, it just saves, reads and creates IDs
        // everything in it is static and it's no Thread/Runnable
        IDSystem.setIDSaveFile(new File(StaticVariables.idSavePath + "idSave.txt"));

        // the service instance, the Controller for all the Requests, is in charge of activating and managing requests
        // you could actually call it an "Controller" of some sorts... but I didn't bother changing the name
        // TODO: maybe later
        serviceInstance = new RequestService();
        serviceThread = serviceInstance;
        serviceThread.start();

        // the ftp service, which allows the Android App to download the .map files
        // !! deprecated !!
        /*ftpInstance = new FTPService(StaticVariables.ftpPort, StaticVariables.ftpServiceUserPropertiesFile,  StaticVariables.standardUserName, StaticVariables.standardUserPassword, StaticVariables.ftpServiceMapDir, StaticVariables.ftpDefaultDir);
        ftpThread = new Thread(ftpInstance);
        ftpThread.start();*/

        // the sftp service, which allows the Android App to download the .map files
        sftpInstance = new SftpService(StaticVariables.sftpPort, StaticVariables.standardUserName, StaticVariables.standardUserPassword, StaticVariables.sftpDefaultKeyFile, StaticVariables.sftpServiceMapDir, StaticVariables.msgPath);
        sftpThread = sftpInstance;
        sftpThread.start();

        // filling up the Request Manager with file-saved Requests from the req-File-Directory
        fillRequestManager();
    }

    private static void fillRequestManager() {
        //List of all files and directories
        File filesList[] = new File(StaticVariables.specificLogDir).listFiles();
        for(File file : filesList) {
            QueryRequest tmp = new QueryRequest().QueryRequest(file.getPath().replace("/" + file.getName(), ""), file.getName().replace(".req", ""));
            if (tmp.getMapName() != null) {
                serviceInstance.add(tmp);
                Logger.instance.addLogEntry(INFO, TAG, "added " + tmp.getMapName() + " to the Request Manager");
            }
        }
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

    public static String reloadRequestService() throws IOException {
        // Method to read, initialize and define all ll 
        StaticVariables.init();
        StaticVariables.createStdFilesAndDirs();
        ArrayList<QueryRequest> bufferSave = serviceInstance.getBUFFER_LIST();
        serviceInstance.stopThread();
        try {
            serviceThread.join();
        } catch (InterruptedException e) {
            return "request service restart interrupted, please try again";
        }
        serviceThread = null;
        serviceInstance = null;

        serviceInstance = new RequestService(bufferSave, 5);
        serviceThread = serviceInstance;
        serviceThread.start();
        return "webservice reloaded | saved pending Requests in Buffer = " + bufferSave.size();
    }

    public static String reloadLog() throws IOException {
        StaticVariables.init();
        StaticVariables.createStdFilesAndDirs();
        Logger.instance.stopThread();
        try {
            Logger.instance.join();
        } catch (InterruptedException e) {
            return "logger restart interrupted, please try again";
        }
        List<LogEntry> bufferSave = Logger.instance.getBUFFER_LIST();
        Logger.instance = null;
        new Logger(StaticVariables.logDefaultDir, StaticVariables.maxLogFileSize, StaticVariables.logTerminalOutput);
        Logger.instance.setBUFFER_LIST(bufferSave);
        Logger.instance.start();

        return "Logger reloaded | saved pending LogEntries in Buffer = " + bufferSave.size();
    }

    public static String restart() throws IOException {
        String _return = "";
        _return += reloadLog() + "\n";
        Logger.instance.addLogEntry(DEBUG, TAG, "restarted Logger");
        _return += reloadID() + "\n";
        Logger.instance.addLogEntry(DEBUG, TAG, "restarted ID System");
        _return += reloadRequestService() + "\n";
        Logger.instance.addLogEntry(DEBUG, TAG, "restarted webService");
        SpringApplication.exit(cntxt, () -> 0);

        System.getProperties().put("server.port", StaticVariables.webPort);
        cntxt = SpringApplication.run(SpringClass.class);
        return _return + "\nrestarted service -  look into daemon-log to see init";
    }

    public static String reloadID() throws IOException {
        StaticVariables.init();
        StaticVariables.createStdFilesAndDirs();
        IDSystem.setIDSaveFile(new File(StaticVariables.idSavePath + "idSave.txt"));
        return "loaded new ID File";
    }

    public static String clearBuffer() {
        return serviceInstance.resetBuffer();
    }

    public static String cleanDone() {
        return serviceInstance.resetDone();
    }

    public static String cleanError() {
        return serviceInstance.resetError();
    }

    @RequestMapping("/")
    public static String webServiceStatus() {

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' HH'h'-mm'm'-ss's'");

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
            sb.append(" | - Coords : " + r.getPrintableCoordsString() +"<br>");
            sb.append(" | - Request Time : " + r.getRequestTime() + "<br>");
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
            sb.append(" | - Coords : " + r.getPrintableCoordsString()  +"<br>");
            sb.append(" | - Status : " + r.getStatus().toString()  +"<br>");
            sb.append(" | - Time passed since start " + StaticVariables.formatDateTimeDif(r.getRuntimeStart(), LocalDateTime.now()) + "<br>");
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
            sb.append(" | - Coords : " + r.getPrintableCoordsString()  +"<br>");
            sb.append(" | - Error after : " + StaticVariables.formatDateTimeDif(r.getRequestTime(), r.getEndTime()) + "<br>");
            sb.append(" | - Timestamp when error occurred : " + timeFormatter.format(r.getEndTime()) + " <br>");
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
            sb.append(" | - Request time : " + timeFormatter.format(r.getRequestTime()) + "<br>");
            sb.append(" | - Start time " + timeFormatter.format(r.getRuntimeStart()) + "<br>");
            sb.append(" | - Runtime : " + StaticVariables.formatDateTimeDif(r.getRequestTime(), r.getEndTime()) + "<br>");
            // sb.append(" | - Link to download .map file : <a href=\"sftp:"+ StaticVariables.standardUserName + ":" + StaticVariables.standardUserPassword + "@141.45.146.200:5002/" + r.getMapName()+ ".map\">direct link</a> <br>");
            sb.append(" | ---------------------------------------------------------------------</p>");
            i++;
        }

        return sb.toString();
    }

    @CrossOrigin(origins = "http://141.45.146.200:8080")
    @RequestMapping(value = "/request")
    //10.12345 45.3132, 10.9 55.34534535, 15.4646456 55, 15 44.3535365, 10.12345 45.3132
    public static String request(@RequestParam(value = "name", defaultValue = "testRequest") String mapname,
                          @RequestParam(value = "coords", defaultValue = "13.,52_14,52_14,53_13,53_13,52") String coords,
                          @RequestParam(value = "date", defaultValue = "2016-12-31") String date,
                          @RequestParam(value = "id", defaultValue = "") String id) {

        if (id.trim().equals("")) {
            try {
                id = IDSystem.createNewID();
            } catch (IOException e) {
                Logger.instance.addLogEntry(INFO, TAG, "couldn't save " + id);
            }
        }

        try {
            if (!IDSystem.idAlreadyExists(id))
                IDSystem.writeEntry(id);
        } catch (IOException e) {
            e.printStackTrace();
        }


        List<Coords> coordinates = new ArrayList<>();
        QueryRequest q;
        try {
            String[] coordsS = coords.split("_");
            float x;
            float y;

            try {
                for (String s : coordsS) {
                    String[] split = s.split(",");

                    x = Float.parseFloat(split[0].trim());
                    y = Float.parseFloat(split[1].trim());

                    System.out.println(x + " , " + y + "\n");

                    coordinates.add(new Coords(x, y));
                }
            } catch (NumberFormatException e) {
                Logger.instance.addLogEntry(ERROR, TAG, e.toString());
                return "couldn't convert coords";
            }

            // TODO : add id system
            q = new QueryRequest(coordinates, date, mapname, id, StaticVariables.osmDir, StaticVariables.mapDir, StaticVariables.specificLogDir, StaticVariables.renderingParameterFilePath, StaticVariables.ohdmConverterFilePath, StaticVariables.javaJdkPath, StaticVariables.jdbcDriverFilePath);

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

            // TODO: add request id reference for instant status review
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
    public static String test() {
        return ("<head> <title> Running ? </title> </head> <body> <h1> Am I running? </h1> <p> i guess i do </p> </body>");
    }

    @RequestMapping("/log")
    public static String log(@RequestParam(value = "log", defaultValue = "")String selectedLog) {
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

    @CrossOrigin(origins = "http://141.45.146.200:8080")
    @RequestMapping("/statusByID")
    public static String statusByID(@RequestParam(value = "id", defaultValue = "") String id) {
        if (id.equals("")) {
            String[] all = new String[serviceInstance.getAllRequests().length];

            for (int i = 0; i < all.length; i++) {
                try {
                    all[i] = serviceInstance.getAllRequests()[i].json();
                } catch (NullPointerException e) {
                    System.out.println(all.length);
                    e.printStackTrace();
                }
            }

            String out = "[";
            for (int i = 0; i < all.length; i++) {
                out += all[i]; if (i < all.length - 1) out += ",";
            }
            out += "]";

            Logger.instance.addLogEntry(INFO, TAG, "new general status request : \n" + out);

            return out;
        } else {
            QueryRequest[] allRequests = serviceInstance.getAllRequests();
            ArrayList<QueryRequest> all_withId = new ArrayList<>();

            for (QueryRequest r :
                    allRequests) {
                if (r.getRequestedByID().equals(id))
                    all_withId.add(r);
            }

            String[] all = new String[all_withId.size()];

            for (int i = 0; i < all.length; i++) {
                all[i] = all_withId.get(i).toString();
            }

            String out = new Gson().toJson(all);
            Logger.instance.addLogEntry(INFO, TAG, "new status request by ID : \n" + "id : " + id + "\n output : " + out);
            return out;
        }
    }

    @RequestMapping("/id")
    public static String id() {
        try {
            String id = IDSystem.createNewID();
            Logger.instance.addLogEntry(INFO, TAG, "requested new ID : " + id);
            return id;
        } catch (IOException e) {
            e.printStackTrace();
            return "couldn't create new ID";
        }
    }

    //@CrossOrigin(origins = "http://141.45.146.200:8080")
    @RequestMapping("/maps")
    public static Object mapsDownload(@RequestParam(value = "name", defaultValue = "")String name) {
        QueryRequest requestAskedFor = null;
        if (name.equals(""))
            return "no name requested";

        for (QueryRequest r :
                serviceInstance.getAllRequests()) {
            if (r.getMapName().equals(name))
                requestAskedFor = r;
        }

        if (requestAskedFor == null) {
            Logger.instance.addLogEntry(INFO, TAG, "download http request : " + name + " (not found)");
            return "no request found with name : " + name;
        }

        else {
            File file = new File(StaticVariables.mapDir + "/" + requestAskedFor.getMapName() + ".map");
            if (!file.exists()) {
                Logger.instance.addLogEntry(INFO, TAG, "download http request : " + name + " (no file found)");
                return "no file found for given request " + name;
            }

            Path path = Paths.get(file.getAbsolutePath());
            ByteArrayResource resource = null;
            try {
                resource = new ByteArrayResource(Files.readAllBytes(path));
            } catch (IOException e) {
                e.printStackTrace();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add("Access-Control-Allow-Origin", "*");

            Logger.instance.addLogEntry(INFO, TAG, "download http request : " + name + " (successful)");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(resource);
        }
    }

    /**
     * performs sanitization on Map-name. allows only the characters in the alphabet, drops anything else.
     *
     * @param nme map Name
     * @return sanitized value or "noname" if no char is left
     */
    public static String sanitize_mapName(String nme) {
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
    public static boolean map_exist(String nme) {
        File fOSM = new File(StaticVariables.osmDir + nme);
        File fMAP = new File(StaticVariables.mapDir + nme);

        if (!(fOSM.exists() && !fOSM.isDirectory()))
            if (!(fMAP.exists() && !fOSM.isDirectory()))
                return false;

            else return true;
        else return true;

    }
}
