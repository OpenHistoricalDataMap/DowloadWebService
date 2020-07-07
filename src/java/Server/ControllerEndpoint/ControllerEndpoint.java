package Server.ControllerEndpoint;

import Server.CustomObjects.QueryRequest;
import Server.IDService.IDSystem;
import Server.LogService.Logger;
import Server.SpringClass;
import Server.StaticVariables;

import javax.swing.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;

import static Server.CustomObjects.LogType.INFO;

public class ControllerEndpoint extends Thread {

    private String msgPath;
    private String LOG_TAG = "ControllerAccessEndpoint";

    private static final String STATUS_KEY = "status";
    private static final String STATUS_DONE_KEY  = "done";
    private static final String STATUS_ERROR_KEY = "error";
    private static final String STATUS_BUFFER_KEY = "buffer";
    private static final String ID_KEY = "id";
    private static final String LOG_KEY = "current log";
    private static final String DAEMON_KEY = "daemon log";
    private static final String RELOAD_S_KEY = "reload requestService";
    private static final String RELOAD_LOG_KEY = "reload log";
    private static final String RELOAD_ID_KEY = "reload id";
    private static final String CLEAR_BUFFER_LIST = "clear buffer";
    private static final String STOP_WORKER_LIST = "stop worker";
    private static final String CLEAN_ERROR_LIST = "clean error";
    private static final String CLEAN_DONE_LIST = "clean done";
    private static final String RESTART_KEY = "restart";

