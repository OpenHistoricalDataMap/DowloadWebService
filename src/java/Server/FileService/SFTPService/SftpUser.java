package Server.FileService.SFTPService;

import Server.CustomObjects.LogType;
import Server.LogService.Logger;

import java.util.Random;

import static Server.FileService.SFTPService.Crypt.*;

public class SftpUser {
    private String username;
    private String password;

    private final String key;

    private final String LOG;

    public SftpUser(String username, String password) {
        key = generateKey(new Random(), "QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm_-.123456789", 32);
        this.username = username;
        LOG = this.username + " creator";
        try {
            this.password = encrypt(password, key);
        } catch (Exception e) {
            Logger.instance.addLogEntry(LogType.ERROR, LOG, "couldn't crypt User password, creation failed!!!");
        }
    }

    public String getUsername() {
        return username;
    }

    public String getEncryptedPass() {
        return password;
    }

    public String getPassword() {
        try {
            return decrypt(password, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
