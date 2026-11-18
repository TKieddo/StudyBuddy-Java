import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class CaptureReportShots {
    static final int SCALE = 2; // retina-ish sharpness for print

    public static void main(String[] args) throws Exception {
        final File outDir = new File("report-screenshots/fresh");
        outDir.mkdirs();
        SwingUtilities.invokeAndWait(() -> {
            try {
                StudyBuddyApp app = new StudyBuddyApp();
                app.setVisible(true);
                app.toFront();
                Method show = StudyBuddyApp.class.getDeclaredMethod("showPage", String.class);
                show.setAccessible(true);
                Field courseField = StudyBuddyApp.class.getDeclaredField("selectedCourse");
                courseField.setAccessible(true);
                Field docField = StudyBuddyApp.class.getDeclaredField("selectedDoc");
                docField.setAccessible(true);
                Field storeField = StudyBuddyApp.class.getDeclaredField("store");
                storeField.setAccessible(true);
                LibraryStore store = (LibraryStore) storeField.get(app);
                if (store.getCourses().size() > 0) {
                    Course c = store.getCourses().get(0);
                    courseField.set(app, c);
                    if (c.docs.size() > 0) docField.set(app, c.docs.get(0));
                }
                Method refresh = StudyBuddyApp.class.getDeclaredMethod("refreshAll");
                refresh.setAccessible(true);
                refresh.invoke(app);
                pause(500);

                show.invoke(app, "overview"); pause(400);
                shoot(app, new File(outDir, "hi-overview.png"));

                show.invoke(app, "course"); pause(400);
                shoot(app, new File(outDir, "hi-course.png"));

                show.invoke(app, "docs"); pause(350);
                shoot(app, new File(outDir, "hi-docs.png"));

                show.invoke(app, "summary"); pause(400);
                shoot(app, new File(outDir, "hi-summary.png"));

                show.invoke(app, "quiz"); pause(400);
                shoot(app, new File(outDir, "hi-quiz.png"));

                show.invoke(app, "cards"); pause(400);
                shoot(app, new File(outDir, "hi-cards.png"));

                show.invoke(app, "plan"); pause(400);
                shoot(app, new File(outDir, "hi-schedule.png"));

                System.out.println("Done hi-res shots");
                app.dispose();
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
    }

    static void pause(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    static void shoot(JFrame frame, File file) throws Exception {
        JComponent root = (JComponent) frame.getContentPane();
        root.doLayout();
        root.revalidate();
        root.repaint();
        pause(100);
        int w = Math.max(root.getWidth(), 1);
        int h = Math.max(root.getHeight(), 1);
        BufferedImage img = new BufferedImage(w * SCALE, h * SCALE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.scale(SCALE, SCALE);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        root.printAll(g);
        g.dispose();
        ImageIO.write(img, "png", file);
        System.out.println("Wrote " + file.getName() + " " + (w*SCALE) + "x" + (h*SCALE));
    }
}
