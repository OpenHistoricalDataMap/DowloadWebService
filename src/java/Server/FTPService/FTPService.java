package Server.FTPService;

import Server.CustomObjects.LogType;
import Server.LogService.Logger;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerConfigurationException;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static Server.StaticVariables.*;

public class FTPService implements Runnable {

    private FtpServer server;

    private FtpServerFactory serverFactory;
    private ListenerFactory listenerFactory;

    private UserManager um;
    private PropertiesUserManagerFactory userManagerFactory;

    private String TAG = "FTPService-Thread";

    @Override
    public void run() {

        try {
            ftpServerSetUp();
        } catch (IOException e) {
            e.printStackTrace();
        }

        server = serverFactory.createServer();
        try {
            server.start(); //Your FTP server starts listening for incoming FTP-connections, using the configuration options previously set
        } catch (FtpException ex) {
            ex.printStackTrace();
        }
    }

    public void ftpServerSetUp() throws IOException {

        //setting up a server Factory
        serverFactory = new FtpServerFactory();
        listenerFactory = new ListenerFactory();

        //defineSSLConf();
        defineListener();
        setUpUser();

        serverFactory.setUserManager(um);
        Map<String, Ftplet> m = new HashMap<>();
        m.put("miaFtplet", new FtpLetImpl());
        serverFactory.setFtplets(m);

        Map<String, Ftplet> mappa = serverFactory.getFtplets();

        Logger.instance.addLogEntry(LogType.INFO, TAG, String.valueOf(mappa.size()));
        Logger.instance.addLogEntry(LogType.INFO, TAG,"Thread #" + Thread.currentThread().getId());
        Logger.instance.addLogEntry(LogType.INFO, TAG, mappa.toString());
    }

    private void defineSSLConf() {
        // define SSL configuration
        SslConfigurationFactory ssl = new SslConfigurationFactory();
        ssl.setKeystoreFile(new File("./ftp/ftpServer.jks"));
        ssl.setKeystorePassword("password");

        // set the SSL configuration for the listener
        listenerFactory.setSslConfiguration(ssl.createSslConfiguration());
        listenerFactory.setImplicitSsl(true);
    }

    private void defineListener() {
        listenerFactory.setPort(ftpPort); // set the port of the listener (choose your desired port, not 1234)
        serverFactory.addListener("default", listenerFactory.createListener()); // adding a "default Listener"
        userManagerFactory = new PropertiesUserManagerFactory(); // adding a new UserManagementClass
        userManagerFactory.setFile(new File(ftpServiceUserPropertiesFile));//choose any. We're telling the FTP-server where to read its user list
        userManagerFactory.setPasswordEncryptor(new PasswordEncryptorsImpl()); // encrypts passwords of users by using the EncrImpl
        serverFactory.setUserManager(um);
    }

    private void setUpUser() throws IOException {
        //Let's add a user, since our userList.properties file is empty on our first test run
        BaseUser user = new BaseUser();

        user.setName(standardUserName);
        user.setPassword(standardUserPassword);
        user.setHomeDirectory(ftpServiceMapDir);

        List<Authority> authorities = new ArrayList<>();
        // adding things for User ( example : new WritePermission() )
        // authorities.add()
        user.setAuthorities(authorities);

        try {
            um = userManagerFactory.createUserManager();
        } catch (FtpServerConfigurationException e) {
                new File(ftpDefaultDir).mkdirs();
                new File(ftpServiceUserPropertiesFile).createNewFile();
            um = userManagerFactory.createUserManager();
        }

        try {
            um.save(user);//Save the user to the user list on the filesystem
        } catch (FtpException e1) {
            //Deal with exception as you need
        }
    }
}
