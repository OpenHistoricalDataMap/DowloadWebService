package Server.FileService.SFTPService.SftpClient;

import java.io.Serializable;

public class RemoteDirectory implements Serializable
{

    //------------Instance Variables------------

    private String filename;
    private String path;
    private String creationDate;


    //------------Constructors------------

    public RemoteDirectory(String path, String creationDate)
    {
        this.setPath(path);
        this.setCreationDate(creationDate);
    }


    //------------Setter------------

    public String getFilename()
    {
        return filename;
    }

    private void setFilename(String path)
    {
        this.filename = path.replace("_", " ").trim();
    }

    public String getPath()
    {
        return path;
    }


    //------------Getter------------

    public void setPath(String path)
    {
        this.path = path;
        this.setFilename(path);
    }

    public String getCreationDate()
    {
        return creationDate;
    }

    public void setCreationDate(String creationDate)
    {
        this.creationDate = creationDate;
    }
}
