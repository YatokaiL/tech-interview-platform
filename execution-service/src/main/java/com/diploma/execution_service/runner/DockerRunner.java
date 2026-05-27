package com.diploma.execution_service.runner;

import com.diploma.execution_service.entity.ExecutionStatus;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DockerRunner {

    private final DockerClient docker;
    private final Duration timeout;
    private final long memoryBytes;
    private final long cpuQuota;
    private final long cpuPeriod;
    private final long pidsLimit;
    private final int stdoutTruncateBytes;

    private final Set<String> imagesReady = new HashSet<>();

    public DockerRunner(DockerClient docker,
                        @Value("${app.execution.timeout}") Duration timeout,
                        @Value("${app.execution.memory-bytes}") long memoryBytes,
                        @Value("${app.execution.cpu-quota}") long cpuQuota,
                        @Value("${app.execution.cpu-period}") long cpuPeriod,
                        @Value("${app.execution.pids-limit}") long pidsLimit,
                        @Value("${app.execution.stdout-truncate-bytes}") int stdoutTruncateBytes) {
        this.docker = docker;
        this.timeout = timeout;
        this.memoryBytes = memoryBytes;
        this.cpuQuota = cpuQuota;
        this.cpuPeriod = cpuPeriod;
        this.pidsLimit = pidsLimit;
        this.stdoutTruncateBytes = stdoutTruncateBytes;
    }

    public ExecutionResult run(LanguageProfile profile, String code, String stdin) {
        String containerId = null;
        long startNs = System.nanoTime();
        try {
            ensureImage(profile.image());
            startNs = System.nanoTime();

            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withNetworkMode("none")
                    .withMemory(memoryBytes)
                    .withMemorySwap(memoryBytes)
                    .withCpuQuota(cpuQuota)
                    .withCpuPeriod(cpuPeriod)
                    .withPidsLimit(pidsLimit)
                    .withCapDrop(Capability.ALL)
                    .withSecurityOpts(java.util.List.of("no-new-privileges"));

            RunPlan plan = planRun(profile, code);

            String runCmd = plan.runCommand();
            if (stdin != null && !stdin.isEmpty()) runCmd += " < /sandbox/stdin";

            CreateContainerResponse created = docker.createContainerCmd(profile.image())
                    .withHostConfig(hostConfig)
                    .withWorkingDir("/sandbox")
                    .withEntrypoint("sh", "-c")
                    .withCmd(runCmd)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withTty(false)
                    .exec();
            containerId = created.getId();

            copyCodeInto(containerId, plan.fileName(), code, stdin);

            docker.startContainerCmd(containerId).exec();

            WaitContainerResultCallback waitCb = docker.waitContainerCmd(containerId).start();
            Integer exitCode;
            try {
                exitCode = waitCb.awaitStatusCode(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (DockerClientException timedOut) {
                forceKill(containerId);
                StreamCapture partial = collectLogs(containerId);
                long dur = (System.nanoTime() - startNs) / 1_000_000L;
                return new ExecutionResult(ExecutionStatus.TIMEOUT, null, partial.stdout(), partial.stderr(), dur,
                        "Execution exceeded " + timeout.toMillis() + " ms and was terminated");
            }

            StreamCapture cap = collectLogs(containerId);
            long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
            int code1 = exitCode == null ? -1 : exitCode;
            ExecutionStatus status = code1 == 0 ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILURE;
            return new ExecutionResult(status, code1, cap.stdout(), cap.stderr(), durationMs, null);
        } catch (Exception e) {
            log.error("Docker run failed", e);
            long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
            return new ExecutionResult(ExecutionStatus.ERROR, null, "", "", durationMs,
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (containerId != null) {
                removeQuiet(containerId);
            }
        }
    }

    private record RunPlan(String fileName, String runCommand) {}

    private static final java.util.regex.Pattern JAVA_PUBLIC_CLASS =
            java.util.regex.Pattern.compile("(?m)public\\s+(?:final\\s+|abstract\\s+)?class\\s+([A-Za-z_$][A-Za-z0-9_$]*)");
    private static final java.util.regex.Pattern JAVA_ANY_CLASS =
            java.util.regex.Pattern.compile("(?m)\\bclass\\s+([A-Za-z_$][A-Za-z0-9_$]*)");

    private RunPlan planRun(LanguageProfile profile, String code) {
        if ("java".equals(profile.key())) {
            String className = "Main";
            java.util.regex.Matcher pub = JAVA_PUBLIC_CLASS.matcher(code);
            if (pub.find()) {
                className = pub.group(1);
            } else {
                java.util.regex.Matcher any = JAVA_ANY_CLASS.matcher(code);
                if (any.find()) className = any.group(1);
            }
            String runCmd = "javac /sandbox/" + className + ".java -d /sandbox && cd /sandbox && java " + className;
            return new RunPlan(className + ".java", runCmd);
        }
        return new RunPlan(profile.fileName(), profile.runCommand());
    }

    private void ensureImage(String image) throws InterruptedException {
        if (imagesReady.contains(image)) return;
        try {
            docker.inspectImageCmd(image).exec();
        } catch (NotFoundException missing) {
            log.info("Pulling image {}", image);
            docker.pullImageCmd(image).exec(new PullCallback()).awaitCompletion(5, TimeUnit.MINUTES);
        }
        imagesReady.add(image);
    }

    private void copyCodeInto(String containerId, String fileName, String code, String stdin) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(baos)) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            addDirEntry(tar, "sandbox/");
            addFileEntry(tar, "sandbox/" + fileName, code);
            if (stdin != null && !stdin.isEmpty()) {
                addFileEntry(tar, "sandbox/stdin", stdin);
            }
            tar.finish();
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray())) {
            docker.copyArchiveToContainerCmd(containerId)
                    .withRemotePath("/")
                    .withTarInputStream(bais)
                    .exec();
        }
    }

    private void addDirEntry(TarArchiveOutputStream tar, String name) throws Exception {
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setMode(0755);
        tar.putArchiveEntry(entry);
        tar.closeArchiveEntry();
    }

    private void addFileEntry(TarArchiveOutputStream tar, String name, String content) throws Exception {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setSize(bytes.length);
        entry.setMode(0644);
        tar.putArchiveEntry(entry);
        tar.write(bytes);
        tar.closeArchiveEntry();
    }

    private StreamCapture collectLogs(String containerId) throws InterruptedException {
        StreamCapture cap = new StreamCapture(stdoutTruncateBytes);
        docker.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(false)
                .withTimestamps(false)
                .exec(new LogContainerResultCallback() {
                    @Override
                    public void onNext(Frame item) {
                        cap.append(item.getStreamType(), item.getPayload());
                    }
                })
                .awaitCompletion(5, TimeUnit.SECONDS);
        return cap;
    }

    private void forceKill(String containerId) {
        try {
            docker.killContainerCmd(containerId).exec();
        } catch (Exception ignored) {}
    }

    private void removeQuiet(String containerId) {
        try {
            docker.removeContainerCmd(containerId).withForce(true).exec();
        } catch (Exception e) {
            log.debug("removeContainer failed for {}: {}", containerId, e.getMessage());
        }
    }

    private static final class PullCallback extends PullImageResultCallback implements ResultCallback<PullResponseItem> {}

    private static final class StreamCapture {
        private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        private final int max;

        StreamCapture(int max) { this.max = max; }

        void append(StreamType type, byte[] payload) {
            ByteArrayOutputStream sink = (type == StreamType.STDERR) ? stderr : stdout;
            int remaining = max - sink.size();
            if (remaining <= 0) return;
            int len = Math.min(remaining, payload.length);
            sink.write(payload, 0, len);
        }

        String stdout() { return stdout.toString(StandardCharsets.UTF_8); }
        String stderr() { return stderr.toString(StandardCharsets.UTF_8); }
    }
}
