package Server.ControllerEndpoint;

import Server.CustomObjects.QueryRequest;
import Server.LogService.Logger;
import Server.SpringClass;

import java.io.*;
import java.text.SimpleDateFormat;

public class ControllerEndpoint extends Thread {

    private String msgPath;

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
        return new ControllerRequest(id, value);
    }

    private BufferedWriter writeResponse(File file, ControllerRequest r) throws IOException {
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
        bw.write(r.returnValue);
        bw.flush();
        return bw;
    }

    private String processRequest(String request) throws IOException {
        String answer = "";
        BufferedReader br;
        switch (request) {
            case "help" :
                answer += "currently implemented : " + "\n";
                answer += " status - giving back the status the service is currently in \n";
                answer += " log - gives back the current log entries\n";
                answer += " daemonLog - gives back the daemon log entries\n";
                answer += " reload webservice - restarts the service with new read init values (all buffered requests will be saved)\n";
                answer += " reload log - restarts log with new read init values\n";
                answer += " restart -  restarts the whole Service with new read init values\n";
                return answer;


            case "status" :
                answer += "Service running : " + SpringClass.serviceInstance.isRunning() + "\n";
                answer += "Service currently working : " + SpringClass.serviceInstance.isActive() + "\n";
                answer += "Requests currently waiting : " + SpringClass.serviceInstance.getBUFFER_LIST().size() + "\n";
                answer += "Logger active : " + Logger.instance.isRunning + "\n";
                answer += "-----------------------------------------------------------------------------\n";
                answer += "current working Requests : \n";
                for (QueryRequest r:
                     SpringClass.serviceInstance.getWORKER_LIST()) {
                    answer += "ID: " + r.getRequestedByID() + "\n";
                    answer += "requestName: " + r.getMapName() + "\n";
                    answer += "status: " + r.getStatus() + "\n\n";
                    answer += "time passed since start" + new SimpleDateFormat("yyyy-MM-dd at HH-mm-ss").format(System.currentTimeMillis() - r.getStartTime());
                }
                return answer;

            case "current log" :
                File loggerFile = Logger.instance.currentWritingFile;
                br = new BufferedReader(new InputStreamReader(new FileInputStream(loggerFile)));
                while (br.ready())
                    answer += br.readLine() + "\n";

                return answer;

            case "daemon log" :
                File daemonLogFile = new File("daemonLog.log");
                br = new BufferedReader(new InputStreamReader(new FileInputStream(daemonLogFile)));
                while (br.ready())
                    answer += br.readLine() + "\n";

                return answer;

            case "reload webservice":
                return SpringClass.reloadWebService();

            case "reload log":
                return SpringClass.reloadLog();

            case "restart":
                return SpringClass.restart();

            default:
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
                System.out.println("new File found");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                ControllerRequest request = readContent(f);

                assert request != null;
                    request.setReturnValue(processRequest(request.value));

                f.delete();

                File returnFile = new File(msgPath + request.id + ".ans");
                writeResponse(returnFile, request).close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
