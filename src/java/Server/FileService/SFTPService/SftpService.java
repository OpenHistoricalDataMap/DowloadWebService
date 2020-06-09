package Server.FileService.SFTPService;

import Server.CustomObjects.LogType;
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
public class SftpService extends Thread {
    // since sftp is based on ssh , we're using an base ssh server
    private SshServer sshServer;

    private int port;
    private SftpUser standardUser;

    private String sharePath;
    public boolean isRunning = false;
    // this is the hostKey Save File
    // if you don't know what it does, you should go on this wiki and read all about sftp
    private static String keySaveFilePath;

    private List<NamedFactory<Command>> sftpCommandFactory;
    private List<NamedFactory<UserAuth>> userAuthFactories;
    private VirtualFileSystemFactory vfsf;
    private List<NamedFactory<Command>> namedFactoryList;

    private static final String LOG_TAG = "SFTP-Service";

    public SftpService(int port, String stdUsername, String stdPassw, String keySaveFilePath, String sharePath) {
        this.keySaveFilePath = keySaveFilePath;
        this.port = port;
        this.sharePath = sharePath;

        // setting up the standard user, these are the credentials used by the Android app
        standardUser = new SftpUser(stdUsername, stdPassw);

    }

    private void setup() throws IOException {
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(port);

        // setting up the path to the key saver file -
        // THIS NEED'S TO BE CONSTANT AND FINAL, PLEASE DO NOT CHANGE THE FILE AFTER DEPLOYMENT !!!
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
                    Logger.instance.addLogEntry(LogType.INFO, LOG_TAG, "new log-in  | public key \n" + session.getHostKey().getPublic()
                            + "\n used credentials : username=" + standardUser.getUsername() + " password=" + standardUser.getEncryptedPass() + " (encrypted)");
                    return true;
                } else {
                    Logger.instance.addLogEntry(LogType.INFO, LOG_TAG, "failed login " + session.getClientAddress());
                    return false;
                }
            }
        });


        // setting up the working directory, so that the user can just access the files we want
        // if no FileSystemFactory is set, everything is see-able, edit-able and delete-able
        vfsf = new VirtualFileSystemFactory();
        // WARNING ! the VFSF always wants to have an absolute path ( means a path from root ), don't forget that
        vfsf.setUserHomeDir(standardUser.getUsername(), Paths.get(new File(sharePath).getAbsolutePath()));
        sshServer.setFileSystemFactory(vfsf);

        // sftpSub System set
        namedFactoryList = new ArrayList<>();
        namedFactoryList.add(new SftpSubsystemFactory());
        sshServer.setSubsystemFactories(namedFactoryList);

        // starting the ssh server ( out sftp server )
        sshServer.start();
        Logger.instance.addLogEntry(LogType.INFO, LOG_TAG, "started sftp Session \nPort: " + port + " \nsharedPath: " + sharePath + "\n success: " + sshServer.isOpen());
    }

    public void exitService() {
        if (isRunning)
            this.interrupt();
        else
            throw new NullPointerException("service isn't running yet");
        return;
    }

    @Override
    public synchronized void run() {
        try {
            setup();
        } catch (IOException e) {
            e.printStackTrace();
        }
        isRunning = true;
        try {
            wait();
        } catch (InterruptedException e) {
            // exiting
        }
    }
}
