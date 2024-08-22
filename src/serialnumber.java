public class serialnumber {
    private static serialnumber instance;
    private static int serialnumber;

    private serialnumber() {
    }

    public static serialnumber getInstance() {
        if (instance == null) {
            instance = new serialnumber();
            serialnumber = 0;
        }
        return instance;
    }

    public int getValue() {
        serialnumber++;
        return serialnumber;
    }
}
