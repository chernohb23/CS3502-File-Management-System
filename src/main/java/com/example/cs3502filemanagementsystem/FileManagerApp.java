package com.example.cs3502filemanagementsystem;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class FileManagerApp extends Application{

    // GUI components
    private TreeView<Path> fileTreeView;        // File/Directory Display
    private TextArea fileContentArea;           // File Content Area (Read/Update)
    private Label statusLabel;                  // Status / Feedback Area
    private TextField currentPathField;         // Current Path Display

    // Track which file is currently loaded into the editor
    private Path currentOpenFile = null;

    // Non-GUI logic
    private final FileService fileService = new FileService();

    @Override
    public void start(Stage primaryStage){
        primaryStage.setTitle("OwlTech File Manager");

        BorderPane root = new BorderPane();

        // Top: menu bar and toolbar (common actions)

        MenuBar menuBar = createMenuBar(primaryStage);
        ToolBar toolBar = createToolBar();

        VBox topBox = new VBox(menuBar, toolBar);
        root.setTop(topBox);

        // Top-middle: shows the currently selected path

        HBox pathBox = new HBox(8);
        pathBox.setPadding(new Insets(5, 10, 5, 10));
        Label pathLabel = new Label("Current Path: ");
        currentPathField = new TextField();
        currentPathField.setEditable(false);
        HBox.setHgrow(currentPathField, Priority.ALWAYS);
        pathBox.getChildren().addAll(pathLabel, currentPathField);

        // Center: split pane with the file tree (left) and file contents (right)

        fileTreeView = new TreeView<>();
        fileTreeView.setShowRoot(true);
        // Custom cell so we only display the file/folder name (not full path)
        fileTreeView.setCellFactory(tv -> new TreeCell<>(){
            @Override
            protected void updateItem(Path item, boolean empty){
                super.updateItem(item, empty);
                if (empty || item == null){
                    setText(null);
                } else {
                    Path name = item.getFileName();
                    setText(name == null ? item.toString() : name.toString());
                }
            }
        });

        // When the user selects a node, reflect it in the "Current Path" field

        fileTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null){
                Path path = selected.getValue();
                currentPathField.setText(path.toAbsolutePath().toString());
            }
        });

        // Double-click a regular file to open it in the editor

        fileTreeView.setOnMouseClicked(event -> {
           if (event.getClickCount() == 2){
               TreeItem<Path> selected = fileTreeView.getSelectionModel().getSelectedItem();
               if (selected != null){
                   Path path = selected.getValue();
                   if (Files.isRegularFile(path)){
                       openFile(path);
                   }
               }
           }
        });

        // Right side: basic text editor area for text files

        fileContentArea = new TextArea();
        fileContentArea.setWrapText(false);
        fileContentArea.setPromptText("File content will appear here...");
        VBox rightPane = new VBox(new Label("File Contents:"), fileContentArea);
        rightPane.setSpacing(5);
        rightPane.setPadding(new Insets(5));
        VBox.setVgrow(fileContentArea, Priority.ALWAYS);

        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.getItems().addAll(fileTreeView, rightPane);
        splitPane.setDividerPositions(0.3);

        VBox centerBox = new VBox(pathBox, splitPane);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        root.setCenter(centerBox);

        // Bottom: status bar for feedback messages

        statusLabel = new Label("Ready");
        statusLabel.setPadding(new Insets(3, 8, 3, 8));
        root.setBottom(statusLabel);

        // Initial load: start from the user's home directory

        Path initialRoot = Paths.get(System.getProperty("user.home"));
        loadRootDirectory(initialRoot);

        Scene scene = new Scene(root, 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Menu Bar

    private MenuBar createMenuBar (Stage stage){         // Builds the File menu (open root, save file, exit) with shortcuts.
        Menu fileMenu = new Menu("File");

        MenuItem chooseRoot = new MenuItem("Open Root Folder");
        chooseRoot.setOnAction(e -> chooseRootDirectory(stage));
        chooseRoot.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));

        MenuItem saveItem = new MenuItem("Save Current File");
        saveItem.setOnAction(e -> updateCurrentFile());
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> stage.close());
        exitItem.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN));

        fileMenu.getItems().addAll(chooseRoot, saveItem, new SeparatorMenuItem(), exitItem);

        return new MenuBar(fileMenu);
    }

    // Tool Bar

    private ToolBar createToolBar (){                   // Creates the toolbar with common actions (new, open, save, rename, delete, refresh).
        Button btnNewFile = new Button("New File");
        btnNewFile.setOnAction(e -> createNewFile());

        Button btnNewFolder = new Button("New Folder");
        btnNewFolder.setOnAction(e -> createNewFolder());

        Button btnOpenFile = new Button("Open");
        btnOpenFile.setOnAction(e -> {
            Path selected = getSelectedPath();
            if (selected != null && Files.isRegularFile(selected)) {
                openFile(selected);
            } else {
                showInfo("Select a file to open.");
            }
        });

        Button btnSave = new Button("Save");
        btnSave.setOnAction(e -> updateCurrentFile());

        Button btnRename = new Button("Rename");
        btnRename.setOnAction(e -> renameSelected());

        Button btnDelete = new Button("Delete");
        btnDelete.setOnAction(e -> deleteSelected());

        Button btnRefresh = new Button("Refresh");
        btnRefresh.setOnAction(e -> refreshTree());

        return new ToolBar(
                btnNewFile,
                btnNewFolder,
                new Separator(),
                btnOpenFile,
                btnSave,
                new Separator(),
                btnRename,
                btnDelete,
                new Separator(),
                btnRefresh);
    }

    // Navigation

    private void chooseRootDirectory(Stage stage) {              // Lets the user pick a root folder to browse in the tree.
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Root Folder");
        chooser.setInitialDirectory(Paths.get(System.getProperty("user.home")).toFile());

        File chosen = chooser.showDialog(stage);
        if (chosen != null) {
            loadRootDirectory(chosen.toPath());
        }
    }

    private void loadRootDirectory(Path rootPath) {          // Loads the given root path into the tree and resets the editor state.
        try {
            if (!Files.isDirectory(rootPath)) {
                showError("Selected path is not a directory: " + rootPath, null);
                return;
            }
            TreeItem<Path> rootItem = fileService.createNode(rootPath);
            rootItem.setExpanded(true);
            fileTreeView.setRoot(rootItem);
            fileTreeView.getSelectionModel().select(rootItem);
            currentPathField.setText(rootPath.toAbsolutePath().toString());
            currentOpenFile = null;
            fileContentArea.clear();
            setStatus("Loaded root directory: " + rootPath.toAbsolutePath());
        } catch (Exception ex) {
            showError("Failed to load root directory: " + rootPath, ex);
        }
    }

    private void refreshTree() {             // Rebuilds the tree for the current root selection (mostly used after changes).
        TreeItem<Path> root = fileTreeView.getRoot();
        if (root != null) {
            loadRootDirectory(root.getValue());
        }
    }

    // CRUD Operations

    private void createNewFile() {             // Creates a new empty file in the currently selected folder.
        Path dir = getCurrentDirectory();
        if (dir == null) {
            showInfo("No directory selected.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("newfile.txt");
        dialog.setTitle("Create New File");
        dialog.setHeaderText("Create a new file in:\n" + dir.toAbsolutePath());
        dialog.setContentText("File name:");

        dialog.showAndWait().ifPresent(name -> {
            if (name.trim().isEmpty()) {
                showInfo("File name cannot be empty.");
                return;
            }
            try {
                Path newFile = fileService.createFile(dir, name.trim());
                setStatus("Created file: " + newFile.getFileName());
                refreshTree();
                selectPathInTree(newFile);
                openFile(newFile);
            } catch (IOException ex) {
                showError("Failed to create file: " + name, ex);
            }
        });
    }

    private void createNewFolder() {           // Creates a new folder in the currently selected directory.
        Path dir = getCurrentDirectory();
        if (dir == null) {
            showInfo("No directory selected.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("New Folder");
        dialog.setTitle("Create New Folder");
        dialog.setHeaderText("Create a new folder in:\n" + dir.toAbsolutePath());
        dialog.setContentText("Folder name:");

        dialog.showAndWait().ifPresent(name -> {
            if (name.trim().isEmpty()) {
                showInfo("Folder name cannot be empty.");
                return;
            }
            try {
                Path newFolder = fileService.createDirectory(dir, name.trim());
                setStatus("Created folder: " + newFolder.getFileName());
                refreshTree();
                selectPathInTree(newFolder);
            } catch (IOException ex) {
                showError("Failed to create folder: " + name, ex);
            }
        });
    }

    private void openFile(Path file) {                // Opens a text file into the editor (if considered a text file by FileService).
        try {
            if (!Files.isRegularFile(file)) {
                showInfo("Selected path is not a regular file.");
                return;
            }
            if (!fileService.isTextFile(file)) {
                fileContentArea.setText("Preview not available for this file type.\n\n"
                        + "Path: " + file.toAbsolutePath()
                        + "\nSize: " + fileService.safeFileSize(file) + " bytes");
                currentOpenFile = null;
                setStatus("Selected non-text file: " + file.getFileName());
                return;
            }
            String content = fileService.readFileContent(file);
            fileContentArea.setText(content);
            currentOpenFile = file;
            setStatus("Opened file: " + file.toAbsolutePath());
        } catch (IOException ex) {
            showError("Failed to open file: " + file.getFileName(), ex);
        }
    }

    private void updateCurrentFile() {                 // Saves the content of the editor back to the currently open file.
        if (currentOpenFile == null) {
            showInfo("No file is currently open to save.");
            return;
        }
        try {
            String newContent = fileContentArea.getText();
            fileService.writeFileContent(currentOpenFile, newContent);
            setStatus("Saved changes to: " + currentOpenFile.getFileName());
        } catch (IOException ex) {
            showError("Failed to save file: " + currentOpenFile.getFileName(), ex);
        }
    }

    private void deleteSelected() {                    // Deletes the selected file or folder (recursively for folders).
        Path target = getSelectedPath();
        if (target == null) {
            showInfo("Select a file or folder to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Confirmation");
        confirm.setHeaderText("Are you sure you want to delete?");
        confirm.setContentText(target.toAbsolutePath().toString());

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    fileService.deleteFile(target);
                    setStatus("Deleted: " + target.getFileName());
                    if (target.equals(currentOpenFile)) {
                        currentOpenFile = null;
                        fileContentArea.clear();
                    }
                    refreshTree();
                } catch (IOException ex) {
                    showError("Failed to delete: " + target.getFileName(), ex);
                }
            }
        });
    }

    private void renameSelected() {                 // Renames the selected file or folder.
        Path target = getSelectedPath();
        if (target == null) {
            showInfo("Select a file or folder to renameFile.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(target.getFileName().toString());
        dialog.setTitle("Rename");
        dialog.setHeaderText("Rename:\n" + target.toAbsolutePath());
        dialog.setContentText("New name:");

        dialog.showAndWait().ifPresent(newName -> {
            String trimmed = newName.trim();
            if (trimmed.isEmpty()) {
                showInfo("New name cannot be empty.");
                return;
            }
            try {
                Path renamed = fileService.renameFile(target, trimmed);
                setStatus("Renamed to: " + renamed.getFileName());
                if (currentOpenFile != null && currentOpenFile.equals(target)) {
                    currentOpenFile = renamed;
                }
                refreshTree();
                selectPathInTree(renamed);
            } catch (IOException ex) {
                showError("Failed to renameFile: " + target.getFileName(), ex);
            }
        });
    }

    // Helpers

    private Path getSelectedPath() {                         // Returns the Path of the currently selected tree node (or null).
        TreeItem<Path> selectedItem = fileTreeView.getSelectionModel().getSelectedItem();
        return selectedItem == null ? null : selectedItem.getValue();
    }

    private Path getCurrentDirectory() {                 // Returns the directory that should be used as the target for create operations.
        Path selected = getSelectedPath();               // If a file is selected, its parent directory is returned.
        if (selected == null) {
            TreeItem<Path> root = fileTreeView.getRoot();
            return root == null ? Paths.get(System.getProperty("user.home")) : root.getValue();
        }
        if (Files.isDirectory(selected)) {
            return selected;
        } else {
            return selected.getParent();
        }
    }

    private void selectPathInTree(Path path) {              // Selects and expands the tree to the given path.
        TreeItem<Path> root = fileTreeView.getRoot();
        if (root == null || path == null) return;

        Path rootPath = root.getValue().toAbsolutePath().normalize();
        Path target = path.toAbsolutePath().normalize();

        if (!target.startsWith(rootPath)) {
            return; 
        }

        root.setExpanded(true);
        TreeItem<Path> current = root;

        Path rel = rootPath.relativize(target);
        Path running = rootPath;

        for (Path part : rel) {
            running = running.resolve(part);
            current.getChildren().size();

            TreeItem<Path> next = null;
            for (TreeItem<Path> child : current.getChildren()) {
                Path childPath = child.getValue().toAbsolutePath().normalize();
                if (childPath.equals(running)) {
                    next = child;
                    break;
                }
            }
            if (next == null) {
                return;
            }
            next.setExpanded(true);
            current = next;
        }

        fileTreeView.getSelectionModel().select(current);
    }

    // Status and errors

    private void setStatus(String msg) {           // Shows a normal (non-error) status message in the status bar.
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: -fx-text-base-color; -fx-padding: 3 8 3 8;");
    }

    private void showError(String msg, Exception ex) {   // Displays an error both in the status bar and via an alert dialog.
        System.err.println(msg);
        if (ex != null) {
            ex.printStackTrace();
        }
        statusLabel.setText("Error: " + msg);
        statusLabel.setStyle("-fx-text-fill: red; -fx-padding: 3 8 3 8;");

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Operation failed");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showInfo(String msg) {           // Lightweight information messages.
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: -fx-text-base-color; -fx-padding: 3 8 3 8;");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // Main

    public static void main(String[] args) {
        launch(args);
    }
}