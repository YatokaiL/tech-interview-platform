package com.diploma.execution_service.runner;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.core.command.PullImageResultCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageWarmupRunner implements ApplicationRunner {

    private final DockerClient docker;
    private final LanguageProfiles profiles;

    @Value("${app.docker.pull-on-startup:false}")
    private boolean pullOnStartup;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (!pullOnStartup) {
            log.info("Image pull-on-startup is disabled; images will be pulled on first execution.");
            return;
        }
        ExecutorService pool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "image-warmer");
            t.setDaemon(true);
            return t;
        });
        pool.submit(() -> {
            for (LanguageProfile profile : profiles.all()) {
                pullIfMissing(profile.image());
            }
            log.info("Image warmup completed.");
        });
        pool.shutdown();
    }

    private void pullIfMissing(String image) {
        try {
            docker.inspectImageCmd(image).exec();
            log.debug("Image already present: {}", image);
        } catch (NotFoundException missing) {
            log.info("Pulling image {}", image);
            try {
                docker.pullImageCmd(image).exec(new PullImageResultCallback())
                        .awaitCompletion(10, TimeUnit.MINUTES);
                log.info("Pulled {}", image);
            } catch (Exception e) {
                log.warn("Failed to pull {}: {}", image, e.getMessage());
            }
        } catch (Exception e) {
            log.warn("inspectImage failed for {}: {}", image, e.getMessage());
        }
    }
}
