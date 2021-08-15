
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Consumer implements Runnable {
    private final BlockingQueue<LogTO> sharedQueue;
    private String currentPodName;
    private final static Logger logger = LoggerFactory.getLogger(Consumer.class);

    public Consumer(BlockingQueue<LogTO> sharedQueue) {
        this.sharedQueue = sharedQueue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                LogTO line = sharedQueue.poll(1, TimeUnit.SECONDS);
                if (line != null) {
                    if (currentPodName == null) {
                        logger.info(String.format("%n>>>>> %s", line.getPodName()));
                        currentPodName = line.getPodName();
                    } else if (!currentPodName.equals(line.getPodName())) {
                        logger.info(String.format("<<<<< %s %n%n>>>>> %s", currentPodName, line.getPodName()));
                        currentPodName = line.getPodName();
                    }
                    logger.info(line.getMessage());
                }
            } catch (InterruptedException e) {
            }
        }
    }

    public static Long convertStringToMilli(Object dateSting) {
        return LocalDateTime.parse(dateSting.toString(), DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS"))
                .atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
    }
}
