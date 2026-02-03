package Diplo10.service.orchestration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.InvocationBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class MonitoringService {
    private final DockerClient dockerClient;

    public MonitoringService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    // Läuft alle 30 Sekunden
    @Scheduled(fixedRate = 30000)
    public void monitorAndScale() {
        dockerClient.listContainersCmd().withLabelFilter(java.util.Map.of("type", "challenge")).exec()
                .forEach(container -> {

                    // Code für Scaling und richtiges Monitoring folgt noch

                    System.out.println("Monitoring Container: " + container.getId());
                });
    }
}