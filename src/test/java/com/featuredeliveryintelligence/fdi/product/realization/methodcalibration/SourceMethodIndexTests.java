package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SourceMethodIndexTests {
    @TempDir Path root;

    @Test void canonicalizesAndFindsOnlyUniqueSourceCalls() throws Exception {
        Path file = root.resolve("Service.java");
        Files.writeString(file, """
                package demo;
                import java.util.Map;
                class Service {
                  Repo repo;
                  void handle(String name, Map<String, String> model) { repo.save(name); helper(); System.out.println(name); }
                  void helper() {}
                }
                class Repo { void save(String name) {} }
                """);
        var index = new SourceMethodIndex(root, List.of(file));
        var seed = index.unique("demo.Service#handle");
        assertNotNull(seed);
        assertEquals("demo.Service#handle(java.lang.String,java.util.Map)", seed.signature());
        assertEquals(List.of("demo.Repo#save(java.lang.String)", "demo.Service#helper()"),
                index.calls(seed).stream().map(SourceMethodIndex.Method::signature).sorted().toList());
    }

    @Test void abstainsOnOverloadsAndShadowedUnknownReceivers() throws Exception {
        Path file = root.resolve("Service.java");
        Files.writeString(file, """
                package demo;
                class Service {
                  Repo repo;
                  void handle(Object repo) { repo.save("x"); }
                  void overloaded() { this.save("x"); }
                  void save(String value) {}
                  void save(Object value) {}
                }
                class Repo { void save(String name) {} }
                """);
        var index = new SourceMethodIndex(root, List.of(file));
        assertNull(index.unique("demo.Service#save"));
        assertTrue(index.calls(index.unique("demo.Service#handle")).isEmpty());
        assertTrue(index.calls(index.unique("demo.Service#overloaded")).isEmpty());
    }

    @Test void excludesNestedDeferredAndAnonymousCalls() throws Exception {
        Path file = root.resolve("Service.java");
        Files.writeString(file, """
                package demo;
                class Service {
                  void handle() { Runnable x = () -> helper(); class Local { void run() { helper(); } } }
                  void helper() {}
                }
                """);
        var index = new SourceMethodIndex(root, List.of(file));
        assertTrue(index.calls(index.unique("demo.Service#handle")).isEmpty());
        assertNull(index.unique("demo.Local#run"));
    }

    @Test void unknownParameterDoesNotHideAnOverload() throws Exception {
        Path file = root.resolve("Service.java");
        Files.writeString(file, "package demo; class Service { void run() { save(1); } void save(int n) {} void save(Missing n) {} }");
        var index = new SourceMethodIndex(root, List.of(file));
        assertNull(index.unique("demo.Service#save"));
        assertTrue(index.calls(index.unique("demo.Service#run")).isEmpty());
    }
}
