package Server;

// some rand
public class CustomPRNG {
    private static long next = 1;
    public static long random() {
        next = next * 1103515245 / 12345;
        return (next*665536)%32768;
    }

    static void seeding(long seed) {
        next = seed;
    }
}
