package com.smev.processors;

import com.smev.Main;
import com.smev.model.VendoringException;
import com.smev.model.input.InputData;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class InputProcessorTest {
    private static final String PATH = "target\\bazel";

    @Test
    void variousVersionsTest() throws IOException, ParseException {
        perform(new String[]{"--dir", PATH, "--dep", "commons-cli:commons-cli:1.5.0,commons-cli:commons-cli:1.3.1,org.jboss.shrinkwrap.resolver:shrinkwrap-resolver-impl-maven-archive:3.1.4"});
        assertTrue(Files.exists(Path.of(PATH).resolve("commons-cli_commons-cli")));
    }

    private void perform(String[] args) throws IOException, ParseException {
        setup();
        invoke(args);
    }

    @Test
    void loadFromFileTest() throws IOException, ParseException {
        perform(new String[]{"--dir", PATH, "--file", "src\\test\\resources\\input.json"});
        assertTrue(Files.exists(Path.of(PATH).resolve("commons-cli_commons-cli")));
    }

    @Test
    void sameArtifactTest() throws IOException, ParseException {
        perform(new String[]{"--dir", PATH, "--dep", "commons-cli:commons-cli:1.5.0,commons-cli:commons-cli:1.5.0"});
        assertTrue(Files.exists(Path.of(PATH).resolve("commons-cli_commons-cli")));
    }

    @Test
    void incorrectParamsTest() throws IOException, ParseException {
        setup();
        String[] args = new String[]{"--dir", PATH};
        VendoringException vendoringException = assertThrows(VendoringException.class, () -> {
            invoke(args);
        });
        assertEquals("Specify at least one dependency as an argument", vendoringException.getMessage());
        assertTrue(isEmpty(Path.of(PATH)));
    }

    @Test
    void noFolderTest() throws IOException {
        setup();
        String[] args = new String[]{};
        VendoringException vendoringException = assertThrows(VendoringException.class, () -> {
            invoke(args);
        });
        assertEquals("Specify a folder to download dependencies as a launch parameter", vendoringException.getMessage());
    }

    @Test
    void mainMethodTest() throws IOException {
        setup();
        Main.main(new String[]{"--dir", PATH, "--dep", "commons-cli:commons-cli:1.5.0,commons-cli:commons-cli:1.5.0"});
        assertTrue(Files.exists(Path.of(PATH).resolve("commons-cli_commons-cli")));
    }

    @Test
    void mainMethodErrorTest() throws IOException {
        setup();
        Main.main(new String[]{});
        assertTrue(isEmpty(Path.of(PATH)));
    }

    private void invoke(String[] args) throws IOException, ParseException {
        final InputProcessor processor = new InputProcessor();
        final InputData data = processor.process(args);
        new VendoringProcessor().process(data.getDeps(), data.getRootDirectory());
    }

    private void setup() throws IOException {
        final Path workingDirectory = Path.of(PATH);
        FileUtils.deleteDirectory(workingDirectory.toFile());
        Files.createDirectory(workingDirectory);
    }

    public boolean isEmpty(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return entries.findFirst().isEmpty();
            }
        }
        return false;
    }

}