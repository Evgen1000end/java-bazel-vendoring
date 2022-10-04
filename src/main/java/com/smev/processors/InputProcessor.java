package com.smev.processors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smev.model.Dep;
import com.smev.model.VendoringException;
import com.smev.model.input.InputData;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class InputProcessor {
    private static final Logger log = LoggerFactory.getLogger(InputProcessor.class);

    public InputData process(String[] args) throws IOException, ParseException {
        final CommandLine line = parseArguments(args);
        final String rootDirectory = extractRootDirectory(line);

        final List<Dep> deps = new ArrayList<>();
        extractDepsFromFile(line, rootDirectory, deps);
        extractDepsFromCmd(line, deps);

        if (deps.isEmpty()) {
            throw new VendoringException("Specify at least one dependency as an argument");
        }
        return new InputData(deps, rootDirectory);
    }

    private void extractDepsFromCmd(CommandLine line, List<Dep> deps) {
        String depStandalone = "";
        if (line.hasOption("dep")) {
            depStandalone = line.getOptionValue("dep");
        }
        if (!depStandalone.equals("")) {
            final String[] dependencies = depStandalone.split(",");
            for (String dependency : dependencies) {
                String[] parts = dependency.split(":");
                deps.add(new Dep(parts[0], parts[1], parts[2]));
            }
        }
    }

    private void extractDepsFromFile(CommandLine line, String rootDirectory, List<Dep> deps) throws IOException {
        String filePath = "";
        if (line.hasOption("file")) {
            filePath = line.getOptionValue("file");
        }

        if (!filePath.equals("")) {
            log.info("Грузим зависимости из файла {} в директорию: {}", filePath, rootDirectory);
            String json = FileUtils.readFileToString(new File(filePath), Charset.defaultCharset());

            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode valuesNode = mapper.readTree(json).get("deps");
            for (JsonNode node : valuesNode) {
                String[] parts = node.asText().split(":");
                deps.add(new Dep(parts[0], parts[1], parts[2]));
            }
        }
    }

    private String extractRootDirectory(CommandLine line) {
        String rootDirectory;
        if (line.hasOption("dir")) {
            rootDirectory = line.getOptionValue("dir");
        } else {
            throw new VendoringException("Specify a folder to download dependencies as a launch parameter");
        }
        return rootDirectory;
    }

    private CommandLine parseArguments(String[] args) throws ParseException {
        final CommandLineParser parser = new DefaultParser();
        final Options options = new Options();
        for (Command value : Command.values()) {
            options.addOption(value.s, value.l, value.hasArgs, value.description);
        }
        return parser.parse(options, args);
    }

    public enum Command {
        DIRECTORY("d", "dir", true, "Root directory for downloading artifacts."),
        FILE("f", "file", true, "A file with a list of dependencies."),
        DEPENDENCY("D", "dep", true, "List of dependencies.");

        private final String s;
        private final String l;
        private final boolean hasArgs;
        private final String description;

        Command(String s, String l, boolean hasArgs, String description) {
            this.s = s;
            this.l = l;
            this.hasArgs = hasArgs;
            this.description = description;
        }
    }
}
