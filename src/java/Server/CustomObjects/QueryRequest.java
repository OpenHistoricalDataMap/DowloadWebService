package Server.CustomObjects;

import Server.LogService.Logger;
import Server.StaticVariables;
import com.google.gson.Gson;

import java.io.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Vector;

public class QueryRequest extends Thread {

    private final List<Coords> coordinates;
    private final String date;
    private String mapName;
    private final String requestedByID;

    private final LocalDateTime startTime = LocalDateTime.now();
    private LocalDateTime runtimeStart;
    private LocalDateTime endTime;

    private QueryRequestStatus status = QueryRequestStatus.REQUESTED;

    private String errorMessage = "";

    private final String TAG;
    private String LOG = "";

    private final String osmDir;
    private final String mapDir;
    private final String sLogDir;

    private final String renderingParameter;
    private final String ohdmConverter;

    private final String javaJdkPath;
    private final String jdbcDriverPath;

    private String individualLogFile;

    private boolean stopThread = false;

    public QueryRequest(List<Coords> coordinates, String date, String mapName, String id, String osmDir, String mapDir, String sLogDir, String renderingParameter, String ohdmConverter, String javaJdkPath, String jdbcDriverPath) {
        this.osmDir = osmDir;
        this.mapDir = mapDir;
        this.sLogDir = sLogDir;

        this.renderingParameter = renderingParameter;
        this.ohdmConverter = ohdmConverter;

        this.javaJdkPath = javaJdkPath;
        this.jdbcDriverPath = jdbcDriverPath;

        this.coordinates = coordinates;
        this.date = date;
        this.mapName = mapName;
        this.requestedByID = id;

        this.individualLogFile = sLogDir + "/" + mapName + ".req";

        TAG = mapName + "_" + getRequestedByID() + "-Thread";
    }

