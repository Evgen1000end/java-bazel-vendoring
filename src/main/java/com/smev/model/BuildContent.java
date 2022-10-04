package com.smev.model;

import java.util.List;

public class BuildContent {
    private final List<JavaImport> imports;

    public BuildContent(List<JavaImport> imports) {
        this.imports = imports;
    }

    public String toBazelFormat(boolean newFile) {
        StringBuilder builder = new StringBuilder();
        if (newFile) {
            createTitle(builder);
        }
        for (JavaImport anImport : imports) {
            builder.append("java_import(\n");
            builder.append("    name = \"").append(anImport.name).append("\",\n");
            builder.append("    jars = [\"").append(anImport.jars.get(0)).append("\"],\n");
            if (!anImport.deps.isEmpty()) {
                builder.append("    deps = [\n");
                for (String dep : anImport.deps) {
                    builder.append("        \"").append(dep).append("\",\n");
                }
                builder.append("    ],\n");
            }
            builder.append(")\n");
        }
        return builder.toString();
    }


    public void createTitle(StringBuilder builder) {
        builder.append("package(default_visibility = [\"//visibility:public\"])").append("\n").append("\n");
    }

    public record JavaImport(String name, List<String> jars, List<String> deps) {
    }
}
