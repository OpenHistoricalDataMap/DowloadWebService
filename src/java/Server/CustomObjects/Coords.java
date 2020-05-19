package Server.CustomObjects;

public class Coords {
    float x;
    float y;

    public Coords(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return String.valueOf(x + ' ' + y);
    }
}
