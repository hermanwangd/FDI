package com.featuredeliveryintelligence.fdi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class JavaOnlySourcePolicyTests {
    @Test
    void frameworkSourceContainsNoPythonFiles() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main"))) {
            assertThat(files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".py") || name.endsWith(".pyi"))
                    .toList()).isEmpty();
        }
    }
}
