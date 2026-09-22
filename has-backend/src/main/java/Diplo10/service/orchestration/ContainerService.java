package Diplo10.service.orchestration;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class ContainerService {
    static final String DEFAULT_FLAG_PATH = "/home/flag.txt";

    private final DockerClient dockerClient;

    public ContainerService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public SpawnResult spawnChallengeContainer(String imageName, String teamName, String networkId) {
        // Read flag_path label from the image
        var imageInspect = dockerClient.inspectImageCmd(imageName).exec();
        var imageLabels = imageInspect.getConfig().getLabels();
        String flagPath = safeFlagPath(
                imageLabels != null ? imageLabels.get("flag_path") : null);

        int points = 100; // default
        if (imageLabels != null && imageLabels.containsKey("points")) {
            try {
                points = Integer.parseInt(imageLabels.get("points"));
            } catch (NumberFormatException ignored) {}
        }

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withNetworkMode(networkId)
                .withMemory(128 * 1024 * 1024L) // 128MB RAM Limit (Sicherheit/Skalierbarkeit)
                .withCpuQuota(50000L)            // 0.5 CPU Cores Limit
                .withRestartPolicy(RestartPolicy.unlessStoppedRestart());

        String safeName = safeContainerName(teamName);
        String containerId = dockerClient.createContainerCmd(imageName)
                .withName(safeName + "_" + System.currentTimeMillis())
                .withHostConfig(hostConfig)
                .withNetworkMode(networkId)
                .withLabels(java.util.Map.of("owner", teamName, "type", "challenge"))
                .exec().getId();

        dockerClient.startContainerCmd(containerId).exec();

        var inspectResponse = dockerClient.inspectContainerCmd(containerId).exec();

        String ipAddress = "";
        var networks = inspectResponse.getNetworkSettings().getNetworks();
        if (networks != null) {
            for (var entry : networks.values()) {
                if (networkId.equals(entry.getNetworkID())
                        && entry.getIpAddress() != null && !entry.getIpAddress().isEmpty()) {
                    ipAddress = entry.getIpAddress();
                    break;
                }
            }
            // fallback: take any non-empty IP
            if (ipAddress.isEmpty()) {
                for (var entry : networks.values()) {
                    if (entry.getIpAddress() != null && !entry.getIpAddress().isEmpty()) {
                        ipAddress = entry.getIpAddress();
                        break;
                    }
                }
            }
        }

        // Generate random flag and write it into the container
        String flag = generateFlag();
        String execId = dockerClient.execCreateCmd(containerId)
                .withCmd("sh", "-c", "echo '" + flag + "' > " + flagPath)
                .exec().getId();
        dockerClient.execStartCmd(execId).exec(new ResultCallback.Adapter<Frame>());

        // Generate root password and set it in the container
        String rootPassword = generateRootPassword();
        String pwExecId = dockerClient.execCreateCmd(containerId)
                .withCmd("sh", "-c", "echo 'root:" + rootPassword + "' | chpasswd")
                .exec().getId();
        dockerClient.execStartCmd(pwExecId).exec(new ResultCallback.Adapter<Frame>());

        return new SpawnResult(containerId, ipAddress, flag, points, rootPassword, flagPath);
    }

    /**
     * Docker only accepts [a-zA-Z0-9_.-] in container names, so team names the
     * players chose freely have to be folded into that set.
     */
    static String safeContainerName(String teamName) {
        return teamName.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    /**
     * The flag path is interpolated into a shell command, so a crafted image
     * label could otherwise run arbitrary commands inside the challenge
     * container. Anything that is not a plain absolute path falls back to the
     * default.
     */
    static String safeFlagPath(String labelValue) {
        if (labelValue == null
                || !labelValue.matches("/[A-Za-z0-9._/-]*")
                || labelValue.contains("..")
                || labelValue.endsWith("/")) {
            return DEFAULT_FLAG_PATH;
        }
        return labelValue;
    }

    private String generateRootPassword() {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generateFlag() {
        SecureRandom random = new SecureRandom();
        StringBuilder hex = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            hex.append(Integer.toHexString(random.nextInt(16)));
        }
        return "HAS{" + hex + "}";
    }
}