package Server.WebService;

import Server.CustomObjects.LogType;
import Server.CustomObjects.QueryRequest;
import Server.LogService.Logger;

import java.util.ArrayList;

import static Server.CustomObjects.LogType.ERROR;
import static Server.CustomObjects.LogType.INFO;

public class ServiceNew extends Thread {

    private ArrayList<QueryRequest> BUFFER_LIST = new ArrayList<>();
    private ArrayList<QueryRequest> WORKER_LIST = new ArrayList<>();
    private ArrayList<QueryRequest> DONE_LIST = new ArrayList<>();
    private ArrayList<QueryRequest> ERROR_LIST = new ArrayList<>();

    private int maxInWorkerList = 5;

    private boolean running = false;
    private boolean active = false;
    private String TAG = "WebService-Thread";

    public boolean isActive() {
        return active;
    }
    public boolean isRunning() { return running; }

    public ArrayList<QueryRequest> getBUFFER_LIST() {
        return BUFFER_LIST;
    }

    public ArrayList<QueryRequest> getWORKER_LIST() {
        cleanWorkerList();
        return WORKER_LIST;
    }

    public ArrayList<QueryRequest> getDONE_LIST() {
        cleanWorkerList();
        return DONE_LIST;
    }

    public ArrayList<QueryRequest> getERROR_LIST() {
        cleanWorkerList();
        return ERROR_LIST;
    }

    public void add(QueryRequest request) {
        if (WORKER_LIST.size() < maxInWorkerList) {
            WORKER_LIST.add(request);
            request.start();
            Logger.instance.addLogEntry(INFO, TAG, "added request : " + request.getMapName() + " and started it");
        } else {
            BUFFER_LIST.add(request);
            Logger.instance.addLogEntry(INFO, TAG,"added request : " + request.getMapName());
            interrupt();
        }
    }
    void cleanWorkerList() {
        for (int i = 0; i < WORKER_LIST.size(); i++) {
            switch (WORKER_LIST.get(i).getStatus()) {
                case DONE:
                    DONE_LIST.add(WORKER_LIST.get(i));
                    WORKER_LIST.remove(i);
                    Logger.instance.addLogEntry(INFO, TAG,DONE_LIST.get(DONE_LIST.size()-1).getMapName() + " is done");
                    break;

                case ERROR:
                    ERROR_LIST.add(WORKER_LIST.get(i));
                    WORKER_LIST.remove(i);
                    Logger.instance.addLogEntry(ERROR, TAG, ERROR_LIST.get(ERROR_LIST.size()-1).getMapName() + " caused an Error and stopped");
                    break;
            }
        }
    }
    void setObjectsToActive() {
        int avaliableItems =  maxInWorkerList - WORKER_LIST.size();

        if (avaliableItems > BUFFER_LIST.size())
            avaliableItems = BUFFER_LIST.size();

        if (avaliableItems <= 0)
            return;

        else {
            for (int i = 0; i < avaliableItems; i++) {
                QueryRequest object = BUFFER_LIST.get(i);
                object.start();
                WORKER_LIST.add(object);
                BUFFER_LIST.remove(i);
                Logger.instance.addLogEntry(INFO, TAG, "started " + WORKER_LIST.get(WORKER_LIST.size()-1).getMapName());
            }
        }
    }

    @Override
    public synchronized void run() {
        running = true;

        try {
            while (true) {
                active = true;
                cleanWorkerList();
                setObjectsToActive();
                if (BUFFER_LIST.isEmpty()) {
                    try {
                        active = false;
                        wait();
                    } catch (InterruptedException e) {
                        active = true;
                    }
                }
            }
        }catch (Exception e) {
            Logger.instance.addLogEntry(LogType.ERROR, TAG,"Service Failed due to Exception thrown " + e.getMessage());
        }
    }
}
