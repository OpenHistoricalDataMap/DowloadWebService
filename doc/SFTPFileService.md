# SFTP File Service

The SFTP File server is used to provide the Android App a Way to download map files and gives the Controller access to the Service.
The Service is not used for the Download over the official OHDM-Website. 

Used Dependencies:

+ [Apache MINA](https://mina.apache.org/mina-project/) (SFTP for the Distribution of said map-files)

## Variables

| Name               | Type                         | Standard Value                | Description                                                  | Authors Note |
| ------------------ | ---------------------------- | ----------------------------- | ------------------------------------------------------------ | ------------ |
| sshServer          | SshServer                    | null                          | since sftp is based on ssh , we're using an base ssh server  |              |
| port               | int                          | null (defined in Constructor) | the port, that will be used to access                        |              |
| standardUser       | [SftpUser]()                 | null (defined in Constructor) | standard user for the access from the app (name and password defined in the init file) |              |
| controller         | [SftpUser]()                 | null (defined in Constructor) | controller access for the controller (name and password constant) |              |
| sharePath          | String                       | null (defined in Constructor) | path for the app access                                      |              |
| msgPath            | String                       | null (defined in Constructor) | path for the controller msg                                  |              |
| isRunning          | boolean                      | false                         | shows, if the SFTP Server Thread is running                  |              |
| keySaveFilePath    | String (static)              | null (defined in Constructor) | the path for the saveFile for the keyFile ( for more information, look up on how sftp works) |              |
| sftpCommandFactory | List<NamedFactory<Command>>  | null (defined in set-up)      | command factory for the sftp server                          |              |
| userAuthFactories  | List<NamedFactory<UserAuth>> | null (defined in set-up)      | User Authentication Factory for the ssh server               |              |
| vfsf               | VirtualFileSystemFactory     | null (defined in set-up)      | VirtualFileSystemFactory for the current access              |              |
| namedFactoryList   | List<NamedFactory<Command>>  | null (defined in set-up)      | namedFactoryList for the sshServer                           |              |
| TAG                | String (static, final)       | "SFTP-Service"                | Tag for the Logger                                           |              |

---

## Constructor

| Parameter List  | Type   | used for...     | Authors Note |
| --------------- | ------ | --------------- | ------------ |
| port            | int    | port            |              |
| strUsername     | String | standardUser    |              |
| stdPasswd       | String | standardUser    |              |
| keySaveFilePath | String | keySaveFilePath |              |
| sharePath       | String | sharePath       |              |
| msgPath         | String | msgPath         |              |

```java
public SftpService(int port, String stdUsername, String stdPasswd, String keySaveFilePath, String sharePath, String msgPath) {
    SftpService.keySaveFilePath = keySaveFilePath;
    this.port = port;
    this.sharePath = sharePath;
    this.msgPath = msgPath;

    // setting up the standard user, these are the credentials used by the Android app
    standardUser = new SftpUser(stdUsername, stdPasswd);
    controller = new SftpUser("controller", "eqpS23PTagZgHmJcFQMBLgJv");

}
```

---

## The "run" Method

```java
@Override
    public synchronized void run() {
        try {
            // setup for the sftp Server
            setup();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // setting the status to "running" 
        isRunning = true;
        try {
            wait();
        } catch (InterruptedException e) {
            Logger.instance.addLogEntry(LogType.INFO, TAG, "SftpService was interrupted adn needs to be restarted");
        }
    }
```

---

## Public Methods

### Interface Methods :

| Name       | Parameter list | return Value | Description                | Authors Note |
| ---------- | -------------- | ------------ | -------------------------- | ------------ |
| stopThread | empty          | void         | method to stop the Service |              |

### Getter and Setter

no getter/setter 

---

## Private Methods

| Name  | Parameter List | return Value | Description                             | Authors Note |
| ----- | -------------- | ------------ | --------------------------------------- | ------------ |
| setup | empty          | void         | Setup method for the ssh -> sftp server |              |

