package Server.CustomObjects;

public class Coords {
    public Double x;
    public Double y;

    public Coords(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return String.valueOf(x + ' ' + y);
    }
}
