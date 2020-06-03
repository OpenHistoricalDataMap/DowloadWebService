package Server.WebService;

import Server.CustomObjects.LogType;
import Server.CustomObjects.QueryRequest;
import Server.LogService.Logger;

import java.util.ArrayList;

import static Server.CustomObjects.LogType.ERROR;
import static Server.CustomObjects.LogType.INFO;

public class ServiceNew extends Thread implements srvInterface {

    private ArrayList<QueryRequest> BUFFER_LIST = new ArrayList<>();
    private ArrayList<QueryRequest> WORKER_LIST = new ArrayList<>();
    private ArrayList<QueryRequest> DONE_LIST = new ArrayList<>();
    private ArrayList<QueryRequest> ERROR_LIST = new ArrayList<>();

    private int maxInWorkerList = 5;

    private boolean running = false;
    private boolean active = false;
    private boolean full = false;

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

    /**
     * Input Method for the Service
     * @param request
     */
    public void add(QueryRequest request) {
        // if there is space in the WORKER_LIST, it actually will directly put it in the WORKER_LIST and activate it
        if (WORKER_LIST.size() < maxInWorkerList) {
            WORKER_LIST.add(request);
            request.start();
            Logger.instance.addLogEntry(INFO, TAG, "added request : " + request.getMapName() + " and started it");
        } else {
            // if there is no space in the WORKER_LIST, it just adds it to the BUFFER_LIST
            BUFFER_LIST.add(request);
            Logger.instance.addLogEntry(INFO, TAG,"added request : " + request.getMapName());

            if (!active)
                interrupt();
        }
    }

    /**
     * runs through the WORKER_LIST and takes all "non-active" Status
     * (so that means either "Error" or "Done"),
     * and puts them into their specified Lists
     * (so either ERROR_LIST or DONE_LIST)
     */
    void cleanWorkerList() {
        //
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

    /**
     * method to fill WORKER_LIST if there is space for it
      */
    void setObjectsToActive() {
        // first it figures out how much space is actually on the WORKER_LIST
        int availableItems =  maxInWorkerList - WORKER_LIST.size();

        // this makes sure, that if there are less items waiting in the Buffer_LIST, than are available
        // that we go down to this number, so that we don't "overshoot" the BUFFER_LIST
        if (availableItems > BUFFER_LIST.size())
            availableItems = BUFFER_LIST.size();

        // if there are no items to start or if the WORKER_LIST is full
        // than jump out here
        if (availableItems <= 0)
            return;

        // here, the service just goes through all the QueryRequests and sets as many as it can active
        for (int i = 0; i < availableItems; i++) {
            QueryRequest object = BUFFER_LIST.get(i);
            object.start();
            WORKER_LIST.add(object);
            BUFFER_LIST.remove(i);
            Logger.instance.addLogEntry(INFO, TAG, "started " + WORKER_LIST.get(WORKER_LIST.size()-1).getMapName());
        }
    }

    // This Thread doesn't really do much, except going through all the Lists and Update their Positions in the Lists
    @Override
    public synchronized void run() {
        // sets Token "running" to true for debug purposes
        // if it is ever "false", this means, that the Service has to be restarted
        // for now, that means that the whole System needs to be restarted, but we'll add an "while running reload"
        // Function later
        running = true;

        try {
            while (true) {
                active = true;
                full = false;
                // runs through the WORKER_LIST and takes all "non-active" Status
                // (so that means either "Error" or "Done"),
                // and puts them into their specified Lists
                // (so either ERROR_LIST or DONE_LIST)
                cleanWorkerList();

                // fills empty slots in the WORKER_LIST by adding new Requests from the BUFFER_LIST
                setObjectsToActive();

                // if the BUFFER_LIST is empty, then the service will turn inactive and wait for a "wake-up"
                // in our case here, it's an interrupt, which will be given in the "add" Method if the Service is inactive.
                if (BUFFER_LIST.isEmpty()) {
                    try {
                        active = false;
                        wait();
                    } catch (InterruptedException e) {
                        active = true;
                    }
                }

                if (WORKER_LIST.size() == maxInWorkerList)
                {
                    try {
                        full = true;
                        wait();
                    } catch (InterruptedException e) {
                        full = false;
                    }
                }
            }
        }catch (Exception e) {
            Logger.instance.addLogEntry(LogType.ERROR, TAG,"Service Failed due to Exception thrown " + e.getMessage());
            running = false;
        }
    }
}
