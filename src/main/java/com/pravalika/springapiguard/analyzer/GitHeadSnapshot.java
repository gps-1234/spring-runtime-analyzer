package com.pravalika.springapiguard.analyzer;

import java.io.File;

public final class GitHeadSnapshot {

    private final File rootDirectory;
    private final String commit;

    public GitHeadSnapshot(
            File rootDirectory,
            String commit
    ) {
        this.rootDirectory = rootDirectory;
        this.commit = commit;
    }

    public File getRootDirectory() {
        return rootDirectory;
    }

    public String getCommit() {
        return commit;
    }
}