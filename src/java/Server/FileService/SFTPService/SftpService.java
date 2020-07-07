package Server.FileService.SFTPService;

import Server.CustomObjects.LogType;
import Server.FileService.FileService;
import Server.LogService.Logger;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuth;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

// based on ssh protocol, library used : apache mina sshd
public class SftpService extends Thread implements FileService {
    // since sftp is based on ssh , we're using an base ssh server
    private SshServer sshServer;

    // the port, that will be used to access
    private int port;
    // standard user for the access from the app
    private SftpUser standardUser;
    // controller access for the controller
    private SftpUser controller;

    // path for the app access
    private String sharePath;
    // path for the controller msg
    private String msgPath;
    // shows, if the SFTP Server Thread is running
    public boolean isRunning = false;

    // this is the hostKey Save File
    // if you don't know what it does, you should go on this wiki and read all about sftp
    private static String keySaveFilePath;

    // command factory for the sftp server
    private List<NamedFactory<Command>> sftpCommandFactory;
    // User Authentication Factory for the ssh server
    private List<NamedFactory<UserAuth>> userAuthFactories;
    // VirtualFileSystemFactory for the current access
    private VirtualFileSystemFactory vfsf;
    // namedFactoryList for the sshServer
    private List<NamedFactory<Command>> namedFactoryList;

    // Tag for the Logger
    private static final String TAG = "SFTP-Service";

    /**
     * Constructor
     * @param port the port, that will be used to access
     * @param stdUsername standard user-name for the access from the app
     * @param stdPasswd standard user-password for the access from the app
     * @param keySaveFilePath hostKey Save File
     * @param sharePath path for the app access
     * @param msgPath path for the controller msg
     */
    public SftpService(int port, String stdUsername, String stdPasswd, String keySaveFilePath, String sharePath, String msgPath) {
        SftpService.keySaveFilePath = keySaveFilePath;
        this.port = port;
        this.sharePath = sharePath;
        this.msgPath = msgPath;

        // setting up the standard user, these are the credentials used by the Android app
        standardUser = new SftpUser(stdUsername, stdPasswd);
        controller = new SftpUser("controller", "eqpS23PTagZgHmJcFQMBLgJv");

    }

    /**
     * Setup method for the ssh -> sftp server
     * @throws IOException exception can be thrown when accessing the keySaveFile
     */
    private void setup() throws IOException {
        // setting up the ssh Server
        sshServer = SshServer.setUpDefaultServer();
        // setting up the ssh connection port
        sshServer.setPort(port);

        // setting up the path to the key saver file -
        // THIS NEEDS TO BE CONSTANT AND FINAL, PLEASE DO NOT CHANGE THE FILE AFTER DEPLOYMENT !!!
        if (!new File(keySaveFilePath).exists()) {
            new File(keySaveFilePath).createNewFile();
        }
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File(keySaveFilePath)));

        // command factory set
        sftpCommandFactory = new ArrayList<>();
        sftpCommandFactory.add(new SftpSubsystemFactory());
        sshServer.setCommandFactory(new ScpCommandFactory());

        // User Auth set
        userAuthFactories = new ArrayList<>();
        userAuthFactories.add(new UserAuthPasswordFactory());
        sshServer.setUserAuthFactories(userAuthFactories);

        // Pass Auth set
        sshServer.setPasswordAuthenticator(new PasswordAuthenticator() {
            public boolean authenticate(String username, String password, ServerSession session) {
                if (username.equals(standardUser.getUsername()) && password.equals(standardUser.getPassword())) {
                    Logger.instance.addLogEntry(LogType.INFO, TAG, "new log-in " + session.getClientAddress() + " | public key \n" + session.getHostKey().getPublic()
                            + "\n used credentials : username=" + standardUser.getUsername() + " password=" + standardUser.getEncryptedPass() + " (encrypted)");
                    return true;
                } else if (username.equals(controller.getUsername()) && password.equals(controller.getPassword())) {
                    Logger.instance.addLogEntry(LogType.INFO, TAG, "new controller login " + session.getClientAddress());
                    return true;
                } else {
                    Logger.instance.addLogEntry(LogType.INFO, TAG, "failed login " + session.getClientAddress() + " with login name : " + username + " wrong password/wrong username");
                    return false;
                }
            }
        });


        // setting up the working directory, so that the user can just access the files we want
        // if no FileSystemFactory is set, everything is see-able, edit-able and delete-able
        vfsf = new VirtualFileSystemFactory();
        // WARNING ! the VFSF always wants to have an absolute path ( means a path from root ), don't forget that
        vfsf.setUserHomeDir(standardUser.getUsername(), Paths.get(new File(sharePath).getAbsolutePath()));
        vfsf.setUserHomeDir(controller.getUsername(), Paths.get(new File(msgPath).getAbsolutePath()));
        sshServer.setFileSystemFactory(vfsf);

        // sftpSub System set
        namedFactoryList = new ArrayList<>();
        namedFactoryList.add(new SftpSubsystemFactory());
        sshServer.setSubsystemFactories(namedFactoryList);

        // starting the ssh server ( -> sftp server )
        sshServer.start();
        Logger.instance.addLogEntry(LogType.INFO, TAG, "started sftp Session \nPort: " + port + " \nsharedPath: " + sharePath + "\n success: " + sshServer.isOpen());
    }

    /**
     * method to stop the Service
     */
    @Override
    public void stopThread() {
        if (isRunning)
            this.interrupt();
        else
            throw new NullPointerException("service isn't running yet");
    }

    /**
     * runner
     */
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
}
