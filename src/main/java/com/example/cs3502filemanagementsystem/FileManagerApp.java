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

        // Top: menuBar and toolBar

        MenuBar menuBar = createMenuBar(primaryStage);
        ToolBar toolBar = createToolBar();

        VBox topBox = new VBox(menuBar, toolBar);
        root.setTop(topBox);

        // Top-middle: currentPathField

        HBox pathBox = new HBox(8);
        pathBox.setPadding(new Insets(5, 10, 5, 10));
        Label pathLabel = new Label("Current Path: ");
        currentPathField = new TextField();
        currentPathField.setEditable(false);
        HBox.setHgrow(currentPathField, Priority.ALWAYS);
        pathBox.getChildren().addAll(pathLabel, currentPathField);

        // Center: SplitPlane (fileTreeView and fileContentArea)

        fileTreeView = new TreeView<>();
        fileTreeView.setShowRoot(true);
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

        // When node is selected, update currentPathField

        fileTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null){
                Path path = selected.getValue();
                currentPathField.setText(path.toAbsolutePath().toString());
            }
        });

        // When file is double-clicked, read it

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

        // fileContentArea

        fileContentArea = new TextArea();
        fileContentArea.setWrapText(false);
        fileContentArea.setPromptText("File content will appear here...");
        VBox rightPane = new VBox(new Label("File Contents:"), fileContentArea);
        rightPane.setSpacing(5);
        rightPane.setPadding(new Insets(5));
        VBox.setVgrow(fileContentArea, Priority.ALWAYS);

        SplitPane splitPlane = new SplitPane();
        splitPlane.setOrientation(Orientation.HORIZONTAL);
        splitPlane.getItems().addAll(fileTreeView, rightPane);

        VBox centerBox = new VBox(pathBox, splitPlane);
        VBox.setVgrow(splitPlane, Priority.ALWAYS);
        root.setCenter(centerBox);

        // Bottom: statusLabel

        statusLabel = new Label("Ready");
        statusLabel.setPadding(new Insets(3, 8, 3, 8));
        root.setBottom(statusLabel);

        // Initial load: user home

        Path initialRoot = Paths.get(System.getProperty("user.home"));
        loadRootDirectory(initialRoot);

        Scene scene = new Scene(root, 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Menu Bar

    private MenuBar createMenuBar (Stage stage){
        Menu fileMenu = new Menu("File");

        MenuItem chooseRoot = new MenuItem("Open Root Folder");

        MenuItem saveItem = new MenuItem("Save Current File");

        fileMenu.getItems().addAll(chooseRoot, saveItem);

        return new MenuBar(fileMenu);
    }

    // Tool Bar

    private ToolBar createToolBar (){
        Button btnNewFile = new Button("New File");

        Button btnNewFolder = new Button("New Folder");

        Button btnOpenFile = new Button("Open");

        Button btnSave = new Button("Save");

        Button btnRename = new Button("Rename");

        Button btnDelete = new Button("Delete");

        Button btnRefresh = new Button("Refresh");

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

    private void chooseRootDirectory(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Root Folder");
        chooser.setInitialDirectory(Paths.get(System.getProperty("user.home")).toFile());

        File chosen = chooser.showDialog(stage);
        if (chosen != null) {
            loadRootDirectory(chosen.toPath());
        }
    }

    private void loadRootDirectory(Path rootPath) {
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

    private void refreshTree() {
        TreeItem<Path> root = fileTreeView.getRoot();
        if (root != null) {
            loadRootDirectory(root.getValue());
        }
    }

    // CRUD Operations

    // private Path getSelectedPath(){}

    // private Path getCurrentDirectory(){}

    private void createNewFile(){}

    private void createNewFolder(){}

    private void openFile(Path file){}

    private void updateCurrentFile(){}

    private void deleteSelectedFile(){}

    private void renameSelectedFile(){}

    // Tree Helper

    private void selectPathInTree(Path path){}

    // private TreeItem<Path> findTreeItem(TreeItem<Path> current, Path target){}

    // Status and errors

    private void setStatus(String msg){}

    private void showError(String msh, Exception e){}

    private void showInfo(String msg){}

    // Main

    public static void main(String[] args) {
        launch(args);
    }
}