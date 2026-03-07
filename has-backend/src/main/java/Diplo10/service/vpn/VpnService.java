package Diplo10.service.vpn;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Network;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VpnService {
    private final DockerClient docker;
    private final VpnManagerClient vpnManagerClient;

    public VpnService(DockerClient docker, VpnManagerClient vpnManagerClient) {
        this.docker = docker;
        this.vpnManagerClient = vpnManagerClient;
    }

    public void includeNetwork(String networkId) {
        final String containerId = "has-wireguard-server-1";
        try {
            // 1. Attach to network
            try {
                docker.connectToNetworkCmd()
                    .withContainerId(containerId)
                    .withNetworkId(networkId)
                    .exec();
                System.out.println("Successfully attached WireGuard to network: " + networkId);
            } catch (Exception e) {
                if (e.getMessage().contains("already exists in network") || e.getMessage().contains("already connected")) {
                    System.out.println("WireGuard already connected to network: " + networkId);
                } else {
                    throw e;
                }
            }

            // 2. Get subnet from network inspection
            String subnet = getSubnet(networkId);
            if (subnet != null) {
                vpnManagerClient.addSubnet(subnet);
                System.out.println("Applied firewall rule for subnet: " + subnet);
            }

        } catch (Exception e) {
            System.err.println("Error in VpnService.includeNetwork: " + e.getMessage());
        }
    }

    public void excludeNetwork(String networkId) {
        final String containerId = "has-wireguard-server-1";
        try {
            // 1. Get subnet before detaching (to identify which rule to delete)
            String subnet = getSubnet(networkId);
            if (subnet != null) {
                vpnManagerClient.deleteSubnet(subnet);
                System.out.println("Removed firewall rule for subnet: " + subnet);
            }

            // 2. Detach from network
            docker.disconnectFromNetworkCmd()
                .withContainerId(containerId)
                .withNetworkId(networkId)
                .exec();

            System.out.println("Successfully detached WireGuard from network: " + networkId);
        } catch (Exception e) {
            System.err.println("Error disconnecting from network: " + e.getMessage());
        }
    }

    private String getSubnet(String networkId) {
        try {
            Network network = docker.inspectNetworkCmd().withNetworkId(networkId).exec();
            List<Network.Ipam.Config> configs = network.getIpam().getConfig();
            if (configs != null && !configs.isEmpty()) {
                return configs.get(0).getSubnet();
            }
        } catch (Exception e) {
            System.err.println("Error inspecting network " + networkId + ": " + e.getMessage());
        }
        return null;
    }

    public void addPeer() {

    }

    public void removePeer() {

    }

    public String getVpnConfig(int peerNumber) {
        final String containerId = "has-wireguard-server-1";
        final String configPath = "/config/peer" + peerNumber + "/peer" + peerNumber + ".conf";

        try {
            String catResult = execInContainer(containerId, "cat", configPath);
            return catResult;
        } catch (Exception e) {
            System.err.println("Error fetching VPN config: " + e.getMessage());
            return null;
        }
    }

    private String execInContainer(String containerId, String... command) {
        try {
            String execId = docker.execCreateCmd(containerId)
                    .withCmd(command)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec().getId();

            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            docker.execStartCmd(execId).exec(new com.github.dockerjava.api.async.ResultCallback.Adapter<com.github.dockerjava.api.model.Frame>() {
                @Override
                public void onNext(com.github.dockerjava.api.model.Frame item) {
                    try {
                        outputStream.write(item.getPayload());
                    } catch (java.io.IOException ignored) {}
                }
            }).awaitCompletion();
            
            return outputStream.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
