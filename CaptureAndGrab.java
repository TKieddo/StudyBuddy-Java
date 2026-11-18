import javax.swing.*;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Show each page long enough for an external screencapture -l. */
public class CaptureAndGrab {
    public static void main(String[] args) throws Exception {
        String page = args.length > 0 ? args[0] : "overview";
        String out = args.length > 1 ? args[1] : "report-screenshots/tmp.png";
        SwingUtilities.invokeAndWait(() -> {
            try {
                StudyBuddyApp app = new StudyBuddyApp();
                app.setVisible(true);
                app.toFront();
                app.setAlwaysOnTop(true);

                Field storeField = StudyBuddyApp.class.getDeclaredField("store");
                storeField.setAccessible(true);
                LibraryStore store = (LibraryStore) storeField.get(app);
                Field courseField = StudyBuddyApp.class.getDeclaredField("selectedCourse");
                courseField.setAccessible(true);
                Field docField = StudyBuddyApp.class.getDeclaredField("selectedDoc");
                docField.setAccessible(true);
                if (store.getCourses().size() > 0) {
                    Course c = store.getCourses().get(0);
                    courseField.set(app, c);
                    if (c.docs.size() > 0) docField.set(app, c.docs.get(0));
                }
                Method refresh = StudyBuddyApp.class.getDeclaredMethod("refreshAll");
                refresh.setAccessible(true);
                refresh.invoke(app);
                Method show = StudyBuddyApp.class.getDeclaredMethod("showPage", String.class);
                show.setAccessible(true);
                show.invoke(app, page);
                app.revalidate();
                app.repaint();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
        // keep alive for screencapture
        Thread.sleep(900);
        // print window id hint
        System.out.println("READY " + out);
        Thread.sleep(200);
        System.exit(0);
    }
}
