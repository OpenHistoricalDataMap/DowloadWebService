package Server.FTPServer;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FTPService {
    public void ftpServer() {
        //setting up a server Factory
        FtpServerFactory serverFactory = new FtpServerFactory();
        ListenerFactory factory = new ListenerFactory();

        factory.setPort(5000);// set the port of the listener (choose your desired port, not 1234)
        serverFactory.addListener("default", factory.createListener()); // adding a "default Listener"
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory(); // adding a new UserManagementClass
        userManagerFactory.setFile(new File("~/ftp/userList.properties"));//choose any. We're telling the FTP-server where to read its user list
        userManagerFactory.setPasswordEncryptor(new PasswordEncryptorsImpl()); // encrypts passwords of users by using the EncrImpl

        //Let's add a user, since our userList.properties file is empty on our first test run
        BaseUser user = new BaseUser();

        user.setName("ohdmOffViewer");
        user.setPassword("H!3r0glyph Sat3llite Era$er");
        user.setHomeDirectory("/map");

        List<Authority> authorities = new ArrayList<>();
        authorities.add(new WritePermission());
        user.setAuthorities(authorities);
        UserManager um = userManagerFactory.createUserManager();

        try {
            um.save(user);//Save the user to the user list on the filesystem
        } catch (FtpException e1) {
            //Deal with exception as you need
        }

        serverFactory.setUserManager(um);
        Map<String, Ftplet> m = new HashMap<>();
        m.put("miaFtplet", new FtpLetImpl());

        serverFactory.setFtplets(m);

        //Map<String, Ftplet> mappa = serverFactory.getFtplets();
        //System.out.println(mappa.size());
        //System.out.println("Thread #" + Thread.currentThread().getId());
        //System.out.println(mappa.toString());

        FtpServer server = serverFactory.createServer();
        try {
            server.start();//Your FTP server starts listening for incoming FTP-connections, using the configuration options previously set
        } catch (FtpException ex) {
            //Deal with exception as you need
        }
    }
}
