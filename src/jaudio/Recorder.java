package jaudio;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Reads data from the input channel and writes to the output stream
 */
public class Recorder implements Runnable {

    private MainController mainController;
    TargetDataLine line;
    Thread thread;

    public Recorder(MainController mainController) {
        this.mainController = mainController;
    }

    public void start() {
        mainController.errStr = null;
        thread = new Thread(this);
        thread.setName("Capture");
        thread.start();
    }

    public void stop() {
        thread = null;
    }

    private void shutDown(String message) {
        if ((mainController.errStr = message) != null && thread != null) {
            thread = null;
            if (mainController.isDrawingRequired)
                mainController.samplingGraph.stop();

            mainController.playBtn.setEnabled(true);
            mainController.pauseBtn.setEnabled(false);
            mainController.saveBtn.setEnabled(true);
            mainController.captureBtn.setText("Record");
            if (mainController.isDrawingRequired)
                mainController.samplingGraph.repaint();
        }
    }


    public void run() {

        mainController.duration = 0;
        mainController.audioInputStream = null;

        // define the required attributes for our line,
        // and make sure a compatible line is supported.

        AudioFormat format = mainController.formatControls.getFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            shutDown("Line matching " + info + " not supported.");
            return;
        }

        // get and open the target data line for capture.

        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format, line.getBufferSize());
        } catch (LineUnavailableException ex) {
            shutDown("Unable to open the line: " + ex);
            return;
        } catch (Exception ex) {
            shutDown(ex.toString());
            // JavaSound.showInfoDialog();
            return;
        }

        // play back the captured audio data
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int frameSizeInBytes = format.getFrameSize();
        int bufferLengthInFrames = line.getBufferSize() / 8;
        int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
        byte[] data = new byte[bufferLengthInBytes];
        int numBytesRead;

        line.start();

        while (thread != null) {
            if ((numBytesRead = line.read(data, 0, bufferLengthInBytes)) == -1) {
                break;
            }
            out.write(data, 0, numBytesRead);
        }

        // we reached the end of the stream. stop and close the line.
        line.stop();
        line.close();
        line = null;

        // stop and close the output stream
        try {
            out.flush();
            out.close();
        } catch (IOException ex) {
            mainController.reportStatus("Error on inputstream  " + ex.getMessage());
        }

        // load bytes into the audio input stream for playback

        mainController.audioBytes = out.toByteArray();
        System.out.println(out.size());
        ByteArrayInputStream bais = new ByteArrayInputStream(mainController.audioBytes);
        mainController.audioInputStream = new AudioInputStream(bais, format, mainController.audioBytes.length / frameSizeInBytes);

        long milliseconds = (long) ((mainController.audioInputStream.getFrameLength() * 1000) / format.getFrameRate());
        mainController.duration = milliseconds / 1000.0;

        try {
            mainController.audioInputStream.reset();
        } catch (Exception ex) {
            mainController.reportStatus("Error in resetting inputStream " + ex.getMessage());
        }
        try {
            if (mainController.isDrawingRequired) {
                mainController.samplingGraph.createWaveForm(mainController.audioBytes);
            }

        } catch (Exception e) {
            mainController.reportStatus("Error in drawing waveform " + e.getMessage());
        }

    }
}
