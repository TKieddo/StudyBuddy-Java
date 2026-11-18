import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;

public class CaptureModalShot {
    public static void main(String[] args) throws Exception {
        File out = new File("report-screenshots/fresh/08-add-subject.png");
        SwingUtilities.invokeAndWait(() -> {
            try {
                StudyBuddyApp app = new StudyBuddyApp();
                app.setVisible(true);
                app.toFront();
                Method add = StudyBuddyApp.class.getDeclaredMethod("addSubjectDialog");
                add.setAccessible(true);
                // open modal on EDT after short delay via timer
                Timer t = new Timer(500, e -> {
                    try {
                        add.invoke(app);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                t.setRepeats(false);
                t.start();
                Thread.sleep(1200);
                // Capture whole layered pane including modal
                JRootPane root = app.getRootPane();
                int w = root.getWidth();
                int h = root.getHeight();
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, w, h);
                root.printAll(g);
                g.dispose();
                ImageIO.write(img, "png", out);
                System.out.println("Wrote " + out);
                app.dispose();
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
    }
}