    public Coords[] getCoordinates() {
        return coordinates.toArray(new Coords[coordinates.size() - 1]);
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
            sb.append("x = ").append(c.x).append(" | y = ").append(c.y).append("\n");
        }
        return sb.toString();
    }
    public String getPrintableCoordsString(String div) {
        StringBuilder sb = new StringBuilder();
        for (Coords c : coordinates) {
            sb.append("x = ").append(c.x).append(" | y = ").append(c.y).append(div);
        }
        return sb.toString();
    }

    public QueryRequestStatus getStatus() {
        return status;
    }

    public String getRequestedByID() {
        return requestedByID;
    }
    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getRequestTime() {
        return startTime;
    }
    public LocalDateTime getRuntimeStart() {
        return runtimeStart;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }

    /**
     * calls a script to convert an .osm to a .map file.
     *
     * @return 0 on success, -1 if file with same name already exists, anything else on failure.
     */
    private CommandReturn convert_map() {
        String[] template = new String[]{"/opt/osmosis/bin/osmosis", "--rx", "file=" + osmDir + "/" + mapName + ".osm", "--mw", "file=" + mapDir + "/" + mapName + ".map"};

        StringBuilder tmpl = new StringBuilder();
        for (String s:
                template) {
            tmpl.append(s).append(" ");
        }

        Logger.instance.addLogEntry(LogType.INFO, TAG,"running: " + tmpl);

        CommandReturn result;

        result = excCommand(template);
        assert result != null;
        Logger.instance.addLogEntry(LogType.INFO, TAG,"OSM-2-MAP output: " + result.output);
        Logger.instance.addLogEntry(LogType.ERROR, TAG,"OSM-2-MAP error: " + result.errOutput);
        Logger.instance.addLogEntry(LogType.INFO, TAG,"OSM-2-MAP return code: " + result.returnCode);

        LOG += "CONVERT MAP : \noutput : " + result.output + "\n";
        LOG += "error : " + result.errOutput + "\n";
        LOG += "return code : "+result.returnCode + "\n\n";
        return result;
    }

    /*private String calcMiddle(Coords point1, Coords point2) {
        Double x2 = point2.x - point1.x;
        Double y2 = point2.y - point1.y;

        Double[] middlePoint = new Double[2];
        middlePoint[0] = x2 / 2;
        middlePoint[1] = y2 / 2;

        return (double) (middlePoint[0] + point1.x) + "," + (double) (middlePoint[1] + point2.y);
    }*/

    /**
     * calls a script to generate an .osm Map-file with given coordinates as boundaries for a specified date.
     *
     * @return 0 on success, anything else on failure.
     */
    private CommandReturn download_map() {
        String[] template = new String[]{javaJdkPath, "-classpath", jdbcDriverPath, "-jar", ohdmConverter, "-o",
                osmDir + "/" + mapName + ".osm", "-r", renderingParameter, "-p",
                rearrangeCoordsForScript(getCoordinates()), "-t", date};


        StringBuilder tmpl = new StringBuilder();
        for (String s :
                template) {
            tmpl.append(s).append(" ");
        }

        Logger.instance.addLogEntry(LogType.INFO, TAG, "running: " + tmpl);

        CommandReturn result;

        result = excCommand(template);

        assert result != null;
        Logger.instance.addLogEntry(LogType.INFO, TAG,"OHDMConverter output: " + result.output);
        Logger.instance.addLogEntry(LogType.ERROR, TAG,"OHDMConverter error: " + result.errOutput);
        Logger.instance.addLogEntry(LogType.INFO, TAG,"OHDMConverter return code: " + result.returnCode);

        LOG += "CONVERT MAP : \noutput : " + result.output + "\n";
        LOG += "error : " + result.errOutput + "\n";
        LOG += "return code : "+result.returnCode + "\n\n";

        return result;
    }

    @Override
    public void run() {
        runtimeStart = LocalDateTime.now();
        try {
            //Logger.instance.addLogEntry(LogType.INFO, TAG,"started request : " + date + " | " + getPrintableCoordsString());
            status = QueryRequestStatus.DOWNLOADING;
            CommandReturn returnMessage = new CommandReturn(0, "", "", null);

            try {
                returnMessage = download_map();
                if (stopThread) {
                    status = QueryRequestStatus.ERROR;
                    errorMessage = "Thread stopped manually";
                    return;
                }

                if (returnMessage.errOutput.contains("Exception")) {
                    errorMessage = returnMessage.errOutput;
                    status = QueryRequestStatus.ERROR;
                    endTime = LocalDateTime.now();
                    refreshIndivLogFile();
                    return;
                }
            } catch (Exception e) {
                errorMessage = "Error in DOWNLOADING: " + returnMessage.errOutput;
                endTime = LocalDateTime.now();
                return;
            } finally {
                refreshIndivLogFile();
            }

            Logger.instance.addLogEntry(LogType.INFO, TAG,"download done - now converting");
            status = QueryRequestStatus.CONVERTING;

            try {
                returnMessage = convert_map();
                if (stopThread) {
                    status = QueryRequestStatus.ERROR;
                    errorMessage = "Thread stopped manually";
                    return;
                }
            } catch (Exception e) {
                status = QueryRequestStatus.ERROR;
                errorMessage = "Error in CONVERTING: " + returnMessage.errOutput + e.getStackTrace()[0];
                endTime = LocalDateTime.now();
                refreshIndivLogFile();
                return;
            } finally {
                refreshIndivLogFile();
            }

            if (returnMessage.returnCode != 0) {
                status = QueryRequestStatus.ERROR;

                if (returnMessage.returnCode == -1) {
                    errorMessage = "ERROR in CONVERTING: File already exists. not overwriting it\n";
                    Logger.instance.addLogEntry(LogType.ERROR, TAG, errorMessage);
                    endTime = LocalDateTime.now();
                    refreshIndivLogFile();
                    return;
                }

                else if (returnMessage.returnCode == 2) {
                    errorMessage = "ERROR in CONVERTING: couldn't find File to convert";
                    Logger.instance.addLogEntry(LogType.ERROR, TAG, errorMessage);
                    endTime = LocalDateTime.now();
                    refreshIndivLogFile();
                    return;
                } else {
                    Logger.instance.addLogEntry(LogType.ERROR, TAG, "Error:" + returnMessage.errOutput + "\nOutput:" + returnMessage.output);
                    endTime = LocalDateTime.now();
                    refreshIndivLogFile();
                    return;
                }

            }

            refreshIndivLogFile();
            status = QueryRequestStatus.DONE;
            endTime = LocalDateTime.now();
        } catch (Exception e) {
            status = QueryRequestStatus.ERROR;
            Logger.instance.addLogEntry(LogType.ERROR, TAG, "Error:" + e.getStackTrace()[0]);
            errorMessage = "ERROR | " + e.getStackTrace()[0];
            endTime = LocalDateTime.now();
        } finally {
            try {
                refreshIndivLogFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized CommandReturn excCommand(String[] command) {
        Runtime rt = Runtime.getRuntime();
        try {
            String output = "";
            String error = "";
            String readBuffer;

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
            assert p != null;
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

    private void refreshIndivLogFile() throws IOException {
        if (!new File(individualLogFile).exists())
            new File(individualLogFile).createNewFile();

        String output = "";

        // indiv write
        output += "name : " + mapName + "\n";
        output += "coords : " + getPrintableCoordsString(" | ");
        output += "date : " + date + "\n";
        output += "id : " + requestedByID + "\n";
        output += "status : " + status + "\n";
        output += "-----------------------------------------------------------------------------------------------\n";
        output += "startTime : " + startTime.toString() + "\n";
        output += "runTimeStart : " + runtimeStart.toString() + "\n";
        if (endTime != null) { output += "endTime : " + endTime.toString() + "\n"; }
        output += "-----------------------------------------------------------------------------------------------\n";
        output += "tagInLog : " + TAG + "\n";
        output += "-----------------------------------------------------------------------------------------------\n";
        output += "osmFile : " + osmDir + "/" + mapName + ".osm" + "\n";
        output += "mapFile : " + mapDir + "/" + mapName + ".map" + "\n";
        output += "renderParamFile : " + renderingParameter + "\n";
        output += "OHDMConverterFile : " + ohdmConverter + "\n";
        output += "javaPath : " + javaJdkPath + "\n";
        output += "jdbcPath : " + jdbcDriverPath + "\n\n";
        output += "-----------------------------------------------------------------------------------------------\n";
        output += "current log : \n" + LOG;
        // end indiv write

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(individualLogFile))));
        bw.write(output);
        bw.flush();
        bw.close();
    }

    @Override
    public String toString() {
        return mapName + " " + status.toString() + " " + StaticVariables.formatDateTimeDif(startTime, LocalDateTime.now());
    }

    public String stringWithPoly() {
        return toString() + " \n POLY : " + getPrintableCoordsString();
    }
    public String json() {
        return new requestToGson(mapName, date, status.toString(), coordinates).getGson();
    }
    private static class requestToGson implements Serializable {
        private String mapName;
        private String date;
        private String status;
        private String[][] polygon;

        public requestToGson(String mapName, String date, String status, List<Coords> polygon) {
            this.mapName = mapName;
            this.date = date;
            this.status = status;
            setPolygon(polygon);
        }

        private void setPolygon(List<Coords> polygon) {
            this.polygon = new String[polygon.size()][2];
            for (int i = 0; i < polygon.size(); i++) {
                this.polygon[i][0] = polygon.get(i).x.toString();
                this.polygon[i][1] = polygon.get(i).y.toString();
            }
        }

        public String getGson() {
            return new Gson().toJson(this);
        }
    }

    public String stopThread() {
        stopThread = true;
        this.interrupt();
        return null;
    }


}
