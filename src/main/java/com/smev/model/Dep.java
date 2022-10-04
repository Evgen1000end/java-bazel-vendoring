package com.smev.model;

public record Dep(String group, String artifact, String version) {

    public String getGroup() {
        return group;
    }

    public String getArtifact() {
        return artifact;
    }

    public String getVersion() {
        return version;
    }
}
