package jaudio;


import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.io.File;
import java.util.Vector;

/**
 * Capture/Playback sample. Record audio in different formats and then playback the recorded audio. The captured audio can be saved either as a WAVE, AU or
 * AIFF. Or load an audio file for streaming playback.
 *
 * @author Brian Lichtenwalter-- visualization and capture
 * @author Ganesh Tiwari (gtiwari333) --> made a reusable class
 * @version 2.1
 */

public class MainController extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;
    byte[] audioBytes = null;
    float[] audioData = null;
    final int BUFFER_SIZE = 16384;
    final FormatControlConf formatControls = new FormatControlConf();            // @jve:decl-index=0:
    final Recorder recorder = new Recorder(this);                    // @jve:decl-index=0:
    final Player player = new Player(this);                    // @jve:decl-index=0:
    final WaveData waveData;
    AudioInputStream audioInputStream;                                        // @jve:decl-index=0:
    SamplingGraph samplingGraph;
    final JButton playBtn;
    final JButton captureBtn;
    final JButton pauseBtn;
    final JButton saveBtn;
    String errStr;
    double duration, seconds;
    File file;                                                    // @jve:decl-index=0:
    final Vector<Line2D.Double> lines = new Vector<>();    // @jve:decl-index=0:
    final boolean isDrawingRequired;
    final boolean isSaveRequired;
    final JPanel innerPanel;
    String saveFileName = null;                                // @jve:decl-index=0:

    /**
     * Instantiates a new j sound capture.
     *
     * @param isDrawingRequired the is drawing required
     * @param isSaveRequired    the is save required
     */
    public MainController(boolean isDrawingRequired, boolean isSaveRequired) {
        waveData = new WaveData();
        this.isDrawingRequired = isDrawingRequired;
        this.isSaveRequired = isSaveRequired;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(1, 1, 1, 1));

        innerPanel = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.X_AXIS));

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setPreferredSize(new Dimension(200, 50));
        buttonsPanel.setBorder(new EmptyBorder(5, 0, 1, 0));
        playBtn = addButton("Play", buttonsPanel, false);
        captureBtn = addButton("Record", buttonsPanel, true);
        pauseBtn = addButton("Pause", buttonsPanel, false);
        saveBtn = addButton("Save ", buttonsPanel, false);
        innerPanel.add(buttonsPanel);

        // samplingPanel
        if (isDrawingRequired) {
            JPanel samplingPanel = new JPanel(new BorderLayout());
            EmptyBorder eb = new EmptyBorder(2, 2, 2, 2);
            SoftBevelBorder sbb = new SoftBevelBorder(SoftBevelBorder.LOWERED);
            samplingPanel.setBorder(new CompoundBorder(eb, sbb));
            samplingPanel.add(samplingGraph = new SamplingGraph(this));
            innerPanel.add(samplingPanel);
        }
        // whole panel
        JPanel completePanel = new JPanel();
        completePanel.setLayout(new BoxLayout(completePanel, BoxLayout.X_AXIS));
        completePanel.add(innerPanel);
        add(completePanel);
    }

    public boolean isSoundDataAvailable() {
        if (audioBytes != null)
            return (audioBytes.length > 100);
        else
            return false;
    }

    public byte[] getAudioBytes() {
        return audioBytes;
    }

    public String getSaveFileName() {
        return saveFileName;
    }

    public void setSaveFileName(String saveFileName) {
        this.saveFileName = saveFileName;
        System.out.println("FileName Changed !!! " + saveFileName);
    }

    public float[] getAudioData() throws Exception {
        if (audioData == null) {
            audioData = waveData.extractFloatDataFromAudioInputStream(audioInputStream);
        }
        return audioData;
    }

    public void setAudioData(float[] audioData) {
        this.audioData = audioData;
    }

    private JButton addButton(String name, JPanel p, boolean state) {
        JButton b = new JButton(name);
        b.setPreferredSize(new Dimension(85, 24));
        b.addActionListener(this);
        b.setEnabled(state);
        b.setFocusable(false);
        p.add(b);
        return b;
    }


    public void actionPerformed(ActionEvent e) {
        System.out.println("actionPerformed *********");
        Object obj = e.getSource();
        if (isSaveRequired && obj.equals(saveBtn)) {

            try {
                getFileNameAndSaveFile();

            } catch (Exception e2) {
                reportStatus("Error in saving file " + e2.getMessage());
            }

        } else if (obj.equals(playBtn)) {
            if (playBtn.getText().startsWith("Play")) {
                playCaptured();
            } else {
                stopPlaying();
            }
        } else if (obj.equals(captureBtn)) {
            if (captureBtn.getText().startsWith("Record")) {
                startRecord();
            } else {
                stopRecording();
            }
        } else if (obj.equals(pauseBtn)) {
            if (pauseBtn.getText().startsWith("Pause")) {
                pausePlaying();
            } else {
                resumePlaying();
            }
        }
    }

    public void playCaptured() {
        player.start();
        if (isDrawingRequired)
            samplingGraph.start();
        captureBtn.setEnabled(false);
        pauseBtn.setEnabled(true);
        playBtn.setText("Stop");
    }

    public void stopPlaying() {
        player.stop();
        if (isDrawingRequired)
            samplingGraph.stop();
        captureBtn.setEnabled(true);
        pauseBtn.setEnabled(false);
        playBtn.setText("Play");
    }

    public void startRecord() {
        file = null;
        recorder.start();
        if (isDrawingRequired)
            samplingGraph.start();
        playBtn.setEnabled(false);
        pauseBtn.setEnabled(true);
        saveBtn.setEnabled(false);
        captureBtn.setText("Stop");
    }

    public void stopRecording() {
        lines.removeAllElements();
        recorder.stop();
        if (isDrawingRequired)
            samplingGraph.stop();
        playBtn.setEnabled(true);
        pauseBtn.setEnabled(false);
        saveBtn.setEnabled(true);
        captureBtn.setText("Record");
    }

    public void pausePlaying() {

        if (recorder.thread != null) {
            recorder.line.stop();
        } else {
            if (player.thread != null) {
                player.line.stop();
            }
        }
        pauseBtn.setText("Resume");

    }

    public void resumePlaying() {
        if (recorder.thread != null) {
            recorder.line.start();
        } else {
            if (player.thread != null) {
                player.line.start();
            }
        }
        pauseBtn.setText("Pause");
    }

    public void getFileNameAndSaveFile() throws Exception {
        while (saveFileName == null) {
            saveFileName = JOptionPane.showInputDialog(null, "Enter WAV File Name", "audiofilename");
        }
        waveData.saveToFile(saveFileName, AudioFileFormat.Type.WAVE, audioInputStream);

    }


    /**
     * Creates the audio input stream.
     *
     * @param file             the file
     * @param updateComponents the update components
     */
    public void createAudioInputStream(File file, boolean updateComponents) {
        if (file != null && file.isFile()) {
            try {
                this.file = file;
                errStr = null;
                audioInputStream = AudioSystem.getAudioInputStream(file);
                playBtn.setEnabled(true);
                // fileName = file.getName();
                long milliseconds = (long) ((audioInputStream.getFrameLength() * 1000) / audioInputStream.getFormat().getFrameRate());
                duration = milliseconds / 1000.0;

                saveBtn.setEnabled(true);
                if (updateComponents) {
                    formatControls.setFormat(audioInputStream.getFormat());
                    if (isDrawingRequired)
                        samplingGraph.createWaveForm(null);
                }
            } catch (Exception ex) {
                reportStatus(ex.toString());
            }
        } else {
            reportStatus("Audio file required.");
        }
    }

    /**
     * Report status.
     *
     * @param msg the msg
     */
    public void reportStatus(String msg) {
        if ((errStr = msg) != null) {
            System.out.println(errStr);
            if (isDrawingRequired)
                samplingGraph.repaint();
        }
    }

    public static void main(String[] s) {
        MainController capturePlayback = new MainController(true, true);
        JFrame f = new JFrame("Capture/Playback/Save/Read for Speaker Data");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.getContentPane().add("Center", capturePlayback);
        f.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int w = 850;
        int h = 500;
        f.setLocation(screenSize.width / 2 - w / 2, screenSize.height / 2 - h / 2);
        f.setSize(w, h);
        f.setResizable(false);
        f.setVisible(true);
    }
}
