package Server.RequestManager;

import Server.CustomObjects.QueryRequest;

public interface srvInterface extends Runnable{

    /**
     * adds new Request
     * ( will run in thread )
     */
    void add(QueryRequest request);

    /**
     * will stop the currently running Thread
     * ( will stop the thread an all Requests )
     */
    void stopThread();

    /**
     * empties the BUFFER_LIST
     * @return Debug Message, used for Logger and Controller
     */
    String resetBuffer();
    /**
     * empties the DONE_LIST
     * @return Debug Message, used for Logger and Controller
     */
    String resetDone();
    /**
     * empties the ERROR_LIST
     * @return Debug Message, used for Logger and Controller
     */
    String resetError();

    /**
     * stops a specific Request in the WORKER_LIST
     * @param i index of specific Request
     * @return Debug Message for Logger and Controller
     */
    String stopRunningThread(int i);
}
