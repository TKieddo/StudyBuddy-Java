import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class CaptureModalOnly {
    static final int SCALE = 2;
    public static void main(String[] args) throws Exception {
        File out = new File("report-screenshots/fresh/hi-add-subject.png");
        SwingUtilities.invokeAndWait(() -> {
            try {
                SoftField name = new SoftField();
                ChoicePill itPill = new ChoicePill("IT Courses");
                ChoicePill otherPill = new ChoicePill("Other");
                itPill.setSelected(true);
                JPanel catRow = new JPanel(new GridLayout(1, 2, 10, 0));
                catRow.setOpaque(false);
                catRow.add(itPill); catRow.add(otherPill);
                JLabel nameLbl = new JLabel("Subject name");
                nameLbl.setFont(UiKit.font(Font.BOLD, 12f));
                nameLbl.setForeground(UiKit.MUTED);
                JLabel catLbl = new JLabel("Category");
                catLbl.setFont(UiKit.font(Font.BOLD, 12f));
                catLbl.setForeground(UiKit.MUTED);
                JPanel form = new JPanel();
                form.setOpaque(false);
                form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
                for (JComponent c : new JComponent[]{nameLbl, name, catLbl, catRow}) {
                    c.setAlignmentX(Component.LEFT_ALIGNMENT);
                }
                name.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
                catRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
                form.add(nameLbl); form.add(Box.createVerticalStrut(6)); form.add(name);
                form.add(Box.createVerticalStrut(14)); form.add(catLbl);
                form.add(Box.createVerticalStrut(8)); form.add(catRow);
                ModalSheet modal = new ModalSheet((Window) null, "Add subject",
                        "Create a folder for PDFs and slides. Each import becomes its own document.",
                        "Create subject");
                modal.setBody(form);
                modal.pack();
                modal.setSize(440, 360);
                JComponent root = (JComponent) modal.getContentPane();
                root.setSize(440, 360);
                root.doLayout(); root.validate();
                int w = 440, h = 360;
                BufferedImage img = new BufferedImage(w*SCALE, h*SCALE, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.scale(SCALE, SCALE);
                g.setColor(new Color(236,236,242));
                g.fillRect(0,0,w,h);
                root.printAll(g);
                g.dispose();
                ImageIO.write(img, "png", out);
                System.out.println("Wrote modal " + (w*SCALE) + "x" + (h*SCALE));
                modal.dispose();
            } catch (Exception e) { e.printStackTrace(); System.exit(1); }
        });
        System.exit(0);
    }
}
