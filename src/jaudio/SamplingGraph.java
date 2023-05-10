package jaudio;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Line2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

/**
 * Render a WaveForm.
 */
class SamplingGraph extends JPanel implements Runnable {

    private static final long serialVersionUID = 1L;

    private MainController mainController;
    private Thread thread;
    private final Font font12 = new Font("serif", Font.PLAIN, 12);
    final Color jfcBlue = new Color(204, 204, 255);
    final Color pink = new Color(255, 175, 175);

    public SamplingGraph(MainController mainController) {
        this.mainController = mainController;
        setBackground(new Color(20, 20, 20));
    }

    /**
     * Creates the wave form.
     *
     * @param audioBytes the audio bytes
     */
    public void createWaveForm(byte[] audioBytes) throws Exception {

        mainController.lines.removeAllElements(); // clear the old vector

        Dimension d = getSize();
        int w = d.width;
        int h = d.height - 15;
        mainController.audioData = null;

        mainController.audioData = mainController.waveData.extractFloatDataFromAudioInputStream(mainController.audioInputStream);
        int frames_per_pixel = mainController.waveData.getAudioBytes().length / mainController.waveData.getFormat().getFrameSize() / w;
        byte my_byte = 0;
        double y_last = 0;
        // we need the format object
        int numChannels = mainController.waveData.getFormat().getChannels();
        for (double x = 0; x < w && mainController.audioData != null; x++) {
            int idx = (int) (frames_per_pixel * numChannels * x);
            if (mainController.waveData.getFormat().getSampleSizeInBits() == 8) {
                my_byte = (byte) mainController.audioData[idx];
            } else {
                my_byte = (byte) (128 * mainController.audioData[idx] / 32768);
            }
            double y_new = (double) (h * (128 - my_byte) / 256);
            mainController.lines.add(new Line2D.Double(x, y_last, x, y_new));
            y_last = y_new;
        }
        // just need lines object to repaint()
        repaint();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.swing.JComponent#paint(java.awt.Graphics)
     */
    public void paint(Graphics g) {

        Dimension d = getSize();
        int w = d.width;
        int h = d.height;
        int INFOPAD = 15;

        Graphics2D g2 = (Graphics2D) g;
        g2.setBackground(getBackground());
        g2.clearRect(0, 0, w, h);
        g2.setColor(Color.white);
        g2.fillRect(0, h - INFOPAD, w, INFOPAD);

        if (mainController.errStr != null) {
            g2.setColor(jfcBlue);
            g2.setFont(new Font("serif", Font.BOLD, 18));
            g2.drawString("ERROR", 5, 20);
            AttributedString as = new AttributedString(mainController.errStr);
            as.addAttribute(TextAttribute.FONT, font12, 0, mainController.errStr.length());
            AttributedCharacterIterator aci = as.getIterator();
            FontRenderContext frc = g2.getFontRenderContext();
            LineBreakMeasurer lbm = new LineBreakMeasurer(aci, frc);
            float x = 5, y = 25;
            lbm.setPosition(0);
            while (lbm.getPosition() < mainController.errStr.length()) {
                TextLayout tl = lbm.nextLayout(w - x - 5);
                if (!tl.isLeftToRight()) {
                    x = w - tl.getAdvance();
                }
                tl.draw(g2, x, y += tl.getAscent());
                y += tl.getDescent() + tl.getLeading();
            }
        } else if (mainController.recorder.thread != null) {
            // paint during capture
            g2.setColor(Color.black);
            g2.setFont(font12);
            g2.drawString("Length: " + mainController.seconds, 3, h - 4);
        } else {
            // paint during playback
            g2.setColor(Color.black);
            g2.setFont(font12);
            g2.drawString("Length: " + mainController.duration + "    Position: " + mainController.seconds, 3, h - 4);

            if (mainController.audioInputStream != null) {
                // .. render sampling graph ..
                g2.setColor(jfcBlue);
                for (int i = 1; i < mainController.lines.size(); i++) {
                    g2.draw((Line2D) mainController.lines.get(i));
                }

                // .. draw current position ..
                if (mainController.seconds != 0) {
                    double loc = mainController.seconds / mainController.duration * w;
                    g2.setColor(pink);
                    g2.setStroke(new BasicStroke(3));
                    g2.draw(new Line2D.Double(loc, 0, loc, h - INFOPAD - 2));
                }
            }
        }
    }

    public void start() {
        thread = new Thread(this);
        thread.setName("SamplingGraph");
        thread.start();
        mainController.seconds = 0;
    }

    public void stop() {
        if (thread != null) {
            thread.interrupt();
        }
        thread = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Runnable#run()
     */
    public void run() {
        mainController.seconds = 0;
        while (thread != null) {
            if ((mainController.player.line != null) && (mainController.player.line.isOpen())) {

                long milliseconds = (long) (mainController.player.line.getMicrosecondPosition() / 1000);
                mainController.seconds = milliseconds / 1000.0;
            } else if ((mainController.recorder.line != null) && (mainController.recorder.line.isActive())) {

                long milliseconds = (long) (mainController.recorder.line.getMicrosecondPosition() / 1000);
                mainController.seconds = milliseconds / 1000.0;
            }

            try {
                Thread.sleep(100);
            } catch (Exception e) {
                break;
            }

            repaint();

            while ((mainController.recorder.line != null && !mainController.recorder.line.isActive()) || (mainController.player.line != null && !mainController.player.line.isOpen())) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                    break;
                }
            }
        }
        mainController.seconds = 0;
        repaint();
    }
}
