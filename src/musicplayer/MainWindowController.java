package musicplayer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;

public class MainWindowController implements Initializable {
    @FXML
    private Slider slider;
    @FXML
    private Button playButton;
    @FXML
    private AreaChart<String, Number> visualiser;

    private static final double INTERVAL = 0.01;
    private static final int BANDS = 48;
    private static final double DROPDOWN = 0.5;
    private XYChart.Data[] seriesData;
    private File selectedFile;
    private MediaPlayer mediaPlayer;
    private Media media;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        seriesData = new XYChart.Data[BANDS + 2];
        for (int i = 0; i < seriesData.length; i++) {
            seriesData[i] = new XYChart.Data<>(Integer.toString(i + 1), 0);
            series.getData().add(seriesData[i]);
        }
        visualiser.getData().add(series);
    }

    @FXML
    private void handlePlay(ActionEvent event) {
        if (mediaPlayer != null) {
            slider.setMax(mediaPlayer.getTotalDuration().toSeconds());

            if (mediaPlayer.getStatus().equals(MediaPlayer.Status.DISPOSED)) {
                loadMedia();
            }

            if (!mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING)) {
                mediaPlayer.play();
                playButton.setText("Pause");
            } else {
                mediaPlayer.pause();
                playButton.setText("Play");
            }
        }
    }

    @FXML
    private void handleOpenMenuItemActtion(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("MP3 files", "*.mp3");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setTitle("Open MP3 File");
        selectedFile = fileChooser.showOpenDialog(MusicPlayer.getStage());
        try {
            media = new Media(selectedFile.toURI().toURL().toString());
            loadMedia();
        } catch (MalformedURLException ex) {
            Logger.getLogger(MainWindowController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void loadMedia() {
        if (selectedFile != null) {
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.currentTimeProperty().addListener((Observable observable) -> {
                slider.setValue(mediaPlayer.getCurrentTime().toSeconds());
            });
            mediaPlayer.setAudioSpectrumListener(new MusicSpectrum());
            mediaPlayer.setAudioSpectrumNumBands(BANDS);
            mediaPlayer.setAudioSpectrumInterval(INTERVAL);
            mediaPlayer.setOnEndOfMedia(() -> {
                slider.setValue(slider.getMax());
                loadMedia();
                playButton.setText("Play");
            });
        }
    }

    private float[] fillBuffer(int size, float fillValue) {
        float[] floats = new float[size];
        Arrays.fill(floats, fillValue);
        return floats;
    }

    private class MusicSpectrum implements AudioSpectrumListener {

        float[] buffer = fillBuffer(BANDS, mediaPlayer.getAudioSpectrumThreshold());

        @Override
        public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases) {
            Platform.runLater(() -> {
                seriesData[0].setYValue(0);
                seriesData[BANDS + 1].setYValue(0);
                for (int i = 0; i < magnitudes.length; i++) {
                    if (magnitudes[i] >= buffer[i]) {
                        buffer[i] = magnitudes[i];
                        seriesData[i + 1].setYValue(magnitudes[i] - mediaPlayer.getAudioSpectrumThreshold());
                    } else {
                        seriesData[i + 1].setYValue(buffer[i] - mediaPlayer.getAudioSpectrumThreshold());
                        buffer[i] -= DROPDOWN;
                    }
                }
            });
        }
    }
}
