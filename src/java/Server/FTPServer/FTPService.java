package Server.FTPServer;

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
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;

import static Server.StaticVariables.*;

public class FTPService implements Runnable {

    private FtpServer server;

    private FtpServerFactory serverFactory;
    private ListenerFactory listenerFactory;

    private UserManager um;
    private PropertiesUserManagerFactory userManagerFactory;

    private boolean LOGGING = doesFTPServiceLog;
    private File logFile = new File(ftpLogFile);
    private PrintStream logStream;

    private String LOG_TAG = "FTPService";

    @Override
    public void run() {
        try {
            logFile.createNewFile();
            logStream = new PrintStream(new FileOutputStream(logFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        log(String.valueOf(mappa.size()));
        log("Thread #" + Thread.currentThread().getId());
        log(mappa.toString());
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
        authorities.add(new WritePermission());
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

    public void log(String msg) {
        if (!LOGGING)
            return;

        if (logStream == null)
            System.err.println("ERROR: PrintStream for log not initialized");

        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date date = new Date(System.currentTimeMillis());
        logStream.println(LOG_TAG + formatter.format(date) + " | " + msg);
    }
}
