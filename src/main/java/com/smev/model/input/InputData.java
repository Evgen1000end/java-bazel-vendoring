package com.smev.model.input;

import com.smev.model.Dep;

import java.util.List;

public record InputData(List<Dep> deps, String rootDirectory) {

    public List<Dep> getDeps() {
        return deps;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }
}
