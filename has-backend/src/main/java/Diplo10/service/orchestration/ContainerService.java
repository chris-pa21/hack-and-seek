package Diplo10.service.orchestration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import org.springframework.stereotype.Service;

@Service
public class ContainerService {
    private final DockerClient dockerClient;

    public ContainerService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public String spawnChallengeContainer(String imageName, String teamName, String networkId) {
        HostConfig hostConfig = HostConfig.newHostConfig()
                .withNetworkMode(networkId)
                .withMemory(512 * 1024 * 1024L) // 512MB RAM Limit (Sicherheit/Skalierbarkeit)
                .withCpuQuota(50000L)            // 0.5 CPU Cores Limit
                .withRestartPolicy(RestartPolicy.unlessStoppedRestart());

        String containerId = dockerClient.createContainerCmd(imageName)
                .withName(teamName + "_" + System.currentTimeMillis())
                .withHostConfig(hostConfig)
                .withLabels(java.util.Map.of("owner", teamName, "type", "challenge"))
                .exec().getId();

        dockerClient.startContainerCmd(containerId).exec();
        return containerId;
    }
}