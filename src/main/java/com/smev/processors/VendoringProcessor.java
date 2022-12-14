package com.smev.processors;

import com.smev.model.BuildContent;
import com.smev.model.Dep;
import org.apache.commons.io.FileUtils;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenArtifactInfo;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VendoringProcessor {
    private static final Logger log = LoggerFactory.getLogger(VendoringProcessor.class);
    private static final String[] requiredTypes = new String[]{"jar"};
    private static final String PATTERN_DEP_LINK = "//third_party/jvm/{0}/{1}:{2}";
    private static final String BAZEL_BUILD_FILENAME = "BUILD";

    private static MavenResolvedArtifact[] prepareAsMavenArtifact(String requiredType, Dep dep) {
        return Maven.configureResolver()
                .withMavenCentralRepo(true)
                .resolve(MessageFormat.format("{0}:{1}:{2}:{3}", dep.getGroup(), dep.getArtifact(), requiredType, dep.getVersion()))
                .withTransitivity()
                .asResolvedArtifact();
    }

    public void process(List<Dep> deps, String rootDirectory) throws IOException {
        for (String requiredType : requiredTypes) {
            for (Dep dep : deps) {
                final MavenResolvedArtifact[] mavenArtifacts = prepareAsMavenArtifact(requiredType, dep);
                for (MavenResolvedArtifact mavenArtifact : mavenArtifacts) {
                    prepareDependency(rootDirectory, mavenArtifact);
                }
            }
        }
    }

    private void prepareDependency(String rootDirectory, MavenResolvedArtifact mavenArtifact) throws IOException {
        // 0. ?????????????? ???????????????????? ?? ?????????????? ???????????????????????? ?????? ?????????? ???????????????? ?????? ?????? ????, ?????? ?? ???????????? ????????????
        final Path directoryPath = Paths.get(rootDirectory).resolve(mavenArtifact.getCoordinate().getGroupId());
        final Path subDirectoryPath = directoryPath.resolve(mavenArtifact.getCoordinate().getArtifactId());
        final Path directory = createDirectory(directoryPath, subDirectoryPath);
        // ???????????????? ???????? ?? ?????????? (?? ?????????????????????? .m2)
        final Path source = mavenArtifact.asFile().toPath();
        // ???????? ?????? ??????????????????????
        final Path destination = directory.resolve(mavenArtifact.asFile().getName());
        // ???????????????????? ??????????????????, ???????? ???? ?????? ???????? ???? ??????????
        final boolean alreadyExists = Files.exists(destination);
        final Path fileName = destination.getFileName();
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        if (!alreadyExists) {
            final File buildFile = directory.resolve(BAZEL_BUILD_FILENAME).toFile();

            final List<BuildContent.JavaImport> imports = new ArrayList<>();
            final String importName = MessageFormat.format("{0}", mavenArtifact.getCoordinate().getVersion());
            final String importJar = mavenArtifact.asFile().getName();
            final List<String> importDeps = new ArrayList<>();

            for (MavenArtifactInfo dependency : mavenArtifact.getDependencies()) {
                MavenCoordinate coordinate = dependency.getCoordinate();
                final String depLink = MessageFormat.format(PATTERN_DEP_LINK, coordinate.getGroupId(), coordinate.getArtifactId(), coordinate.getVersion());
                importDeps.add(depLink);
            }

            imports.add(new BuildContent.JavaImport(importName, Collections.singletonList(importJar), importDeps));
            createBuildFile(buildFile, imports);
        } else {
            log.info("Artefact {} already exist", fileName);
        }
    }

    private void createBuildFile(File buildFile, List<BuildContent.JavaImport> imports) throws IOException {
        // ??????????????????, ???????? ???? ?????? BUILD ???????? (???? ???????????? ????????????)
        if (buildFile.exists()) {
            String data = MessageFormat.format("{0}\n\n", FileUtils.readFileToString(buildFile, Charset.defaultCharset()));
            BuildContent buildContent = new BuildContent(imports);
            FileUtils.writeStringToFile(buildFile, data + buildContent.toBazelFormat(false), Charset.defaultCharset());
        } else {
            // ?????????????? ??????????
            BuildContent buildContent = new BuildContent(imports);
            FileUtils.writeStringToFile(buildFile, buildContent.toBazelFormat(true), Charset.defaultCharset());
        }
    }

    private Path createDirectory(Path directoryPath, Path subDirectoryPath) throws IOException {
        if (!Files.exists(directoryPath)) {
            Files.createDirectory(directoryPath);
        }
        return !Files.exists(subDirectoryPath) ? Files.createDirectory(subDirectoryPath) : subDirectoryPath;
    }
}
