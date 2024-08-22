public class Portnumber {
    private static Portnumber instance;
    private static int portnumber;

    private Portnumber() {
    }

    public static Portnumber getInstance() {
        if (instance == null) {
            instance = new Portnumber();
            portnumber = 1023;
        }
        return instance;
    }

    public int getValue() {
        portnumber++;
        return portnumber;
    }
}
