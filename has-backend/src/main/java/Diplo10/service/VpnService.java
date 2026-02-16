package Diplo10.service;

import com.github.dockerjava.api.DockerClient;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;

@Service
public class VpnService {
    private final DockerClient docker;

    public VpnService(DockerClient docker) {
        this.docker = docker;
    }

    public void includeNetwork(String networkId) {
        final String containerId = "has-wireguard-server";
        try {
            docker.connectToNetworkCmd()
                .withContainerId(containerId)
                .withNetworkId(networkId)
                .exec();

            System.out.println("Successfully attached WireGuard to network: " + networkId);
        } catch (Exception e) {
            System.err.println("Error connecting to network: " + e.getMessage());
        }
    }

    

    public void connect() {

    }
}
