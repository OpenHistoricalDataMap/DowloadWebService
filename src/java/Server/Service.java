package Server;

import Server.CustomObjects.CommandReturn;
import Server.CustomObjects.Coords;
import Server.CustomObjects.QueueRequest;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static Server.StaticVariables.*;

public class Service implements Runnable, srvInterface{
    // this is currently not in use
    //private String LISTEN_IP = "0.0.0.0"; // "::" ipv6 , "0.0.0.0" ipv4
    //private int LISTEN_PORT = 5000;

    //fetching DirStrings from StaticVariables Class
    private String OSM_DIR = osmDir;
    private String MAP_DIR = mapDir;
    private String OHDM_DIR = ohdmDir;

    private boolean LOGGING = doesWebServiceLog;

    // fetching ftpLogFile from StaticVariables Class
    private File logFile = new File(webServiceLogFile);
    private PrintStream logStream;

    private String LOG_TAG = "WebService";

    List<QueueRequest> WORK_QUEUE = new ArrayList<>();

    boolean watcherWorking;
    boolean serviceRunning;

    public void run() {
        try {
            logStream = new PrintStream(new FileOutputStream(logFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        fixFolders();
        watch();
    }

    public synchronized void watch() {
        serviceRunning = true;
        try {
            while (true) {
                if (!WORK_QUEUE.isEmpty()) {
                    watcherWorking = true;
                    QueueRequest content = WORK_QUEUE.get(WORK_QUEUE.size() - 1);
                    Coords[] coords = content.getCoordinates();
                    String date = content.getDate();
                    String mapName = content.getMapName();

                    log("Downloading mapName = " + mapName + " | Date = " + date + " | Coordinates = " + coords);

                    if (download_map(coords, date, mapName) != 0) {
                        String msg = "ERROR: Couldn't download map : " + mapName;
                        System.err.println(msg);
                        log(msg);
                    }

                    if (convert_map(mapName) != 0) {
                        String msg = "ERROR: Couldn't convert map : " + mapName;
                        System.err.println(msg);
                        log(msg);
                    }
                } else {
                    watcherWorking = false;

                    try {
                        wait();
                    } catch (InterruptedException e) {
                        String msg = "new Request found";
                        System.out.println(msg);
                        log(msg);
                    }
                }
            }
        } catch (Exception e) {
            serviceRunning = false;
        }
    }

    public void fixFolders() {
        createFolderIfMissing(OSM_DIR);
        createFolderIfMissing(MAP_DIR);
    }

    private static void createFolderIfMissing(String folderName) {
        new File(folderName).mkdirs();
    }

    public void log(String msg) {
        if (!LOGGING)
            return;

        if (logStream == null)
            throw new NullPointerException("ERROR: PrintStream for webServiceLog not initialized");

        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date date = new Date(System.currentTimeMillis());
        logStream.println(LOG_TAG + " : " + formatter.format(date) + " | " + msg);
    }

    public int download_map(Coords[] coords, String date, String mapName) {
        // java -jar OHDMConverter.jar -r rendering_parameter -p "POLYGON((10.12345 45.3132, 10.9 55.34534535, 15.4646456 55, 15 44.3535365, 10.12345 45.3132))" -t "2016-12-31" -o output.osm

        /*
        template = ['java', '-jar', f'{OHDM_DIR}OHDMConverter.jar', '-r', 'db_inter.txt', '-p', f'POLYGON(({coords}))',
                '-t', f'{date}', '-o', f'{OSM_DIR}{mapname}.osm']
        */

        String[] template = {"java", "-jar", OHDM_DIR, "OHDMConverter.jar", "-r", "./db_inter.txt", "-p",
                "\"POLYGON((" + rearrangeCoordsForScript(coords) + "))", "-t", "\"" + date + "\"",
                "-o", OSM_DIR + mapName + ".osm"};

        log("running: " + template);

        CommandReturn result = null;
        try {
            result = excCommand(template);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        int result = run(template, stdout=PIPE, stderr=PIPE, encoding='utf-8')
        log("stdout: " + result.output);
        log("stderr: " + result.errOutput);
        return result.returnCode;
    }

    private String rearrangeCoordsForScript(Coords[] coords) {
        StringBuilder coordsString = new StringBuilder();
        for (int i = 0; i < coords.length; i++) {
            coordsString.append(coords[i]);
            if (i < coords.length-1)
                coordsString.append(", ");
        }
        return coordsString.toString();
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

    public int convert_map(String nme) {
        String[] template = {"./convert-osm2map.sh", OSM_DIR + nme + ".osm", MAP_DIR + nme + ".map"};
        log("running: " + template);
        CommandReturn result = null;
        try {
            result = excCommand(template);
        } catch (Exception e) {
            e.printStackTrace();
        }
        log("output: " + result.output);
        log("error: " + result.errOutput);
        log("return code: " + result.returnCode);

        if (result.output.endsWith("already exists. not overwriting it\n"))
            return -1;

        if (result.output.endsWith("does not exist\n"))
            return 2;

        return result.returnCode;
    }

    /**
     * checks if the map already exists ( is searching in std. OSM_DIR and MAP_DIR )
     *
     * @param nme map Name
     * @return true if existent, false if not
     */
    public boolean map_exist(String nme) {
        File fOSM = new File(OSM_DIR + nme);
        File fMAP = new File(MAP_DIR + nme);

        if (!(fOSM.exists() && !fOSM.isDirectory()))
            if (!(fMAP.exists() && !fOSM.isDirectory()))
                return false;

            else return true;
        else return true;

    }

    public CommandReturn excCommand(String[] command) throws Exception {
        Runtime rt = Runtime.getRuntime();
        try {
            String output = "";
            String error = "";
            String readBuffer = "";

            Process p = rt.exec(command);
            System.out.println(p.isAlive());

            // read Process output
            InputStream is = p.getInputStream();
            BufferedInputStream buffer = new BufferedInputStream(is);
            BufferedReader commandResult = new BufferedReader(new InputStreamReader(buffer));

            try {
                while ((readBuffer = commandResult.readLine()) != null) {
                    output += readBuffer + "\n";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // read Process err
            InputStream isErr = p.getErrorStream();
            BufferedInputStream bufferErr = new BufferedInputStream(isErr);
            BufferedReader commandResultErr = new BufferedReader(new InputStreamReader(bufferErr));

            readBuffer = "";

            try {
                while ((readBuffer = commandResultErr.readLine()) != null) {
                    error += readBuffer + "\n";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!output.equals(""))
                return new CommandReturn(p.exitValue(), output, error, p);

            else
                throw new Exception("Couldn't execute command");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public boolean queueAlreadyContainsMapWithGivenName(String nme) {
        for(int i = 0 ; i < WORK_QUEUE.size() ; i++) {
            if (WORK_QUEUE.get(i).getMapName().equals(nme))
                return true;
        }
        return false;
    }
}
