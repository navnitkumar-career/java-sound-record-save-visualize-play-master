package jaudio;

import javax.sound.sampled.*;

/**
 * Write data to the OutputChannel.
 */
public class Player implements Runnable {

    private MainController mainController;
    SourceDataLine line;
    Thread thread;

    public Player(MainController mainController) {
        this.mainController = mainController;
    }

    public void start() {
        mainController.errStr = null;
        thread = new Thread(this);
        thread.setName("Playback");
        thread.start();
    }

    public void stop() {
        thread = null;
    }

    private void shutDown(String message) {
        if ((mainController.errStr = message) != null) {
            System.err.println(mainController.errStr);
            if (mainController.isDrawingRequired)
                mainController.samplingGraph.repaint();
        }
        if (thread != null) {
            thread = null;
            if (mainController.isDrawingRequired)
                mainController.samplingGraph.stop();
            mainController.captureBtn.setEnabled(true);
            mainController.pauseBtn.setEnabled(false);
            mainController.playBtn.setText("Play");
        }
    }


    public void run() {

        // reload the file if loaded by file
        if (mainController.file != null) {
            mainController.createAudioInputStream(mainController.file, false);
        }

        // make sure we have something to play
        if (mainController.audioInputStream == null) {
            shutDown("No loaded audio to play back");
            return;
        }
        // reset to the beginnning of the stream
        try {
            mainController.audioInputStream.reset();
        } catch (Exception e) {
            shutDown("Unable to reset the stream\n" + e);
            return;
        }

        // get an AudioInputStream of the desired format for playback
        AudioFormat format = mainController.formatControls.getFormat();
        AudioInputStream playbackInputStream = AudioSystem.getAudioInputStream(format, mainController.audioInputStream);

        if (playbackInputStream == null) {
            shutDown("Unable to convert stream of format " + mainController.audioInputStream + " to format " + format);
            return;
        }

        // define the required attributes for our line,
        // and make sure a compatible line is supported.

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            shutDown("Line matching " + info + " not supported.");
            return;
        }

        // get and open the source data line for playback.

        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, mainController.BUFFER_SIZE);
        } catch (LineUnavailableException ex) {
            shutDown("Unable to open the line: " + ex);
            return;
        }

        // play back the captured audio data

        int frameSizeInBytes = format.getFrameSize();
        int bufferLengthInFrames = line.getBufferSize() / 8;
        int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
        byte[] data = new byte[bufferLengthInBytes];
        int numBytesRead = 0;

        // start the source data line
        line.start();

        while (thread != null) {
            try {
                if ((numBytesRead = playbackInputStream.read(data)) == -1) {
                    break;
                }
                int numBytesRemaining = numBytesRead;
                while (numBytesRemaining > 0) {
                    numBytesRemaining -= line.write(data, 0, numBytesRemaining);
                }
            } catch (Exception e) {
                shutDown("Error during playback: " + e);
                break;
            }
        }
        // we reached the end of the stream. let the data play out, then
        // stop and close the line.
        if (thread != null) {
            line.drain();
        }
        line.stop();
        line.close();
        line = null;
        shutDown(null);
    }
}
