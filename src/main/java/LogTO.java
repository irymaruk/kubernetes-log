public class LogTO {

    String podName;
    String message;

    public LogTO(String podName, String message) {
        this.podName = podName;
        this.message = message;
    }

    public String getPodName() {
        return podName;
    }

    public String getMessage() {
        return message;
    }
}