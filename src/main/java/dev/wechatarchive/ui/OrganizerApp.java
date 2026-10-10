package dev.wechatarchive.ui;

import dev.wechatarchive.core.ExportPlan;
import dev.wechatarchive.core.FileNames;
import dev.wechatarchive.core.Screenshot;
import dev.wechatarchive.core.ScreenshotOrder;
import dev.wechatarchive.core.ZipExporter;
import dev.wechatarchive.ocr.LocalOcr;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public final class OrganizerApp extends Application {
    private static final String TITLE = "微信聊天截图整理";
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final Set<Path> imported = new HashSet<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "local-ocr-and-export");
        thread.setDaemon(true);
        return thread;
    });
    private final ListView<Row> imageList = new ListView<>(rows);
    private final ListView<String> previewList = new ListView<>();
    private final TextField prefixField = new TextField("记录");
    private final ComboBox<ExportPlan.Separator> prefixNameSeparator =
        new ComboBox<>(FXCollections.observableArrayList(ExportPlan.Separator.values()));
    private final ComboBox<ExportPlan.Separator> nameNumberSeparator =
        new ComboBox<>(FXCollections.observableArrayList(ExportPlan.Separator.values()));
    private final TextField zipField = new TextField("微信聊天截图");
    private final Label status = new Label("截图只在本机处理，不会上传。");
    private final Button importButton = new Button("导入图片");
    private final Button clearButton = new Button("全部清除");
    private final Button folderButton = new Button("选择保存位置");
    private final Button exportButton = new Button("导出 ZIP");
    private Stage stage;
    private Path outputDirectory = Path.of(System.getProperty("user.home"), "Desktop");
    private Label directoryLabel;
    private Path modelDirectory;
    private boolean exporting;

    private static final class Row {
        final Path path;
        final StringProperty name = new SimpleStringProperty("");
        final StringProperty state = new SimpleStringProperty("等待识别");

        Row(Path path) { this.path = path; }
    }

    @Override public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle(TITLE);
        stage.getIcons().add(new Image(getClass().getResource("/icon.png").toExternalForm()));
        stage.setMinWidth(850);
        stage.setMinHeight(620);
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app");

        Label eyebrow = new Label("LOCAL  /  PRIVATE  /  ZIP");
        eyebrow.getStyleClass().add("eyebrow");
        Label heading = new Label("整理聊天截图");
        heading.getStyleClass().add("heading");
        Label subtitle = new Label("导入截图，核对标题与顺序，再保存为一个 ZIP。原图不会改动。");
        subtitle.getStyleClass().add("subtitle");
        importButton.getStyleClass().add("primary");
        importButton.getStyleClass().add("header-action");
        importButton.setOnAction(e -> chooseImages());
        clearButton.getStyleClass().add("danger-outline");
        clearButton.getStyleClass().add("header-action");
        clearButton.setDisable(true);
        clearButton.setOnAction(e -> clearAll());
        HBox headerActions = new HBox(10, importButton, clearButton);
        headerActions.setAlignment(Pos.CENTER_RIGHT);
        HBox headingRow = new HBox(18, new VBox(4, eyebrow, heading, subtitle), headerActions);
        headingRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(headingRow.getChildren().getFirst(), Priority.ALWAYS);
        headingRow.getStyleClass().add("header");
        root.setTop(headingRow);

        imageList.setPlaceholder(new Label("拖入多张 PNG / JPG 截图，或点击“导入图片”"));
        imageList.setCellFactory(list -> new ScreenshotCell());
        imageList.getStyleClass().add("image-list");
        Label imagesTitle = new Label("截图与名称");
        imagesTitle.getStyleClass().add("section-title");
        Label imagesHint = new Label("点击缩略图查看原图；识别有误可直接改名，用 ↑ ↓ 调整顺序。");
        imagesHint.getStyleClass().add("hint");
        VBox left = new VBox(8, imagesTitle, imagesHint, imageList);
        VBox.setVgrow(imageList, Priority.ALWAYS);
        left.getStyleClass().add("panel");

        Label previewTitle = new Label("导出预览");
        previewTitle.getStyleClass().add("section-title");
        Label previewHint = new Label("文件名会随名称和顺序实时更新");
        previewHint.getStyleClass().add("hint");
        prefixField.setPromptText("例如：记录");
        prefixNameSeparator.setValue(ExportPlan.Separator.HYPHEN);
        nameNumberSeparator.setValue(ExportPlan.Separator.NONE);
        prefixNameSeparator.setAccessibleText("前缀与名称之间的连接符");
        nameNumberSeparator.setAccessibleText("名称与序号之间的连接符");
        prefixNameSeparator.setMaxWidth(Double.MAX_VALUE);
        nameNumberSeparator.setMaxWidth(Double.MAX_VALUE);
        prefixNameSeparator.valueProperty().addListener((a,b,c) -> refreshPreview());
        nameNumberSeparator.valueProperty().addListener((a,b,c) -> refreshPreview());
        zipField.setPromptText("例如：微信聊天截图");
        prefixField.textProperty().addListener((a,b,c) -> refreshPreview());
        zipField.textProperty().addListener((a,b,c) -> refreshPreview());
        previewList.setPlaceholder(new Label("导入图片后在这里核对最终文件名"));
        previewList.setFocusTraversable(false);
        directoryLabel = new Label(outputDirectory.toString());
        directoryLabel.setWrapText(true);
        folderButton.setOnAction(e -> chooseFolder());
        HBox folder = new HBox(10, folderButton, directoryLabel);
        folder.setAlignment(Pos.CENTER_LEFT);
        exportButton.getStyleClass().add("primary");
        exportButton.setMaxWidth(Double.MAX_VALUE);
        exportButton.setOnAction(e -> export());
        exportButton.setDisable(true);
        VBox prefixNameOption = new VBox(5, label("前缀与名称之间"), prefixNameSeparator);
        VBox nameNumberOption = new VBox(5, label("名称与序号之间"), nameNumberSeparator);
        HBox separatorOptions = new HBox(10, prefixNameOption, nameNumberOption);
        HBox.setHgrow(prefixNameOption, Priority.ALWAYS);
        HBox.setHgrow(nameNumberOption, Priority.ALWAYS);
        VBox right = new VBox(9,
            previewTitle, previewHint,
            label("文件名前缀"), prefixField,
            separatorOptions,
            label("ZIP 压缩包名称"), zipField,
            label("保存位置"), folder,
            label("ZIP 内文件名"), previewList,
            exportButton);
        VBox.setVgrow(previewList, Priority.ALWAYS);
        right.getStyleClass().add("panel");
        right.setPrefWidth(340);
        HBox content = new HBox(16, left, right);
        content.setPadding(new Insets(0, 24, 18, 24));
        HBox.setHgrow(left, Priority.ALWAYS);
        root.setCenter(content);
        status.getStyleClass().add("status");
        root.setBottom(status);

        Scene scene = new Scene(root, 1100, 750);
        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        scene.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY);
            e.consume();
        });
        scene.setOnDragDropped(e -> {
            boolean accepted = e.getDragboard().hasFiles();
            if (accepted) importImages(e.getDragboard().getFiles().stream().map(java.io.File::toPath).toList());
            e.setDropCompleted(accepted);
            e.consume();
        });
        stage.setScene(scene);
        stage.show();
        try {
            modelDirectory = extractModel();
        } catch (IOException error) {
            status.setText("OCR 模型不可用：" + error.getMessage() + "；仍可手动填写名称并导出。");
        }
    }

    private Label label(String value) {
        Label result = new Label(value);
        result.getStyleClass().add("field-label");
        return result;
    }

    private void chooseImages() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择微信聊天截图");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("图片 (PNG, JPG, JPEG)", "*.png", "*.jpg", "*.jpeg", "*.PNG", "*.JPG", "*.JPEG"));
        var selected = chooser.showOpenMultipleDialog(stage);
        if (selected != null) importImages(selected.stream().map(java.io.File::toPath).toList());
    }

    private void clearAll() {
        if (rows.isEmpty() || exporting) return;
        int count = rows.size();
        ButtonType cancel = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType clear = new ButtonType("清除全部", ButtonBar.ButtonData.OK_DONE);
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.initOwner(stage);
        confirmation.setTitle("清除当前批次");
        confirmation.setHeaderText("清除当前的 " + count + " 张图片？");
        confirmation.setContentText("列表中的名称修改会丢失；原图和已导出的 ZIP 不受影响。");
        confirmation.getButtonTypes().setAll(cancel, clear);
        if (confirmation.showAndWait().orElse(cancel) != clear) return;
        rows.clear();
        imported.clear();
        imageList.getSelectionModel().clearSelection();
        refreshPreview();
        status.setText("已清除 " + count + " 张图片，可以导入下一批。原图和已导出的 ZIP 未改动。");
    }

    private void importImages(List<Path> paths) {
        int added = 0, duplicate = 0, invalid = 0;
        for (Path path : paths) {
            try {
                Path canonical = path.toRealPath();
                FileNames.extension(canonical.getFileName().toString());
                if (imported.contains(canonical)) { duplicate++; continue; }
                if (!Files.isRegularFile(canonical) || !isReadableImage(canonical)) { invalid++; continue; }
                Row row = new Row(canonical);
                row.name.addListener((a,b,c) -> refreshPreview());
                imported.add(canonical);
                rows.add(row);
                added++;
                recognize(row);
            } catch (Exception error) { invalid++; }
        }
        refreshPreview();
        status.setText("已加入 " + added + " 张；跳过重复 " + duplicate + " 张、无效图片 " + invalid + " 张。"
            + (added > 0 ? "正在后台逐张识别，" : "") + "截图只在本机处理。");
    }

    private boolean isReadableImage(Path path) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
            if (input == null) return false;
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return false;
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                return reader.getWidth(0) > 0 && reader.getHeight(0) > 0;
            } finally {
                reader.dispose();
            }
        }
    }

    private void recognize(Row row) {
        if (modelDirectory == null) {
            row.state.set("OCR 不可用，请手动填写");
            return;
        }
        worker.submit(() -> {
            Platform.runLater(() -> {
                if (rows.contains(row)) row.state.set("正在识别顶部标题…");
            });
            try {
                LocalOcr.Result result = LocalOcr.readTitle(row.path, modelDirectory);
                Platform.runLater(() -> {
                    if (!rows.contains(row)) return;
                    if (result.name().isBlank()) row.state.set("未可靠识别（最高置信度 " + result.confidence() + "%），请手动填写");
                    else {
                        if (row.name.get().isBlank()) row.name.set(result.name());
                        row.state.set("标题识别，置信度 " + result.confidence() + "% · 请核对");
                    }
                });
            } catch (Exception | LinkageError error) {
                Platform.runLater(() -> {
                    if (rows.contains(row)) row.state.set("识别失败，请手动填写：" + error.getMessage());
                });
            }
        });
    }

    private void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择 ZIP 保存位置");
        if (Files.isDirectory(outputDirectory)) chooser.setInitialDirectory(outputDirectory.toFile());
        var selected = chooser.showDialog(stage);
        if (selected != null) {
            outputDirectory = selected.toPath();
            directoryLabel.setText(outputDirectory.toString());
        }
    }

    private List<Screenshot> snapshots() {
        return rows.stream().map(row -> new Screenshot(row.path, row.name.get())).toList();
    }

    private void refreshPreview() {
        clearButton.setDisable(rows.isEmpty() || exporting);
        if (rows.isEmpty()) {
            previewList.getItems().clear();
            exportButton.setDisable(true);
            return;
        }
        try {
            List<ExportPlan.Item> plan = ExportPlan.create(snapshots(), prefixField.getText(),
                prefixNameSeparator.getValue(), nameNumberSeparator.getValue());
            FileNames.zipName(zipField.getText());
            previewList.getItems().setAll(plan.stream().map(ExportPlan.Item::entryName).toList());
            exportButton.setDisable(exporting);
        } catch (IllegalArgumentException error) {
            previewList.getItems().setAll("请修正后导出：" + error.getMessage());
            exportButton.setDisable(true);
        }
    }

    private void export() {
        List<ExportPlan.Item> plan;
        Path destination;
        try {
            plan = ExportPlan.create(snapshots(), prefixField.getText(),
                prefixNameSeparator.getValue(), nameNumberSeparator.getValue());
            destination = outputDirectory.resolve(FileNames.zipName(zipField.getText()));
        } catch (IllegalArgumentException error) {
            status.setText(error.getMessage());
            return;
        }
        setExporting(true);
        status.setText("正在创建 ZIP…");
        worker.submit(() -> {
            try {
                ZipExporter.export(plan, destination);
                Platform.runLater(() -> status.setText("已导出 " + plan.size() + " 张图片：" + destination));
            } catch (Exception error) {
                Platform.runLater(() -> status.setText(error.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    setExporting(false);
                    refreshPreview();
                });
            }
        });
    }

    private Path extractModel() throws IOException {
        Path directory = Files.createTempDirectory("wechat-ocr-model-");
        directory.toFile().deleteOnExit();
        Path model = directory.resolve("chi_sim.traineddata");
        try (InputStream input = getClass().getResourceAsStream("/chi_sim.traineddata")) {
            if (input == null) throw new IOException("内置模型文件不存在");
            Files.copy(input, model);
        }
        model.toFile().deleteOnExit();
        return directory;
    }

    @Override public void stop() { worker.shutdownNow(); }

    private void setExporting(boolean value) {
        exporting = value;
        importButton.setDisable(value);
        clearButton.setDisable(value || rows.isEmpty());
        folderButton.setDisable(value);
        imageList.setDisable(value);
        prefixField.setDisable(value);
        prefixNameSeparator.setDisable(value);
        nameNumberSeparator.setDisable(value);
        zipField.setDisable(value);
        exportButton.setDisable(value);
    }

    private void showPreview(Row row) {
        if (!Files.isRegularFile(row.path) || !Files.isReadable(row.path)) {
            status.setText("无法预览，原图片已移动或不可读取：" + row.path.getFileName());
            return;
        }
        Stage preview = new Stage();
        preview.initOwner(stage);
        preview.initModality(Modality.WINDOW_MODAL);
        preview.setTitle("图片预览 · " + row.path.getFileName());
        preview.getIcons().add(new Image(getClass().getResource("/icon.png").toExternalForm()));
        preview.setMinWidth(640);
        preview.setMinHeight(400);

        Image image = new Image(row.path.toUri().toString(), true);
        ImageView fullImage = new ImageView(image);
        fullImage.setPreserveRatio(true);
        fullImage.setSmooth(true);
        ScrollPane scroller = new ScrollPane(fullImage);
        scroller.setPannable(true);
        scroller.setFitToWidth(false);
        scroller.getStyleClass().add("preview-scroll");
        DoubleProperty zoom = new SimpleDoubleProperty(1.0);
        fullImage.fitWidthProperty().bind(Bindings.createDoubleBinding(
            () -> Math.max(300, scroller.getViewportBounds().getWidth() - 20) * zoom.get(),
            scroller.viewportBoundsProperty(), zoom));

        Label filename = new Label(row.path.getFileName().toString());
        filename.getStyleClass().add("section-title");
        filename.setWrapText(true);
        Label hint = new Label("滚动查看原图；使用缩放按钮核对顶部昵称。");
        hint.getStyleClass().add("hint");
        VBox previewHeader = new VBox(5, filename, hint);
        previewHeader.getStyleClass().add("preview-header");

        Label feedback = new Label("正在加载原图…");
        feedback.getStyleClass().add("hint");
        image.progressProperty().addListener((observable, before, progress) -> {
            if (progress.doubleValue() >= 1 && !image.isError()) feedback.setText("原图预览 · 不会修改文件");
        });
        image.errorProperty().addListener((observable, before, error) -> {
            if (error) feedback.setText("原图加载失败，请检查文件是否仍可读取。");
        });
        if (image.isError()) feedback.setText("原图加载失败，请检查文件是否仍可读取。");
        else if (image.getProgress() >= 1) feedback.setText("原图预览 · 不会修改文件");
        Button zoomOut = new Button("－");
        zoomOut.setAccessibleText("缩小图片");
        zoomOut.setOnAction(e -> zoom.set(Math.max(0.5, zoom.get() - 0.25)));
        Button zoomIn = new Button("＋");
        zoomIn.setAccessibleText("放大图片");
        zoomIn.setOnAction(e -> zoom.set(Math.min(3.0, zoom.get() + 0.25)));
        Button fit = new Button("适应宽度");
        fit.setOnAction(e -> zoom.set(1.0));
        Label zoomValue = new Label("100%");
        zoom.addListener((observable, before, value) -> {
            zoomValue.setText(Math.round(value.doubleValue() * 100) + "%");
            zoomOut.setDisable(value.doubleValue() <= 0.5);
            zoomIn.setDisable(value.doubleValue() >= 3.0);
        });
        Button close = new Button("关闭预览");
        close.setOnAction(e -> preview.close());
        HBox controls = new HBox(9, feedback, zoomOut, zoomValue, zoomIn, fit, close);
        controls.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(feedback, Priority.ALWAYS);
        controls.getStyleClass().add("preview-controls");
        BorderPane previewRoot = new BorderPane();
        previewRoot.getStyleClass().add("preview-root");
        previewRoot.setTop(previewHeader);
        previewRoot.setCenter(scroller);
        previewRoot.setBottom(controls);
        Scene previewScene = new Scene(previewRoot, 760, 780);
        previewScene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        previewScene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) preview.close();
        });
        preview.setScene(previewScene);
        preview.setOnShown(e -> close.requestFocus());
        preview.showAndWait();
    }

    private final class ScreenshotCell extends ListCell<Row> {
        @Override protected void updateItem(Row row, boolean empty) {
            super.updateItem(row, empty);
            if (empty || row == null) { setGraphic(null); return; }
            ImageView thumbnail = new ImageView(new Image(row.path.toUri().toString(), 68, 82, true, true, true));
            thumbnail.setFitWidth(68);
            thumbnail.setFitHeight(82);
            thumbnail.setPreserveRatio(true);
            Button thumbnailButton = new Button();
            thumbnailButton.setGraphic(thumbnail);
            thumbnailButton.setAccessibleText("预览图片 " + row.path.getFileName());
            thumbnailButton.setTooltip(new Tooltip("点击查看原图"));
            thumbnailButton.getStyleClass().add("thumbnail-button");
            thumbnailButton.setOnAction(e -> showPreview(row));
            TextField name = new TextField();
            name.setPromptText("填写好友或群聊名称");
            name.textProperty().bindBidirectional(row.name);
            Label filename = new Label(row.path.getFileName().toString());
            filename.getStyleClass().add("filename");
            Label state = new Label();
            state.textProperty().bind(row.state);
            state.getStyleClass().add("hint");
            VBox middle = new VBox(5, filename, name, state);
            HBox.setHgrow(middle, Priority.ALWAYS);
            Button up = new Button("↑");
            up.setAccessibleText("上移 " + row.path.getFileName());
            up.setOnAction(e -> move(row, -1));
            Button down = new Button("↓");
            down.setAccessibleText("下移 " + row.path.getFileName());
            down.setOnAction(e -> move(row, 1));
            Button remove = new Button("移除");
            remove.setOnAction(e -> {
                rows.remove(row);
                imported.remove(row.path);
                refreshPreview();
            });
            VBox actions = new VBox(5, up, down, remove);
            actions.setAlignment(Pos.CENTER);
            HBox line = new HBox(12, thumbnailButton, middle, actions);
            line.setAlignment(Pos.CENTER_LEFT);
            line.setPadding(new Insets(7));
            setGraphic(line);
        }
    }

    private void move(Row row, int delta) {
        int from = rows.indexOf(row);
        int to = from + delta;
        if (to < 0 || to >= rows.size()) return;
        ScreenshotOrder.move(rows, from, to);
        imageList.getSelectionModel().select(to);
        refreshPreview();
    }
}
