package com.example.cs3502filemanagementsystem;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.file.Path;

public class FileManagerApp extends Application{

    // GUI components
    private TreeView<Path> fileTreeView;        // File/Directory Display
    private TextArea fileContentArea;           // File Content Area (Read/Update)
    private Label statusLabel;                  // Status / Feedback Area
    private TextField currentPathField;         // Current Path Display

    // Track which file is currently loaded into the editor
    private Path currentOpenFile = null;

    private final FileService fileService = new FileService();

    @Override
    public void start(Stage primaryStage){
        primaryStage.setTitle("OwlTech File Manager");

        BorderPane root = new BorderPane();

        // Top: MenuBar (File button) and ToolBar (action buttons)

        MenuBar menuBar = createMenuBar(primaryStage);

        VBox topBox = new VBox(menuBar);
        root.setTop(topBox);

        Scene scene = new Scene(root, 1000, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    // createMenuBar

    private MenuBar createMenuBar (Stage stage){
        Menu fileMenu = new Menu("File");

        MenuItem chooseRoot = new MenuItem("Open Root Folder");

        MenuItem saveItem = new MenuItem("Save Current File");

        MenuItem exitApp = new MenuItem("Exit");

        fileMenu.getItems().addAll(chooseRoot, saveItem, new SeparatorMenuItem(), exitApp);

        return new MenuBar(fileMenu);
    }
}