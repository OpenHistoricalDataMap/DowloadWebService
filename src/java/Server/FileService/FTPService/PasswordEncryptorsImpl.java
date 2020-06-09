package Server.FileService.FTPService;

public class PasswordEncryptorsImpl implements org.apache.ftpserver.usermanager.PasswordEncryptor {
    @Override
    public String encrypt(String password) {
        return password;
    }

    @Override
    public boolean matches(String passwordToCheck, String storedPassword) {
        return passwordToCheck.equals(storedPassword);
    }
}
