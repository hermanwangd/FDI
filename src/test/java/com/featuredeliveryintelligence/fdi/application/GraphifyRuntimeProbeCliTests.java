package com.featuredeliveryintelligence.fdi.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ports the CLI characterization cases of the transitional Python
 * {@code graphify_runtime_probe.py} main: {@code --command}/{@code --descriptor}
 * /{@code --output} handling, PATH discovery of {@code graphify} /
 * {@code graphify-cli}, the deterministic report on stdout and in the output
 * file, exit 0 on a described interface, exit 2 when the runtime stays
 * {@code NOT_VERIFIED}, and exit 1 with empty stdout on descriptor failures.
 */
class GraphifyRuntimeProbeCliTests {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir Path temp;

    @Test
    void handlesOnlyGraphifyRuntimeProbeCommand() {
        assertFalse(GraphifyRuntimeProbeCli.handles(new String[0]));
        assertFalse(GraphifyRuntimeProbeCli.handles(new String[] {"unrelated"}));
        assertTrue(GraphifyRuntimeProbeCli.handles(new String[] {"graphify-runtime-probe"}));
    }

    @Test
    void cliWritesReportToOutputAndStdoutWithExitZero() throws Exception {
        Path descriptor = validDescriptor();
        Path output = temp.resolve("nested/probe.json");

        Result result = run(new String[] {"graphify-runtime-probe",
                "--descriptor", descriptor.toString(), "--output", output.toString()});

        assertEquals(0, result.exitCode());
        assertEquals("", result.stderr());
        byte[] written = Files.readAllBytes(output);
        assertTrue(written.length > 0);
        assertEquals(new String(written, StandardCharsets.UTF_8), result.stdout());
        assertTrue(result.stdout().endsWith("}\n"), result.stdout());
        var report = JSON.readTree(result.stdout());
        assertEquals("INTERFACE_DESCRIBED_NOT_SNAPSHOT_BOUND", report.get("verification_status").asText());
        assertEquals("query_graph", report.get("supported_operations").get(0).asText());
    }

    @Test
    void cliPrintsReportToStdoutOnlyWithoutOutputFile() throws Exception {
        Path executable = temp.resolve("graphify");
        Files.write(executable, "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));

        Result result = run(new String[] {"graphify-runtime-probe", "--command", executable.toString()});

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().startsWith("{\n  \"runtime_found\": true,"), result.stdout());
        assertTrue(result.stdout().contains("\"verification_status\": \"DISCOVERED_NOT_VERIFIED\""),
                result.stdout());
    }

    @Test
    void cliExitsTwoWhenRuntimeNotVerified() {
        Result result = run(new String[] {"graphify-runtime-probe",
                "--command", temp.resolve("missing").toString()});

        assertEquals(2, result.exitCode());
        assertEquals("", result.stderr());
        assertTrue(result.stdout().contains("\"verification_status\": \"NOT_VERIFIED\""), result.stdout());
    }

    @Test
    void cliDiscoversGraphifyOnPath() throws Exception {
        Path bin = Files.createDirectories(temp.resolve("bin"));
        Path graphify = bin.resolve("graphify");
        Files.write(graphify, "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        graphify.toFile().setExecutable(true);

        Result result = runWithPath(List.of(bin),
                new String[] {"graphify-runtime-probe", "--descriptor", validDescriptor().toString()});

        assertEquals(0, result.exitCode());
        var report = JSON.readTree(result.stdout());
        assertTrue(report.get("runtime_found").asBoolean(), result.stdout());
        assertEquals(graphify.toRealPath().toString(), report.get("runtime_path").asText());
    }

    @Test
    void cliDiscoversGraphifyCliFallbackOnPath() throws Exception {
        Path bin = Files.createDirectories(temp.resolve("bin"));
        Path fallback = bin.resolve("graphify-cli");
        Files.write(fallback, "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
        fallback.toFile().setExecutable(true);

        Result result = runWithPath(List.of(bin), new String[] {"graphify-runtime-probe"});

        assertEquals(0, result.exitCode(), result.stdout());
        var report = JSON.readTree(result.stdout());
        assertTrue(report.get("runtime_found").asBoolean(), result.stdout());
        assertEquals(fallback.toRealPath().toString(), report.get("runtime_path").asText());
        assertEquals("DISCOVERED_NOT_VERIFIED", report.get("verification_status").asText());
    }

    @Test
    void cliExitsOneWithEmptyStdoutOnIncompleteDescriptor() throws Exception {
        Path descriptor = temp.resolve("descriptor.json");
        Files.write(descriptor, "{}\n".getBytes(StandardCharsets.UTF_8));

        Result result = run(new String[] {"graphify-runtime-probe", "--descriptor", descriptor.toString()});

        assertEquals(1, result.exitCode());
        assertEquals("", result.stdout());
        assertTrue(result.stderr().contains("Graphify descriptor is incomplete"), result.stderr());
    }

    @Test
    void cliExitsOneWithEmptyStdoutOnMalformedDescriptorJson() throws Exception {
        Path descriptor = temp.resolve("descriptor.json");
        Files.write(descriptor, "not json\n".getBytes(StandardCharsets.UTF_8));

        Result result = run(new String[] {"graphify-runtime-probe", "--descriptor", descriptor.toString()});

        assertEquals(1, result.exitCode());
        assertEquals("", result.stdout());
        assertTrue(result.stderr().contains("Expecting value"), result.stderr());
    }

    @Test
    void cliExitsOneWithEmptyStdoutOnMissingDescriptorFile() {
        Result result = run(new String[] {"graphify-runtime-probe",
                "--descriptor", temp.resolve("missing.json").toString()});

        assertEquals(1, result.exitCode());
        assertEquals("", result.stdout());
        assertFalse(result.stderr().isEmpty());
    }

    @Test
    void cliAcceptsEqualsFormForOptions() throws Exception {
        Path descriptor = validDescriptor();

        Result result = run(new String[] {"graphify-runtime-probe",
                "--descriptor=" + descriptor, "--output=" + temp.resolve("out.json")});

        assertEquals(0, result.exitCode());
        assertTrue(Files.exists(temp.resolve("out.json")));
    }

    @Test
    void cliExitsTwoWithUsageOnUnrecognizedArgument() {
        Result result = run(new String[] {"graphify-runtime-probe", "--bogus"});

        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("usage:"), result.stderr());
        assertTrue(result.stderr().contains("unrecognized arguments"), result.stderr());
    }

    @Test
    void cliExitsTwoWithUsageOnMissingOptionValue() {
        Result result = run(new String[] {"graphify-runtime-probe", "--command"});

        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("usage:"), result.stderr());
        assertTrue(result.stderr().contains("expected one argument"), result.stderr());
    }

    private Path validDescriptor() throws Exception {
        Path descriptor = temp.resolve("descriptor.json");
        Files.write(descriptor, ("{\n"
                + "  \"runtime_identity\": \"graphify-mcp\",\n"
                + "  \"runtime_version\": \"0.9.0\",\n"
                + "  \"transport\": \"mcp-stdio\",\n"
                + "  \"wire_version\": \"1.0\",\n"
                + "  \"supported_operations\": [\"query_graph\"]\n"
                + "}\n").getBytes(StandardCharsets.UTF_8));
        return descriptor;
    }

    private static Result run(String[] args) {
        return runWithPath(null, args);
    }

    private static Result runWithPath(List<Path> pathDirs, String[] args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int exitCode = GraphifyRuntimeProbeCli.run(args,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8),
                pathDirs);
        return new Result(exitCode,
                stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
    }

    private record Result(int exitCode, String stdout, String stderr) { }
}
