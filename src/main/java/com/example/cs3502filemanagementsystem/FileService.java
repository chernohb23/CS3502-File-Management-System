package com.example.cs3502filemanagementsystem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileService {

    // Tree building

    public TreeItem<Path> createNode(Path path) {
        return new TreeItem<>(path){
            private boolean isFirstTimeChildren = true;
            private boolean isFirstTimeLeaf = true;
            private boolean leaf;

            @Override
            public ObservableList<TreeItem<Path>> getChildren() {
                if (isFirstTimeChildren) {
                    isFirstTimeChildren = false;
                    super.getChildren().setAll(buildChildren(this));
                }
                return super.getChildren();
            }

            @Override
            public boolean isLeaf() {
                if (isFirstTimeLeaf) {
                    isFirstTimeLeaf = false;
                    leaf = !Files.isDirectory(getValue());
                }
                return leaf;
            }

            private ObservableList<TreeItem<Path>> buildChildren(TreeItem<Path> treeItem) {
                Path f = treeItem.getValue();
                if (f == null || !Files.isDirectory(f)) {
                    return FXCollections.emptyObservableList();
                }

                try (Stream<Path> stream = Files.list(f)) {
                    List<Path> children = stream
                            .sorted((p1, p2) -> {
                                try {
                                    boolean d1 = Files.isDirectory(p1);
                                    boolean d2 = Files.isDirectory(p2);
                                    if (d1 && !d2) return -1;
                                    if (!d1 && d2) return 1;
                                    return p1.getFileName().toString()
                                            .compareToIgnoreCase(p2.getFileName().toString());
                                } catch (Exception e) {
                                    return 0;
                                }
                            })
                            .collect(Collectors.toList());

                    ObservableList<TreeItem<Path>> items = FXCollections.observableArrayList();
                    for (Path child : children) {
                        items.add(createNode(child));
                    }
                    return items;
                } catch (IOException e) {
                    return FXCollections.emptyObservableList();
                }
            }
        };
    }

    // Create

    public Path createFile(Path directory, String filename) throws IOException {
        Path newFile = directory.resolve(filename);
        if (Files.exists(newFile)){
            throw new FileAlreadyExistsException("File already exists: " + newFile);
        }
        return Files.createFile(newFile);
    }

    public Path createDirectory(Path directory, String folderName) throws IOException {
        Path newDir = directory.resolve(folderName);
        if (Files.exists(newDir)) {
            throw new FileAlreadyExistsException("Directory already exists: " + newDir);
        }
        return Files.createDirectory(newDir);
    }

    // Read

    public String readFileContent(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new NoSuchFileException("File not found: " + path);
        }
        if (Files.isDirectory(path)) {
            throw new IOException("Cannot read a directory as a file: " + path);
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    // Update

    public void writeFileContent(Path path, String content) throws IOException {
        if (!Files.exists(path)) {
            throw new NoSuchFileException("File not found: " + path);
        }
        if (Files.isDirectory(path)) {
            throw new IOException("Cannot write to a directory: " + path);
        }
        Files.writeString(path, content, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // Delete

    public void deleteRecursive(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new NoSuchFileException("Path not found: " + path);
        }
        // Delete children first, then the root
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    // Rename

    public Path rename(Path path, String newName) throws IOException {
        Path parent = path.getParent();
        if (parent == null) {
            throw new IOException("Cannot rename root path: " + path);
        }
        Path target = parent.resolve(newName);
        if (Files.exists(target)) {
            throw new FileAlreadyExistsException("Target name already exists: " + target);
        }
        return Files.move(path, target);
    }

    // Helpers

    public boolean isTextFile(Path path) {
        if (path == null || Files.isDirectory(path)) return false;
        String name = path.getFileName().toString().toLowerCase();

        String[] text = {
                ".txt", ".md", ".java", ".c", ".cpp", ".h", ".hpp", ".py",
                ".log", ".json", ".xml", ".html", ".htm", ".css", ".js",
                ".csv", ".properties"
        };
        for (String i : text) {
            if (name.endsWith(i)) return true;
        }
        return false;
    }

    public long safeFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }

}
