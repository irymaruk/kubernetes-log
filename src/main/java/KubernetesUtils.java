import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class KubernetesUtils {
    private KubernetesClient client;
    private String context;
    private boolean useIntegratedConfigFile;

    KubernetesUtils(String context, boolean useIntegratedConfigFile) {
        this.context = context;
        this.useIntegratedConfigFile = useIntegratedConfigFile;
        getClient();
    }

    public KubernetesClient getClient() {
        if (client != null) return client;
        else {
            if (useIntegratedConfigFile) {
                client = new DefaultKubernetesClient(getConfig());
            } else {
                client = new DefaultKubernetesClient(Config.autoConfigure(context));
            }
            return client;
        }
    }

    private Config getConfig() {
        Config config = null;
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("config")) {
            Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
            String content = scanner.next();
            config = Config.fromKubeconfig(context, content, null);
        } catch (IOException e) {
        }
        return config;
    }


    public List<Pod> getPodsByLabel(String appLabel) {
        return client.pods().withLabelIn("app", appLabel).list().getItems();
    }

    public String getPodName(Pod pod) {
        return pod.getMetadata().getName();
    }

    public List<String> getContainerNames(Pod pod) {
        return pod.getSpec()
                .getContainers()
                .stream()
                .map(Container::getName)
                .collect(Collectors.toList());
    }

    public String getContainerName(Pod pod) {
        List<String> podContainerNames = getContainerNames(pod);
        if (podContainerNames.isEmpty() || podContainerNames.size() > 2) {
            throw new RuntimeException("please check a logic to get container name from list: " + podContainerNames);
        }
        String first = podContainerNames.get(0);
        if (first.equals("filebeatapplogs")) {
            return podContainerNames.get(1);
        } else return first;
    }

    public InputStream tailLogForPod(Pod pod) {
        return client.pods()
                .withName(getPodName(pod))
                .inContainer(getContainerName(pod))
                .tailingLines(0)
                .watchLog()
                .getOutput();
    }

    public List<String> getAllAppLabels() {
        return client.pods()
                .list()
                .getItems()
                .stream()
                .map(p -> p.getMetadata().getLabels().get("app"))
                .distinct()
                .collect(Collectors.toList());
    }

}
