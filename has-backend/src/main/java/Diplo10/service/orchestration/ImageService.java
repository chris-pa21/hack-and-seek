package Diplo10.service.orchestration;

import Diplo10.dto.ChallengeDTO;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ImageService {
    private final DockerClient dockerClient;

    public ImageService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public List<ChallengeDTO> listChallengeImages() {
        List<Image> images = dockerClient.listImagesCmd()
                .withLabelFilter(Map.of("type", "challenge"))
                .exec();

        List<ChallengeDTO> challenges = new ArrayList<>();
        for (Image image : images) {
            String[] tags = image.getRepoTags();
            if (tags != null) {
                int difficulty = 0;
                Map<String, String> labels = image.getLabels();
                if (labels != null && labels.containsKey("difficulty")) {
                    try {
                        difficulty = Integer.parseInt(labels.get("difficulty"));
                    } catch (NumberFormatException ignored) {}
                }

                for (String tag : tags) {
                    String name = tag.contains(":") ? tag.substring(0, tag.lastIndexOf(':')) : tag;
                    int finalDifficulty = difficulty;
                    if (challenges.stream().noneMatch(c -> c.name().equals(name))) {
                        challenges.add(new ChallengeDTO(name, finalDifficulty));
                    }
                }
            }
        }
        return challenges;
    }
}
