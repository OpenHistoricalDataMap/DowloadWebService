package Server.RequestService;

import Server.CustomObjects.QueryRequest;

public interface srvInterface extends Runnable{

    /**
     * adding
     * ( will run in thread )
     */
    void add(QueryRequest request);
}
