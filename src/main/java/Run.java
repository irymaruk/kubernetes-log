import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import io.fabric8.kubernetes.api.model.Pod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Run {
    private static final String PROPERTIES_FILE_NAME = "kubernetes-log.properties";
    private static final Properties properties = readProperties();
    private static String context;
    private static String label = "clinicalevaluation";
    private static BlockingQueue<LogTO> queue = new LinkedBlockingQueue();
    private static ExecutorService executor = Executors.newFixedThreadPool(10);
    private static KubernetesUtils kube;

    private final static Logger logger = LoggerFactory.getLogger(Run.class);

    public static void main(String[] args) {
        readParams(args);
        List<Pod> pods = kube.getPodsByLabel(label);
        logger.info("Pod Names: ");
        pods.forEach(p -> logger.info(kube.getPodName(p)));
        for (Pod pod : pods) {
            InputStream is = kube.tailLogForPod(pod);
            String podName = kube.getPodName(pod);
            executor.execute(new Producer(queue, is, podName));
        }
        executor.execute(new Consumer(queue));
        stopExecutorService(executor);
    }

    /**
     * Read environment and podName params from program arguments or stdin
     *
     * @param args - first env, second podName
     */
    private static void readParams(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String readEnv = null;
        Integer readLabel = null;

        if (args.length > 0) readEnv = args[0];
        if (args.length > 1) readLabel = Integer.valueOf(args[1]);
        // read env
        HashMap<String, String> env = new HashMap<>();
        env.put("1", "dev");
        env.put("2", "devqe");
        env.put("3", "sit");
        env.put("4", "e2e");
        if (readEnv == null) {
            logger.info("Select environment:");
            env.forEach((k, v) -> logger.info(String.format("%s. %s", k, v)));
            readEnv = scanner.nextLine();
        }
        if (!env.containsKey(readEnv)) {
            throw new IllegalArgumentException("Please use only this values: " + env.keySet());
        }
        context = env.get(readEnv);
        logger.info("Env = " + context);

        // read labels
        if (readLabel == null) logger.info("\nSelect microservice:");
        String useConfig = properties.getProperty("kubeconfig.useintegrated", "true");
        kube = new KubernetesUtils(context, useConfig.equals("true"));
        List<String> labels = filterLabels(kube.getAllAppLabels());
        StringBuilder stringBuilder = new StringBuilder();
        int columns = new Integer(Optional.ofNullable(properties.getProperty("services.print.columns")).orElse("3"));
        for (int i = 1; i <= labels.size(); i++) {
            stringBuilder.append(String.format("%3s. %-40s", i, labels.get(i - 1)));
            if ((i % columns) == 0) {
                stringBuilder.append(System.lineSeparator());
            }
        }
        if (readLabel == null) {
            logger.info(stringBuilder.toString());
            readLabel = Integer.valueOf(scanner.nextLine());
        }
        if (readLabel < 1 || readLabel > labels.size())
            throw new IllegalArgumentException("Please use only this values: 1 - " + labels.size());
        label = labels.get(readLabel - 1);
        logger.info("Label number = " + readLabel + " name = " + label);
        initFileLogger("all.log", true);
        initFileLogger(String.format("%s-%s.log", context, label), false);
    }

    private static void initFileLogger(String fileName, boolean append) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        FileAppender fileAppender = new FileAppender();
        fileAppender.setContext(loggerContext);
        fileAppender.setAppend(append);
        String logFileFullPath = String.format("%s/log/%s", getJarPath(), fileName);
        fileAppender.setFile(logFileFullPath);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%msg%n");
        encoder.start();

        fileAppender.setEncoder(encoder);
        fileAppender.start();

        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(fileAppender);
    }

    private static void stopExecutorService(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private static Properties readProperties() {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(getJarPath() + File.separator + PROPERTIES_FILE_NAME));
        } catch (Exception ignore) {
        }
        return props;
    }

    private static String getJarPath() {
        return new File(Run.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getAbsolutePath();
    }

    private static List<String> filterLabels(List<String> labels) {
        final String filter = properties.getProperty("services.filter.regexp");
        if (null == filter) {
            return labels;
        } else {
            logger.info("Apply filter: " + filter);
            final List<String> filtered = labels.stream()
                    .filter(Objects::nonNull)
                    .filter(s -> s.matches(filter))
                    .collect(Collectors.toList());
            if (filtered.size() == 0) {
                logger.info("No microservices found with provided filter");
                System.exit(1);
            }
            return filtered;
        }
    }
}
