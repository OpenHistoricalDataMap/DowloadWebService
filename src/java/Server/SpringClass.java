package Server;

import Server.CustomObjects.Coords;
import Server.CustomObjects.QueryRequest;
import Server.FTPService.FTPService;
import Server.LogService.Logger;
import Server.WebService.ServiceNew;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static Server.CustomObjects.LogType.ERROR;
import static Server.CustomObjects.LogType.INFO;
import static Server.StaticVariables.*;

@SpringBootApplication
@RestController
public class SpringClass {

    private static ServiceNew serviceInstance;
    private static Thread serviceThread;

    private static FTPService ftpInstance;
    private static Thread ftpThread;

    private String TAG = "Spring-Thread";

    public static void main(String[] args) throws IOException {

        // just standard inits for the Variables used in this Project
        // most of them are read from the init.txt
        // but more of that in the OHDM wiki
        StaticVariables.init();
        StaticVariables.createStdFilesAndDirs();

        // Logger is a singleton Class due to the availability need
        // the Logger is a System in itself
        new Logger();
        Logger.instance.start();

        // the service instance, which just goes through a couple of lists and sets things to what they are
        // you could actually call it an "Controller" of some sorts... but I didn't bother changing the name
        // TODO: maybe later
        serviceInstance = new ServiceNew();
        serviceThread = new Thread(serviceInstance);
        serviceThread.start();

        // the ftp service, which allows the Android App to download the .map files
        ftpInstance = new FTPService();
        ftpThread = new Thread(ftpInstance);
        ftpThread.start();

        // and here starts the Spring Application with the server port set to the
        // before given Port in StaticVariables
        System.getProperties().put("server.port", StaticVariables.webPort);
        SpringApplication.run(SpringClass.class, args);
    }

    @RequestMapping("/")
    public String webServiceStatus() {

        StringBuilder sb = new StringBuilder();
        sb.append("<head> <title> WebService status</title> </head>");
        sb.append("<p> Service Running = " + serviceInstance.isRunning() + "</p>");
        sb.append("<p> WatcherThread currently working = " + serviceInstance.isActive() + "</p>");
        sb.append("<p> current Buffer list length = " + serviceInstance.getBUFFER_LIST().size() + "</p>");
        sb.append("<p> current Worker list length = " + serviceInstance.getWORKER_LIST().size() + "</p>");
        sb.append("<p> current Error list length = " + serviceInstance.getERROR_LIST().size() + "</p>");
        sb.append("<p> current Done list length = " + serviceInstance.getDONE_LIST().size() + "</p>");

        sb.append("<h3> BUFFER LIST: </h3>");
        int i = 0;
        for (QueryRequest r: serviceInstance.getBUFFER_LIST()) {
            sb.append("<p> | ---------------------------------------------------------------------<br>");
            sb.append(" | Map Name :" + r.getMapName() +"<br>");
            sb.append(" | ----------------------------------------------------------------------" +"<br>");
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
            sb.append(" | Map Name : " + r.getMapName()  +"<br>");
            sb.append(" | ----------------------------------------------------------------------" +"<br>");;
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
            sb.append("<p> | ---------------------------------------------------------------------" + "<br>");
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
            sb.append(" | Map Name : " + r.getMapName() + "<br>");
            sb.append(" | ----------------------------------------------------------------------" +"<br>");
            sb.append(" | - Date : " + r.getDate() + "<br>");
            sb.append(" | - Coords : \n" + r.getPrintableCoordsString() + "<br>");
            sb.append(" | - Link to download .map file : <a href=\"ftp:"+ standardUserName + ":" +standardUserPassword + "@141.45.146.200:5000/" + r.getMapName()+ ".map\">direct link</a> <br>");
            sb.append(" | ---------------------------------------------------------------------</p>");
            i++;
        }

        return sb.toString();
    }

    @RequestMapping(value = "/request")
    public String request(@RequestParam(value = "name", defaultValue = "testRequest") String mapname,
                          @RequestParam(value = "coords", defaultValue = "10,45_10,55_15,55_15,45_10,45") String coords,
                          @RequestParam(value = "date", defaultValue = "2000-12-11") String date) {

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
            q = new QueryRequest(coordinates, date, mapname, 0000);

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
            return "Map " + q.getMapName() + ".map with data from " + q.getDate() + " will be created. Check back later!\n";

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
    public String log() {
        BufferedReader br;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Logger.instance.currentWritingFile))));
        } catch (FileNotFoundException e) {
            return "couldn't read logger";
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
        File fOSM = new File(osmDir + nme);
        File fMAP = new File(mapDir + nme);

        if (!(fOSM.exists() && !fOSM.isDirectory()))
            if (!(fMAP.exists() && !fOSM.isDirectory()))
                return false;

            else return true;
        else return true;

    }
}
