import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;

public class Producer implements Runnable {
    private BlockingQueue<LogTO> sharedQueue;
    private InputStream is;
    private String podName;

    public Producer(BlockingQueue<LogTO> sharedQueue, InputStream is, String podName) {
        this.sharedQueue = sharedQueue;
        this.is = is;
        this.podName = podName;
    }

    @Override
    public void run() {
        try (BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(is))) {
            do {
                try {
                    if (inputStreamReader.ready()) {
                        String line = inputStreamReader.readLine();
                        sharedQueue.put(new LogTO(podName, line));
                    } else {
                        Thread.sleep(100);
                    }
                } catch (InterruptedException | IOException ignored) {
                }
            } while (true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
