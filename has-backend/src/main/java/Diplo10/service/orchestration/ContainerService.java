package Diplo10.service.orchestration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.RestartPolicy;
import org.springframework.stereotype.Service;
import com.github.dockerjava.api.model.ExposedPort;

@Service
public class ContainerService {
    private final DockerClient dockerClient;

    public ContainerService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public String spawnChallengeContainer(String imageName, String teamName, String networkId) {
        ExposedPort tcp80 = ExposedPort.tcp(80);
        Ports portBindings = new Ports();
        portBindings.bind(tcp80, Ports.Binding.empty());

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withNetworkMode(networkId)
                .withMemory(512 * 1024 * 1024L) // 512MB RAM Limit (Sicherheit/Skalierbarkeit)
                .withCpuQuota(50000L)            // 0.5 CPU Cores Limit
                .withRestartPolicy(RestartPolicy.unlessStoppedRestart());

        String containerId = dockerClient.createContainerCmd(imageName)
                .withName(teamName + "_" + System.currentTimeMillis())
                .withHostConfig(hostConfig.newHostConfig())
                .withNetworkMode(networkId)
                .withPortBindings(portBindings)
                .withExposedPorts(tcp80)
                .withLabels(java.util.Map.of("owner", teamName, "type", "challenge"))
                .exec().getId();

        dockerClient.startContainerCmd(containerId).exec();

        var inspectResponse = dockerClient.inspectContainerCmd(containerId).exec();
        var bindings = inspectResponse.getNetworkSettings().getPorts().getBindings().get(tcp80);

        if (bindings != null && bindings.length > 0) {
            String hostPort = bindings[0].getHostPortSpec();
            // Diesen Port geben wir jetzt zurück oder speichern ihn direkt
            return containerId + ":" + hostPort;
        }
        return containerId;
    }
}