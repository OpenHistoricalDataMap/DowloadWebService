# The Request Manager <srvInterface>

The Request Manager handles the Request put into it's System. The Manager itself is a Thread.

## Variables :

| Name            | Type                    | Standard Value                | Description                                                  | Authors Note                                                 |
| --------------- | ----------------------- | ----------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| BUFFER_LIST     | ArrayList<QueryRequest> | new (existent in Constructor) | List, where an Request goes to first when it's added         |                                                              |
| WORKER_LIST     | ArrayList<QueryRequest> | new                           | List, where an Request goes to when it's processing          |                                                              |
| DONE_LIST       | ArrayList<QueryRequest> | new                           | List, where the Request goes to, when it's done              |                                                              |
| ERROR_LIST      | ArrayList<QueryRequest> | new                           | List, where the Request goes to, when an Exception was thrown during the "Downloading" or "Converting" Phase |                                                              |
| maxInWorkerList | int                     | 5 (existent in Constructor)   | the max of Request, that are allowed being active at the same time |                                                              |
| running         | boolean                 | false                         | shows, if the Request Manager Thread is running              |                                                              |
| active          | boolean                 | false                         | shows, if at least on Request is in the Worker List          |                                                              |
| full            | boolean                 | false                         | shows, if the Worker list is full at the moment (max number of Requests working reached) |                                                              |
| TAG             | String (static, final)  | "WebService-Thread"           | Tag for the Logger                                           | more on that in [Logger]()                                   |
| stop            | boolean                 | false                         | if stop == true -> the Thread is about to stop or is already stopped | If the boolean is stop but the Thread is still running longer than a couple of seconds, then one of the Requests that is in the WORKER_LIST couldn't be stopped. In that case, just restart. |

---

## Constructors

### Constructor 1 :

| Parameter List  | Type                    | used for...             | Authors Note |
| --------------- | ----------------------- | ----------------------- | ------------ |
| BUFFER_LIST     | ArrayList<QueryRequest> | defines BUFFER_LIST     |              |
| maxInWorkerList | int                     | defines maxInWorkerList |              |

### Constructor 2:

| Parameter List | Type  | used for... | Authors Note |
| -------------- | ----- | ----------- | ------------ |
| empty          | empty | empty       | empty        |

Constructor Code : 

```java
public RequestManager(ArrayList<QueryRequest> BUFFER_LIST, int maxInWorkerList) {
    this.BUFFER_LIST = BUFFER_LIST;
    this.maxInWorkerList = maxInWorkerList;
}
```

---

## The "run" Method

```java
// This Thread doesn't really do much, except going through all the Lists and Update their Positions in the Lists
@Override
public synchronized void run() {
    // sets Token "running" to true for debug purposes
    // if it is ever "false", this means, that the Service has to be restarted
    // for now, that means that the whole System needs to be restarted, but we'll add an "while running 	reload"
    // Function later
    running = true;
    active = true;
    full = false;

    try {
        while (true) {
            // runs through the WORKER_LIST and takes all "non-active" Status
            // (so that means either "Error" or "Done"),
            // and puts them into their specified Lists
            // (so either ERROR_LIST or DONE_LIST)
            cleanWorkerList();

            // fills empty slots in the WORKER_LIST by adding new Requests from the BUFFER_LIST
            setObjectsToActive();

            // if the BUFFER_LIST is empty, then the service will turn inactive and wait for a "wake-up"
            // in our case here, it's an interrupt, which will be given in the "add" Method if the 					Service is inactive or if the stopThread method was called.
            if (BUFFER_LIST.isEmpty()) {
                try {
                    active = false;
                    wait();
                } catch (InterruptedException e) {
                    if (stop) {
                        Logger.instance.addLogEntry(INFO, TAG, "Service ended");
                        return;
                    }
                    active = true;
                }
           	// if the WORKER_LISTs Size is equal to the max of Workers being allowed to be processed 				at once, then the boolean full will be set, so no new Requests can be added to the 						WORKER_LIST. After that  it will wait for an intterput to happen, which can be called on 2 				different occasions :
                
			/* Occasion 1 = An Request is done, so it calles an interrput, so the WORKER, ERROR and DONE 			 lists can be updated.*/
            /* Occasion 2 = The Request Manager is asked to shutdown, by the stopThread Method. In this 			case, the booleans will be set to default and the run() Method calls a return */
            } else if (WORKER_LIST.size() == maxInWorkerList)
            {
                try {
                    full = true;
                    wait();
                } catch (InterruptedException e) {
                    if (stop) {
                        Logger.instance.addLogEntry(INFO, TAG, "Service ended");
                        running = false;
                        active = false;
                        full = false;
                        return;
                    }
                    full = false;
                }
            }
        }
        // if an Exception was trown, which wasn't handled before, it will be cauch here
    } catch (Exception e) {
        Logger.instance.addLogEntry(LogType.ERROR, TAG,"Service Failed due to Exception thrown " + e.getMessage());
        running = false;
    }
}
```

---

## Public Methods

### Interface Methods :

| Name              | Parameter List          | return Value                                           | Description                                 | Authors Note                                                 |
| :---------------- | ----------------------- | ------------------------------------------------------ | ------------------------------------------- | ------------------------------------------------------------ |
| add               | request (Query Request) | void                                                   | adds new Request                            | Please do not add already running Requests. It can happen that it will be ran twice. Not really the Point of the System, is it? |
| stopThread        | empty                   | void                                                   | will stop the currently running Thread      | If the Method is called, but the Thread is still running longer than a couple of seconds, then one of the Requests that is in the Worker List couldn't be stopped. In that case, just restart. |
| resetBuffer       | empty                   | Debug Message, used for Logger and Controller (String) | empties the BUFFER_LIST                     |                                                              |
| resetDone         | empty                   | Debug Message, used for Logger and Controller (String) | empties the DONE_LIST                       |                                                              |
| resetError        | empty                   | Debug Message, used for Logger and Controller (String) | empties the ERROR_LIST                      |                                                              |
| stopRunningThread | i (int)                 | Debug Message for Logger and Controller (String)       | stops a specific Request in the WORKER_LIST | Parameter i : index of specific Request                      |



### Getter and Setter

| Name           | Parameter List | return Value            | Description                                     | Authors Note |
| :------------- | -------------- | ----------------------- | ----------------------------------------------- | ------------ |
| getBUFFER_LIST | empty          | ArrayList<QueryRequest> | gives back current BUFFER_LIST                  |              |
| getWORKER_LIST | empty          | ArrayList<QueryRequest> | gives back current WORKER_LIST                  |              |
| getDONE_LIST   | empty          | ArrayList<QueryRequest> | gives back current DONE_LIST                    |              |
| getERROR_LIST  | empty          | ArrayList<QueryRequest> | gives back current ERROR_LIST                   |              |
| getAllRequests | empty          | QueryRequest[]          | gives back all Requests currently in the System |              |
| isActive       | empty          | boolean                 | is Active?                                      |              |
| isRunning      | empty          | boolean                 | is Running?                                     |              |

---

## Private Methods

| Name               | Parameter List | return Value | Description                                                  | Authors Note |
| ------------------ | -------------- | ------------ | ------------------------------------------------------------ | ------------ |
| cleanWorkerList    | empty          | void         | runs through the WORKER_LIST and takes all "non-active" Status (so that means either "Error" or "Done"), and puts them into their specified Lists (so either ERROR_LIST or DONE_LIST) |              |
| setObjectsToActive | empty          | void         | fills empty slots in the WORKER_LIST by adding new Requests from the BUFFER_LIST, and starts them |              |

