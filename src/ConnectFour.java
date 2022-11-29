import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.layout.Pane;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConnectFour extends Application {

    private static final int TILE = 100;
    private static final int COLUMNS = 7;
    private static final int ROWS = 6;

    private boolean Player = true;
    private boolean AllowToInsert = true;
    private static String PLAYER1 = "Player One";
    private static String PLAYER2 = "Player Two";

    private Disc[][] grid = new Disc[COLUMNS][ROWS];
    AudioClip backgroundMusic = new AudioClip(this.getClass().getResource("motivational-day-112790.mp3").toString());

    private Pane discRoot = new Pane();

    private Parent Content() {

        Pane root = new Pane();
        root.getChildren().add(discRoot);

        Shape gridShape = createGrid();
        root.getChildren().add(gridShape);
        root.getChildren().addAll(createColumns());

        backgroundMusic.setCycleCount(AudioClip.INDEFINITE);
        backgroundMusic.play();

        return root;
    }

    private Shape createGrid() {
        Shape shape = new Rectangle((COLUMNS + 1) * TILE, (ROWS + 1) * TILE);

        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLUMNS; x++) {
                Circle circle = new Circle(TILE / 2);
                circle.setCenterX(TILE / 2);
                circle.setCenterY(TILE / 2);
                circle.setTranslateX(x * (TILE + 5) + TILE / 4);
                circle.setTranslateY(y * (TILE + 5) + TILE / 4);

                shape = Shape.subtract(shape, circle);
            }
        }

        Light.Distant light = new Light.Distant();
        light.setAzimuth(50.0);
        light.setElevation(35.0);

        Lighting lighting = new Lighting();
        lighting.setLight(light);
        lighting.setSurfaceScale(5.0);

        shape.setFill(Color.BLUE);
        shape.setEffect(lighting);

        return shape;
    }

    private List<Rectangle> createColumns() {
        List<Rectangle> list = new ArrayList<>();

        for (int x = 0; x < COLUMNS; x++) {
            Rectangle rect = new Rectangle(TILE, (ROWS + 1) * TILE);
            rect.setTranslateX(x * (TILE + 5) + TILE / 4);
            rect.setFill(Color.TRANSPARENT);

            rect.setOnMouseEntered(e -> rect.setFill(Color.rgb(200, 200, 50, 0.3)));
            rect.setOnMouseExited(e -> rect.setFill(Color.TRANSPARENT));

            final int column = x;

            rect.setOnMouseClicked(e -> {
                if (AllowToInsert) {
                    AllowToInsert = false;
                    placingDisc(new Disc(Player), column);
                } 
            });
            list.add(rect);
        }

        return list;
    }

    private void placingDisc(Disc disc, int column) {
        int row = ROWS - 1;

        do {
            if (!getDisc(column, row).isPresent())
                break;
            row--;
        } while (row >= 0);

        if (row < 0)
            return;

        grid[column][row] = disc;
        discRoot.getChildren().add(disc);
        disc.setTranslateX(column * (TILE + 5) + TILE / 4);

        final int currentRow = row;

        TranslateTransition animation = new TranslateTransition(Duration.seconds(0.5), disc);
        animation.setToY(row * (TILE + 5) + TILE / 4);
        animation.setOnFinished(e -> {
            AllowToInsert = true;
            if (gameEnded(column, currentRow)) {
                gameOver();
            }
            Player = !Player;

        });
        animation.play();
    }

    private boolean gameEnded(int column, int row) {
        List<Point2D> vertical = IntStream.rangeClosed(row - 3, row + 3)
                .mapToObj(r -> new Point2D(column, r))
                .collect(Collectors.toList());

        List<Point2D> horizontal = IntStream.rangeClosed(column - 3, column + 3)
                .mapToObj(c -> new Point2D(c, row))
                .collect(Collectors.toList());

        Point2D topLeft = new Point2D(column - 3, row - 3);
        List<Point2D> diagonal1 = IntStream.rangeClosed(0, 6)
                .mapToObj(i -> topLeft.add(i, i))
                .collect(Collectors.toList());

        Point2D botLeft = new Point2D(column - 3, row + 3);
        List<Point2D> diagonal2 = IntStream.rangeClosed(0, 6)
                .mapToObj(i -> botLeft.add(i, -i))
                .collect(Collectors.toList());

        boolean isEnded = checkRange(vertical) || checkRange(horizontal)
                || checkRange(diagonal1) || checkRange(diagonal2);
        return isEnded;
    }

    private boolean checkRange(List<Point2D> points) {
        int chain = 0;

        for (Point2D p : points) {
            int column = (int) p.getX();
            int row = (int) p.getY();

            Disc disc = getDisc(column, row).orElse(new Disc(!Player));
            if (disc.red == Player) {
                chain++;
                if (chain == 4) {
                    return true;
                }
            } else {
                chain = 0;
            }
        }

        return false;
    }

    private void gameOver() {

        String winner = Player ? PLAYER1 : PLAYER2;
        if (AllowToInsert) {
            System.out.println("Winner is : " + winner);
        } else {
            System.out.println("It is a Tie");
        }

        Alert alert = new Alert(null);
        alert.setTitle("Connect four");
        alert.setHeaderText("Winner is : " + winner);
        alert.setContentText("Want to Play Again ? ");

        ButtonType yesBtn = new ButtonType("yes");
        ButtonType noBtn = new ButtonType("No , Exit");
        alert.getButtonTypes().setAll(yesBtn, noBtn);

        Platform.runLater(() -> {
            Optional<ButtonType> BtnClicked = alert.showAndWait();
            if (BtnClicked.isPresent() && BtnClicked.get() == yesBtn) {
                backgroundMusic.stop();
                resetGame();
            } else {
                Platform.exit();
                System.exit(0);
            }
        });
    }

    public void resetGame() {
        discRoot.getChildren().clear();
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                grid[row][col] = null;
            }
        }
        Player = true;
        backgroundMusic.setCycleCount(AudioClip.INDEFINITE);
        backgroundMusic.play();

    }

    private Optional<Disc> getDisc(int column, int row) {
        if (column < 0 || column >= COLUMNS || row < 0 || row >= ROWS)
            return Optional.empty();

        return Optional.ofNullable(grid[column][row]);
    }

    private static class Disc extends Circle {
        private final boolean red;

        public Disc(boolean red) {
            super(TILE / 2, red ? Color.RED : Color.YELLOW);
            this.red = red;

            setCenterX(TILE / 2);
            setCenterY(TILE / 2);
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Connect Four");
        stage.setScene(new Scene(Content()));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}