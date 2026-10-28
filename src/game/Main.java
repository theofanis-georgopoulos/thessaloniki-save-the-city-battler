package game;

import controller.BattleViewController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Φορτώνουμε το BattleScene.fxml από τον φάκελο view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/BattleScene.fxml"));
            Parent root = loader.load();

            // Παίρνουμε τον Controller για να αρχικοποιήσουμε τη μάχη
            BattleViewController controller = loader.getController();

            // Παράδειγμα μάχης: Sunday εναντίον Doctor Douros
            controller.initBattle("sunday", "douros");


            // 2. Κάνουμε το παράθυρο να ανοίγει αυτόματα σε Fullscreen (ή μεγιστοποιημένο)
            primaryStage.setTitle("City of the Greek Dream: The Prequel - Battle");
            primaryStage.setScene(new Scene(root));
            primaryStage.setMaximized(true);

            primaryStage.setResizable(true);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}