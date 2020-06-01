package Server.CustomObjects;

import Server.LogService.Logger;

import java.io.*;
import java.util.List;

import static Server.StaticVariables.mapDir;
import static Server.StaticVariables.osmDir;

public class QueryRequest extends Thread {

    private List<Coords> coordinates;
    private String date;
    private String mapName;
    private int requestedByID;

    private QueryRequestStatus status = QueryRequestStatus.REQUESTED;

    private String errorMessage = "";

    private String TAG;

    public QueryRequest(List<Coords> coordinates, String date, String mapName, int id) {
        this.coordinates = coordinates;
        this.date = date;
        this.mapName = mapName;
        this.requestedByID = id;
        TAG = mapName + "-Thread";
    }

    public Coords[] getCoordinates() {
        return coordinates.toArray(new Coords[coordinates.size()]);
    }
    public String getDate() {
        return date;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public String getMapName() {
        return mapName;
    }
    public String getPrintableCoordsString() {
        StringBuilder sb = new StringBuilder();
        for (Coords c : coordinates) {
            sb.append("x = " + c.x + " | y = " + c.y + "\n");
        }
        return sb.toString();
    }
    public QueryRequestStatus getStatus() {
        return status;
    }

    public int getRequestedByID() {
        return requestedByID;
    }
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * calls a script to convert an .osm to a .map file.
     *
     * @return 0 on success, -1 if file with same name already exists, anything else on failure.
     */
    private CommandReturn convert_map() {
        String[] template = new String[]{"/opt/osmosis/bin/osmosis", "--rx", "file=" + osmDir + "/" + mapName + ".osm", "--mw", "file=" + mapDir + "/" + mapName + ".map"};

        String tmpl = "";
        for (String s:
                template) {
            tmpl += s + " ";
        }

        Logger.instance.addLogEntry(LogType.INFO, TAG,"running: " + tmpl);

        CommandReturn result;

        result = excCommand(template);
        Logger.instance.addLogEntry(LogType.INFO, TAG,"output: " + result.output);
        Logger.instance.addLogEntry(LogType.ERROR, TAG,"error: " + result.errOutput);
        Logger.instance.addLogEntry(LogType.INFO, TAG,"return code: " + result.returnCode);

        return result;
    }

    /**
     * calls a script to generate an .osm Map-file with given coordinates as boundaries for a specified date.
     *
     * @return 0 on success, anything else on failure.
     */
    private CommandReturn download_map() throws Exception {
        String[] template = new String[]{"java", "-jar", "OHDMConverter.jar", "-o", osmDir + "/" + mapName + ".osm", "-r", "db_rendering.txt", "-p", rearrangeCoordsForScript(getCoordinates()), "-t", date};

        String tmpl = "";
        for (String s:
             template) {
            tmpl += s + " ";
        }

        Logger.instance.addLogEntry(LogType.INFO, TAG,"running: " + tmpl);

        CommandReturn result;
        result = excCommand(template);

        return result;
    }

    @Override
    public void run() {
        try {
            //Logger.instance.addLogEntry(LogType.INFO, TAG,"started request : " + date + " | " + getPrintableCoordsString());
            status = QueryRequestStatus.DOWNLOADING;
            CommandReturn returnMessage = null;

            try {
                returnMessage = download_map();
            } catch (Exception e) {
                status = QueryRequestStatus.ERROR;
                errorMessage = "Error in DOWNLOADING: " + returnMessage.errOutput;
                return;
            }

            Logger.instance.addLogEntry(LogType.INFO, TAG, "OHDMConverter output: " + returnMessage.output + "\n");
            Logger.instance.addLogEntry(LogType.INFO, TAG, "OHDMConverter error: " + returnMessage.errOutput + "\n");
            Logger.instance.addLogEntry(LogType.INFO, TAG,"download done - now converting");
            status = QueryRequestStatus.CONVERTING;

            try {
                returnMessage = convert_map();
            } catch (Exception e) {
                status = QueryRequestStatus.ERROR;
                errorMessage = "Error in CONVERTING: " + returnMessage.errOutput;
                return;
            }

            if (returnMessage.returnCode != 0) {
                status = QueryRequestStatus.ERROR;
                if (returnMessage.returnCode == -1) {
                    errorMessage = "ERROR in CONVERTING: File already exists. not overwriting it\n";
                    Logger.instance.addLogEntry(LogType.ERROR, TAG, errorMessage);
                    return;
                }
                if (returnMessage.returnCode == 2) {
                    errorMessage = "ERROR in CONVERTING: couldn't find File to convert";
                    Logger.instance.addLogEntry(LogType.ERROR, TAG, errorMessage);
                    return;
                }
            }

            status = QueryRequestStatus.DONE;
        } catch (Exception e) {
            status = QueryRequestStatus.ERROR;
            errorMessage = e.getMessage();
            return;
        }
    }

    public synchronized CommandReturn excCommand(String[] command) {
        Runtime rt = Runtime.getRuntime();
        try {
            String output = "";
            String error = "";
            String readBuffer = "";

            Process p = null;
            try {
                p = rt.exec(command);
            } catch (IOException e) {
                String stack = "";
                for (int i = 0; i < e.getStackTrace().length; i++) {
                    stack += e.getStackTrace()[i].toString() + "\n";
                }
                Logger.instance.addLogEntry(LogType.ERROR, TAG, e.getMessage() + "\n" + stack);
            }
            p.waitFor();

            // read Process output
            InputStream is = p.getInputStream();
            BufferedInputStream buffer = new BufferedInputStream(is);
            BufferedReader commandResult = new BufferedReader(new InputStreamReader(buffer));

            try {
                while ((readBuffer = commandResult.readLine()) != null) {
                    output += readBuffer + "\n";
                }
            } catch (Exception e) {
                String stack = "";
                for (int i = 0; i < e.getStackTrace().length; i++) {
                    stack += e.getStackTrace()[i].toString() + "\n";
                }
                Logger.instance.addLogEntry(LogType.ERROR, TAG, stack);
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

            return new CommandReturn(p.exitValue(), output, error, p);

        } catch (InterruptedException e) {
            String stack = "";
            for (int i = 0; i < e.getStackTrace().length; i++) {
                stack += e.getStackTrace()[i].toString() + "\n";
            }
            Logger.instance.addLogEntry(LogType.ERROR, TAG, stack);
        }
        return null;
    }
    private static String rearrangeCoordsForScript(Coords[] coords) {
        StringBuilder coordsString = new StringBuilder();
        coordsString.append("POLYGON((");
        for (int i = 0; i < coords.length; i++) {
            coordsString.append(coords[i].x.toString() + ' ' + coords[i].y.toString());
            if (i < coords.length-1) {
                coordsString.append(", ");
            }
        }
        coordsString.append("))");
        return coordsString.toString();
    }
}