    public ControllerEndpoint(String msgPath) {
        this.msgPath = msgPath;
    }
    private synchronized File fetchNewFile() throws InterruptedException {
        File msgDir = new File(msgPath);
        if (!msgDir.exists()) {
            msgDir.mkdir();
        }

        File[] fileList;

        while (true) {
            fileList = msgDir.listFiles();
            if (fileList.length == 0)
                wait(1000);
            else
                for (File f : fileList) {
                    if (!f.getName().endsWith(".req") && !f.getName().endsWith(".ans"))
                        f.delete();
                    else {
                        if (f.getName().endsWith(".ans")) {
                            // kindly ignore
                        } else {
                            return f;
                        }
                    }

                    wait(1000);
                }
        }
    }
    private ControllerRequest readContent(File file) throws IOException {
        String id = file.getName().replace(".req", "");
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

        if (!br.ready())
            return null;

        String value = br.readLine();
        br.close();
        Logger.instance.addLogEntry(INFO, LOG_TAG, "read new request | ID : " + id + " | Request : " + value);
        return new ControllerRequest(id, value);
    }
    private BufferedWriter writeResponse(File file, ControllerRequest r) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
        bw.write(r.returnValue);
        bw.flush();
        Logger.instance.addLogEntry(INFO, LOG_TAG, "response send : " + r.returnValue + " | \nID: " + r.id);
        return bw;
    }
    private String processRequest(String request) throws IOException {
        String answer = "";
        BufferedReader br;
        switch (request) {
            case "help" :
                answer += "currently implemented : " + "\n";
                answer += STATUS_KEY + "\t - giving back the status the service is currently in\n";
                answer += STATUS_DONE_KEY + "\t - showing all the finished requests\n";
                answer += STATUS_ERROR_KEY + "\t - shows all the Requests with an error\n";
                answer += STATUS_BUFFER_KEY + "\t - shows all the Requests in the Buffer\n";
                answer += ID_KEY + "\t - gives all currently saved ids\n";
                answer += LOG_TAG + "\t - gives back the current log entries\n";
                answer += DAEMON_KEY + "\t - gives back the daemon log entries\n";
                answer += RELOAD_S_KEY + "\t - restarts the service with new read init values (all buffered requests will be saved)\n";
                answer += RELOAD_LOG_KEY + "\t - restarts log with new read init values\n";
                answer += RELOAD_ID_KEY + "\t - restarts id with new read init values\n";
                answer += CLEAR_BUFFER_LIST + "\t - clears all requests in the buffer\n";
                answer += CLEAN_DONE_LIST + "\t - cleans all requests in the \"Done List\"\n";
                answer += CLEAN_ERROR_LIST + "\t - cleans all requests in the \"Error list\"\n";
                answer += STOP_WORKER_LIST + "\t - stops a specific or all currently running Requests\n\t nr <1-5>\n";
                answer += RESTART_KEY + "\t - restarts the whole Service with new read init values\n";
                answer += "testRequest \t- makes a testRequest of default values\n";
                return answer;


            case STATUS_KEY :
                answer += "Service running : " + SpringClass.serviceInstance.isRunning() + "\n";
                answer += "Service currently working : " + SpringClass.serviceInstance.isActive() + "\n";
                answer += "Requests currently waiting : " + SpringClass.serviceInstance.getBUFFER_LIST().size() + "\n";
                answer += "Logger active : " + Logger.instance.isRunning() + "\n";
                answer += "-----------------------------------------------------------------------------\n";
                answer += "current Requests in Error : " + SpringClass.serviceInstance.getERROR_LIST().size() + "\n";
                answer += "current Requests in Done : " + SpringClass.serviceInstance.getDONE_LIST().size() + "\n";
                answer += "current working Requests : \n";
                for (QueryRequest r: SpringClass.serviceInstance.getWORKER_LIST()) {
                    answer += " - ID: " + r.getRequestedByID() + "\n";
                    answer += " - requestName: " + r.getMapName() + "\n";
                    answer += " - status: " + r.getStatus() + "\n";
                    answer += " - time passed since start " + StaticVariables.formatDateTimeDif(r.getRequestTime(), LocalDateTime.now()) + "\n\n";
                }
                return answer;

            case STATUS_DONE_KEY:
                answer += "current done requests : \n";
                for (QueryRequest r: SpringClass.serviceInstance.getDONE_LIST()) {
                    answer += " - ID: " + r.getRequestedByID() + "\n";
                    answer += " - requestName: " + r.getMapName() + "\n";
                    answer += " - request Time" + r.getRequestTime() + "\n";
                    answer += " - start Time: " + r.getRuntimeStart() + "\n";
                    answer += " - end Time: " + r.getEndTime() + "\n\n";
                }
                return answer;

            case STATUS_ERROR_KEY:
                answer += "current error requests : \n";
                for (QueryRequest r: SpringClass.serviceInstance.getERROR_LIST()) {
                    answer += " - ID: " + r.getRequestedByID() + "\n";
                    answer += " - requestName: " + r.getMapName() + "\n";
                    answer += " - start Time: " + r.getRuntimeStart() + "\n";
                    answer += " - end Time: " + r.getEndTime() + "\n";
                    answer += " - error message: " + r.getErrorMessage() + "\n\n";
                }
                return answer;

            case STATUS_BUFFER_KEY:
                answer += "current buffer requests : \n";
                int i = 0;
                for (QueryRequest r: SpringClass.serviceInstance.getBUFFER_LIST()) {
                    i++;
                    answer += " - ID: " + r.getRequestedByID() + "\n";
                    answer += " - requestName: " + r.getMapName() + "\n";
                    answer += " - request Time: " + r.getRequestTime() + "\n";
                    answer += " - Query number: " + i + "\n\n";
                }
                return answer;

            case ID_KEY :
                answer += "All IDs\n";
                answer += "-------------------------------\n";
                answer += IDSystem.getAllIDs();
                return answer;

            case LOG_KEY :
                File loggerFile = Logger.instance.getCurrentWritingFile();
                br = new BufferedReader(new InputStreamReader(new FileInputStream(loggerFile)));
                while (br.ready())
                    answer += br.readLine() + "\n";

                return answer;

            case DAEMON_KEY :
                File daemonLogFile = new File("daemonLog.txt");
                br = new BufferedReader(new InputStreamReader(new FileInputStream(daemonLogFile)));
                while (br.ready())
                    answer += br.readLine() + "\n";

                return answer;

            case RELOAD_S_KEY:
                return SpringClass.reloadRequestService();

            case RELOAD_LOG_KEY:
                return SpringClass.reloadLog();

            case RELOAD_ID_KEY:
                return SpringClass.reloadID();

            case CLEAR_BUFFER_LIST:
                return SpringClass.clearBuffer();

            case CLEAN_ERROR_LIST:
                return SpringClass.cleanError();

            case CLEAN_DONE_LIST:
                return SpringClass.cleanDone();

            case RESTART_KEY:
                return SpringClass.restart();

            case STOP_WORKER_LIST:
                return SpringClass.serviceInstance.stopRunningThread(0);

            case "testRequest":
                return SpringClass.request("controllerRequest", "13.3772707,52.5092213_13.3772707,52.5245509_13.4065819,52.5245509_13.4065819,52.5092213_13.3772707,52.5092213", "2016-12-31", "CONTROLR");

            default:
                // special requests with values attached to them
                String[] requestSplit = request.split(" ");
                if (requestSplit[0].equals("stop") && requestSplit[1].equals("worker"))
                    if (requestSplit[2].equals("nr")) {
                        int j;
                        try { j = Integer.parseInt(requestSplit[3]); } catch (NumberFormatException e) { return requestSplit[3] + " is not a number..."; }
                        return SpringClass.serviceInstance.stopRunningThread(j);
                    }
                return "couldn't process request " + request;
        }
    }
    private class ControllerRequest {
        String id;
        String value;
        String returnValue;

        public ControllerRequest(String id, String value) {
            this.id = id;
            this.value = value;
        }

        public void setReturnValue(String returnValue) {
            this.returnValue = returnValue;
        }

        public String getReturnValue() {
            return returnValue;
        }
    }
    @Override
    public synchronized void run() {
        while (true) {
            File f = null;
            try {
                f = fetchNewFile();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ControllerRequest request = null;
            try {
                request = readContent(f);

            } catch (IOException e) {
                e.printStackTrace();
            }
            assert request != null;

            try {
                request.setReturnValue(processRequest(request.value));
            } catch (IOException e) {
                request.setReturnValue(e.getMessage());
            }

            f.delete();

            File returnFile = new File(msgPath + request.id + ".ans");
            try {
                writeResponse(returnFile, request).close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
