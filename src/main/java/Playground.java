import Server.StaticVariables;

import java.io.*;

import static Server.StaticVariables.*;

class Playground {
    public static void main(String[] args) {
        StaticVariables.init();

        System.out.println(standardUserName);
        System.out.println(ftpPort);
        System.out.println(logDefaultDir);
    }
}