# Main-Class : SpringClass

The Spring Class is used as two Things:

1. Main Class - initial execution Class
2. [Spring Boot Class](https://spring.io/projects/spring-boot) (for "Request Handling" and "Status Distribution")

This Class is the Main Part of the whole System. 
Every new Module needs to be implemented and started here! (for more information, click [here]()) 

_Authors Note : The Decision of "pressing" the Main Class and the SpringBoot Class together, was in a very early state of the Project and at that Point, the size of this part wasn't clear yet. For later Versions, this needs to be fixed, because this is the only part of the Project that isn't Module based._

## The "set-up" Method

The "set-up" Method includes all initial calls from reading the init-File to setting up and launching all Modules. If there will ever be a new Module addition to the System, it needs to be initialized and started here

```java
private static void set_up() throws IOException {
    // just standard inits for the Variables used in this Project
    // most of them are read from the init.txt
    // but more of that in the OHDM wiki

    new StaticVariables(initFile);
    StaticVariables.init();
    StaticVariables.createStdFilesAndDirs();

    // Logger is a singleton Class due to the availability need
    // the Logger is a System in itself
    new Logger(StaticVariables.logDefaultDir, StaticVariables.maxLogFileSize, StaticVariables.logTerminalOutput);
    Logger.instance.start();

    /*------------------ new Services and Systems need to be established after the Logger!!!!! -------------------*/

    // set up for ID System
    // this isn't really a System of some sort, it just saves, reads and creates IDs
    // everything in it is static and it's no Thread/Runnable
    IDSystem.setIDSaveFile(new File(StaticVariables.idSavePath + "idSave.txt"));

    // the service instance, the Controller for all the Requests, is in charge of activating and managing requests
    // you could actually call it an "Controller" of some sorts... but I didn't bother changing the name
    // TODO: maybe later
    serviceInstance = new RequestManager();
    serviceThread = serviceInstance;
    serviceThread.start();

    // the ftp service, which allows the Android App to download the .map files
    // !! deprecated !!
    /*ftpInstance = new FTPService(StaticVariables.ftpPort, StaticVariables.ftpServiceUserPropertiesFile,  StaticVariables.standardUserName, StaticVariables.standardUserPassword, StaticVariables.ftpServiceMapDir, StaticVariables.ftpDefaultDir);
    ftpThread = new Thread(ftpInstance);
    ftpThread.start();*/

    // the sftp service, which allows the Android App to download the .map files
    sftpInstance = new SftpService(StaticVariables.sftpPort, StaticVariables.standardUserName, StaticVariables.standardUserPassword, StaticVariables.sftpDefaultKeyFile, StaticVariables.sftpServiceMapDir, StaticVariables.msgPath);
    sftpThread = sftpInstance;
    sftpThread.start();

    // filling up the Request Manager with file-saved Requests from the req-File-Directory
    fillRequestManager();
}
```

---

## The "main" Method

The initial execution Method, also known as "main" Method, does not contain much except the call of the __set up__ method, start of the __Spring Application__ and the start of the __Controller Endpoint Module__.

```java
public static void main(String[] args) throws IOException {
    set_up();

    // and here starts the Spring Application with the server port set to the
    // before given Port in Server.StaticVariables
    System.getProperties().put("server.port", StaticVariables.webPort);
    cntxt = SpringApplication.run(SpringClass.class, args);

    ControllerEndpoint ce = new ControllerEndpoint(StaticVariables.msgPath);
    Thread ce_thread = new Thread(ce);
    ce_thread.start();
}
```

_Authors Note : The Controller is the only exception to the "every Module is called in the set up Method" Rule, because the Controller isn't allowed to start before the Server itself. If it were, the Controller could try to access a specific part of the String Boot Application before the Class even started. This is also a part of the reason, why the **Spring Boot Application Class** needs to be separated in further Versions of this Project._

---

## SpringBoot Methods

These Methods are the Spring Boot Request Methods, which will be called with their specific parameters over HTTP.

| Name         | Request Mapping | Parameter List            | Default Value                   | Response Entity                | Description                                                  | Authors Note                                                 |
| ------------ | --------------- | ------------------------- | ------------------------------- | ------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| request      | "/request"      | mapName (value = "name")  | "testRequest"                   | String (the id of the request) | This is the proper way to request a map to be made.          |                                                              |
|              |                 | coords (value = "coords") | "13,52_14,52_14,53_13,53_13,52" |                                | x1,y1_x2,y2_x3,y3_x4,y4_..._x                                |                                                              |
|              |                 | date (value = "date")     | "2016-01-31"                    |                                |                                                              |                                                              |
|              |                 | id (value = "id")         | ""                              |                                |                                                              |                                                              |
| statusByID   | "/statusByID"   | id (value = "id")         | ""                              | String (request status)        | Used for either the App if given a specific id, or the Website, for giving an overview of all the current existing Requests | Note that the output for the website is at it's core, completely  different from the output for the App, so can't be swapped for now. |
| id           | "/id"           | empty                     | empty                           | String                         | creates a new id, saves it and returns it in form of a String | Used for the app, when requesting a new ID.                  |
| mapsDownload | "/maps"         | name (value = "name")     | ""                              | Object                         | download link for a maps osm Data                            | This is only used for the website! The app uses sftp to download a map |

---

## Controller Methods



```java
public static String reloadRequestService() throws IOException {
    // Method to read, initialize and define all ll 
    StaticVariables.init();
    StaticVariables.createStdFilesAndDirs();
    ArrayList<QueryRequest> bufferSave = serviceInstance.getBUFFER_LIST();
    serviceInstance.stopThread();
    try {
        serviceThread.join();
    } catch (InterruptedException e) {
        return "request service restart interrupted, please try again";
    }
    serviceThread = null;
    serviceInstance = null;

    serviceInstance = new RequestManager(bufferSave, 5);
    serviceThread = serviceInstance;
    serviceThread.start();
    return "webservice reloaded | saved pending Requests in Buffer = " + bufferSave.size();
}

public static String reloadLog() throws IOException {
    StaticVariables.init();
    StaticVariables.createStdFilesAndDirs();
    Logger.instance.stopThread();
    try {
        Logger.instance.join();
    } catch (InterruptedException e) {
        return "logger restart interrupted, please try again";
    }
    List<LogEntry> bufferSave = Logger.instance.getBUFFER_LIST();
    Logger.instance = null;
    new Logger(StaticVariables.logDefaultDir, StaticVariables.maxLogFileSize, StaticVariables.logTerminalOutput);
    Logger.instance.setBUFFER_LIST(bufferSave);
    Logger.instance.start();

    return "Logger reloaded | saved pending LogEntries in Buffer = " + bufferSave.size();
}

public static String restart() throws IOException {
    String _return = "";
    _return += reloadLog() + "\n";
    Logger.instance.addLogEntry(DEBUG, TAG, "restarted Logger");
    _return += reloadID() + "\n";
    Logger.instance.addLogEntry(DEBUG, TAG, "restarted ID System");
    _return += reloadRequestService() + "\n";
    Logger.instance.addLogEntry(DEBUG, TAG, "restarted webService");
    SpringApplication.exit(cntxt, () -> 0);

    System.getProperties().put("server.port", StaticVariables.webPort);
    cntxt = SpringApplication.run(SpringClass.class);
    return _return + "\nrestarted service -  look into daemon-log to see init";
}

public static String reloadID() throws IOException {
    StaticVariables.init();
    StaticVariables.createStdFilesAndDirs();
    IDSystem.setIDSaveFile(new File(StaticVariables.idSavePath + "idSave.txt"));
    return "loaded new ID File";
}

public static String clearBuffer() {
    return serviceInstance.resetBuffer();
}

public static String cleanDone() {
    return serviceInstance.resetDone();
}

public static String cleanError() {
    return serviceInstance.resetError();
}
```