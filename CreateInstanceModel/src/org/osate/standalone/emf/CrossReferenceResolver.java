package org.osate.standalone.emf;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public class CrossReferenceResolver {
    static int UP_LEVELS = 1;

    static Map<String, Object> resolve(String path, String extension) {
        List<String> foundFiles = new ArrayList<>(Arrays.asList(path));
        File file = new File(path);
        String parentDirectory = null;
        String parentName = null;
        int levelsUp = 0;
        Map<String, Object> dataOutput = new HashMap<>();
        if (extension == null) {
            extension = getExtension(path);
        }

        while (levelsUp < UP_LEVELS && file.getParent() != null) {
            file = new File(file.getParent());
            levelsUp++;
        }
        parentDirectory = file.getPath();
        parentName = file.getName();
        Set<String> visitedFiles = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(parentDirectory);
        visitedFiles.add(path);
        while (queue.size() > 0) {
            String pathFileOrArchive = queue.poll();
            file = new File(pathFileOrArchive);
            if (!visitedFiles.contains(pathFileOrArchive)) {
                if (file.isDirectory()) {
                    try {
                        for (File childFile : Objects.requireNonNull(file.listFiles())) {
                            queue.add(childFile.getPath());
                        }
                    } catch (Exception e) {
                        System.err
                                .println("ERROR CrossReferenceResolver:: reading the files of the directory: " + file);
                    }
                } else {
                    String filePath = file.getPath();
                    String ext = getExtension(filePath);
                    if (ext.equals(extension)) {
                        foundFiles.add(filePath);
                    }
                }

            }
        }
        /////////// OUTPUT //////////////////
        dataOutput.put("parentName", parentName);
        dataOutput.put("foundFiles", foundFiles);
        return dataOutput;
    }

    static String getExtension(String path) {
        if (!path.contains(".")) {
            return "txt";
        }
        String[] chunksFileString = path.split("\\.");
        return chunksFileString[chunksFileString.length - 1];
    }
}
