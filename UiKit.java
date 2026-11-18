import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;

/*
    Shared look: soft grey dashboard, white rounded cards, pill buttons.
    Uses Inter from the fonts folder when available.
*/
class UiKit {
    static final Color BG = new Color(236, 236, 240);
    static final Color SIDEBAR = new Color(248, 248, 250);
    static final Color INK = new Color(28, 28, 35);
    static final Color MUTED = new Color(120, 120, 132);
    static final Color WHITE = Color.WHITE;
    static final Color LINE = new Color(225, 225, 232);
    static final Color ACCENT = new Color(40, 40, 48);
    static final Color PILL_A = new Color(255, 90, 110);
    static final Color PILL_B = new Color(140, 80, 240);

    static final Color[] TAG_COLORS = new Color[] {
            new Color(255, 140, 90),
            new Color(90, 170, 255),
            new Color(120, 210, 140),
            new Color(190, 130, 255),
            new Color(255, 180, 70),
            new Color(255, 120, 150),
            new Color(100, 190, 210)
    };

    // Soft pastel fills for plan day cards
    static final Color[] PLAN_CARD_COLORS = new Color[] {
            new Color(255, 236, 228),
            new Color(230, 242, 255),
            new Color(232, 248, 236),
            new Color(242, 234, 255),
            new Color(255, 246, 230),
            new Color(255, 232, 238),
            new Color(228, 246, 250)
    };

    static Font interRegular;
    static Font interMedium;
    static Font interSemiBold;
    static Font interBold;
    static boolean fontsReady;

    static {
        fontsReady = loadFonts();
    }

    static boolean loadFonts() {
        try {
            interRegular = Font.createFont(Font.TRUETYPE_FONT, new File("fonts/Inter-Regular.ttf"));
            interMedium = Font.createFont(Font.TRUETYPE_FONT, new File("fonts/Inter-Medium.ttf"));
            interSemiBold = Font.createFont(Font.TRUETYPE_FONT, new File("fonts/Inter-SemiBold.ttf"));
            interBold = Font.createFont(Font.TRUETYPE_FONT, new File("fonts/Inter-Bold.ttf"));
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(interRegular);
            ge.registerFont(interMedium);
            ge.registerFont(interSemiBold);
            ge.registerFont(interBold);
            return true;
        } catch (Exception e) {
            interRegular = null;
            interMedium = null;
            interSemiBold = null;
            interBold = null;
            return false;
        }
    }

    static Font font(int style, float size) {
        if (fontsReady) {
            if (style == Font.BOLD) {
                return interBold.deriveFont(Font.PLAIN, size);
            }
            // Treat “plain” as Medium for UI text — cleaner than thin Regular on screens
            return interMedium.deriveFont(Font.PLAIN, size);
        }
        // Fallback if font files are missing
        String name = "Avenir Next";
        Font probe = new Font(name, style, Math.round(size));
        if (probe.getFamily().toLowerCase().contains("dialog")) {
            name = "Helvetica Neue";
            probe = new Font(name, style, Math.round(size));
        }
        if (probe.getFamily().toLowerCase().contains("dialog")) {
            return new Font("SansSerif", style, Math.round(size));
        }
        return probe;
    }

    static Font fontBold(float size) {
        return font(Font.BOLD, size);
    }

    // Apply Inter to Swing defaults so menus, dialogs, and labels match.
    static void applyAppFont() {
        Font ui = font(Font.PLAIN, 13f);
        Font uiBold = font(Font.BOLD, 13f);
        UIManager.put("Label.font", ui);
        UIManager.put("Button.font", uiBold);
        UIManager.put("ToggleButton.font", ui);
        UIManager.put("RadioButton.font", ui);
        UIManager.put("CheckBox.font", ui);
        UIManager.put("ColorChooser.font", ui);
        UIManager.put("ComboBox.font", ui);
        UIManager.put("Label.font", ui);
        UIManager.put("List.font", ui);
        UIManager.put("MenuBar.font", ui);
        UIManager.put("MenuItem.font", ui);
        UIManager.put("RadioButtonMenuItem.font", ui);
        UIManager.put("CheckBoxMenuItem.font", ui);
        UIManager.put("Menu.font", ui);
        UIManager.put("PopupMenu.font", ui);
        UIManager.put("OptionPane.font", ui);
        UIManager.put("OptionPane.messageFont", ui);
        UIManager.put("OptionPane.buttonFont", uiBold);
        UIManager.put("Panel.font", ui);
        UIManager.put("ProgressBar.font", ui);
        UIManager.put("ScrollPane.font", ui);
        UIManager.put("Viewport.font", ui);
        UIManager.put("TabbedPane.font", uiBold);
        UIManager.put("Table.font", ui);
        UIManager.put("TableHeader.font", uiBold);
        UIManager.put("TextField.font", ui);
        UIManager.put("PasswordField.font", ui);
        UIManager.put("TextArea.font", ui);
        UIManager.put("TextPane.font", ui);
        UIManager.put("EditorPane.font", ui);
        UIManager.put("TitledBorder.font", uiBold);
        UIManager.put("ToolBar.font", ui);
        UIManager.put("ToolTip.font", ui);
        UIManager.put("Tree.font", ui);
    }

    static void styleArea(JTextArea area, float size) {
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setOpaque(false);
        area.setForeground(INK);
        area.setFont(font(Font.PLAIN, size));
        area.setMargin(new Insets(12, 12, 12, 12));
    }

    static Color tagColor(int index) {
        return TAG_COLORS[Math.abs(index) % TAG_COLORS.length];
    }

    static Color planCardColor(int index) {
        return PLAN_CARD_COLORS[Math.abs(index) % PLAN_CARD_COLORS.length];
    }
}

class SoftPanel extends JPanel {
    Color fill;
    int arc;

    public SoftPanel(Color fill, int arc) {
        this.fill = fill;
        this.arc = arc;
        setOpaque(false);
        setLayout(new BorderLayout());
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(0, 0, 0, 18));
        g2.fillRoundRect(3, 5, getWidth() - 6, getHeight() - 6, arc, arc);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }
}

class PillButton extends JButton {
    boolean primary;
    boolean dark;
    boolean hover;
    boolean useOverride;
    Color overrideFill;
    Color overrideBorder;

    public PillButton(String text, boolean primary) {
        this(text, primary, false);
    }

    public PillButton(String text, boolean primary, boolean dark) {
        super(text);
        this.primary = primary;
        this.dark = dark;
        setFont(UiKit.font(Font.BOLD, 13));
        setForeground(primary || dark ? Color.WHITE : UiKit.INK);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(10, 18, 10, 18));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    public void clearColorOverride() {
        useOverride = false;
        overrideFill = null;
        overrideBorder = null;
        if (!primary && !dark) {
            setForeground(UiKit.INK);
        }
        repaint();
    }

    // Soft fill used for quiz options (pastel / green / red).
    public void setColorOverride(Color fill, Color text, Color border) {
        useOverride = true;
        overrideFill = fill;
        overrideBorder = border;
        setForeground(text);
        repaint();
    }

    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width, 40), Math.max(40, d.height));
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        if (useOverride && overrideFill != null) {
            Color fill = hover ? overrideFill.brighter() : overrideFill;
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w, h, h, h);
            if (overrideBorder != null) {
                g2.setColor(overrideBorder);
                g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
            }
        } else if (primary) {
            Color a = hover ? new Color(255, 105, 125) : UiKit.PILL_A;
            Color b = hover ? new Color(160, 95, 255) : UiKit.PILL_B;
            g2.setPaint(new GradientPaint(0, 0, a, w, h, b));
            g2.fillRoundRect(0, 0, w, h, h, h);
        } else if (dark) {
            g2.setColor(hover ? new Color(55, 55, 65) : UiKit.ACCENT);
            g2.fillRoundRect(0, 0, w, h, h, h);
        } else {
            g2.setColor(hover ? new Color(245, 245, 248) : Color.WHITE);
            g2.fillRoundRect(0, 0, w, h, h, h);
            g2.setColor(UiKit.LINE);
            g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
        }
        FontMetrics fm = g2.getFontMetrics(getFont());
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
        int tx = getHorizontalAlignment() == SwingConstants.LEFT ? 16
                : (w - fm.stringWidth(getText())) / 2;
        g2.setFont(getFont());
        g2.setColor(getForeground());
        String label = getText();
        int maxW = w - tx - 14;
        if (fm.stringWidth(label) > maxW) {
            while (label.length() > 3 && fm.stringWidth(label + "…") > maxW) {
                label = label.substring(0, label.length() - 1);
            }
            label = label + "…";
        }
        g2.drawString(label, tx, ty);
        g2.dispose();
    }
}

class NavButton extends JButton {
    boolean active;
    boolean hover;

    public NavButton(String text) {
        super(text);
        setHorizontalAlignment(SwingConstants.LEFT);
        setFont(UiKit.font(Font.PLAIN, 14));
        setForeground(UiKit.MUTED);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(12, 16, 12, 16));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    public void setActive(boolean active) {
        this.active = active;
        setFont(UiKit.font(active ? Font.BOLD : Font.PLAIN, 14));
        setForeground(active ? UiKit.INK : UiKit.MUTED);
        repaint();
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (active || hover) {
            g2.setColor(active ? new Color(235, 235, 240) : new Color(242, 242, 246));
            g2.fillRoundRect(4, 2, getWidth() - 8, getHeight() - 4, 14, 14);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}

class CategoryChip extends JButton {
    boolean selected;
    boolean hover;

    public CategoryChip(String text) {
        super(text);
        setFont(UiKit.font(Font.BOLD, 11));
        setForeground(UiKit.INK);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(96, 72));
        setMinimumSize(new Dimension(96, 72));
        setMaximumSize(new Dimension(96, 72));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        setForeground(selected ? Color.WHITE : UiKit.INK);
        repaint();
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (selected) {
            g2.setColor(UiKit.ACCENT);
        } else {
            g2.setColor(hover ? new Color(250, 250, 252) : Color.WHITE);
        }
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
        if (!selected) {
            g2.setColor(UiKit.LINE);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}

/*
    Chat bubble that hugs short text. Long text wraps inside a fixed max
    width (including long words / JSON with no spaces).
*/
class ChatBubble extends JPanel {
    static final int ARC = 16;

    boolean mine;
    JTextArea area;
    Color fill;
    int bubbleW;
    int bubbleH;

    public ChatBubble(String text, boolean mine) {
        this(text, mine, 220);
    }

    // maxOuter = hard max width for the whole bubble (must fit the chat viewport).
    public ChatBubble(String text, boolean mine, int maxOuter) {
        this.mine = mine;
        this.fill = mine ? new Color(18, 18, 22) : Color.WHITE;
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(1, 1, 1, 1));

        // Whole bubble must stay inside maxOuter (text area is smaller than that).
        int maxBubble = Math.max(100, maxOuter);
        int chrome = 22; // text insets + round border
        int maxInner = Math.max(72, maxBubble - chrome);
        if (mine) {
            maxInner = Math.min(maxInner, (int) (maxBubble * 0.82) - chrome / 2);
            maxInner = Math.max(56, maxInner);
        }

        area = new JTextArea(text == null ? "" : text);
        area.setFont(UiKit.font(Font.PLAIN, 12.5f));
        area.setForeground(mine ? Color.WHITE : UiKit.INK);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(new EmptyBorder(8, 10, 8, 10));

        FontMetrics fm = area.getFontMetrics(area.getFont());
        int raw = 0;
        String[] lines = area.getText().split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            raw = Math.max(raw, fm.stringWidth(lines[i]));
        }
        int innerW;
        if (mine) {
            innerW = Math.min(maxInner, Math.max(36, raw + 4));
        } else if (area.getText().contains("\n") || raw > maxInner) {
            innerW = maxInner;
        } else {
            innerW = Math.min(maxInner, Math.max(90, raw + 4));
        }
        area.setSize(innerW, Short.MAX_VALUE);
        int innerH = area.getPreferredSize().height;

        // Never exceed the chat column width — this was clipping on the right.
        bubbleW = Math.min(maxBubble, innerW + chrome);
        bubbleH = Math.max(innerH, 34);

        JPanel bubble = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, mine ? 30 : 12));
                g2.fillRoundRect(1, 2, getWidth() - 2, getHeight() - 2, ARC, ARC);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
                if (!mine) {
                    g2.setColor(new Color(228, 228, 234));
                    g2.drawRoundRect(0, 0, getWidth() - 2, getHeight() - 2, ARC, ARC);
                }
                g2.dispose();
            }

            public boolean isOpaque() {
                return false;
            }
        };
        bubble.setPreferredSize(new Dimension(bubbleW, bubbleH));
        bubble.setMaximumSize(new Dimension(bubbleW, bubbleH));
        bubble.add(area, BorderLayout.CENTER);
        add(bubble, BorderLayout.CENTER);
    }

    public Dimension getPreferredSize() {
        Insets in = getInsets();
        return new Dimension(bubbleW + in.left + in.right, bubbleH + in.top + in.bottom);
    }

    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }
}

// Soft rounded text field for the Ask assistant row.
class RoundTextField extends JTextField {
    int arc = 18;

    public RoundTextField() {
        setOpaque(false);
        setBorder(new EmptyBorder(12, 14, 12, 14));
        setFont(UiKit.font(Font.PLAIN, 13f));
        setForeground(UiKit.INK);
        setCaretColor(UiKit.INK);
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(248, 248, 252));
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        g2.setColor(new Color(230, 230, 238));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }
}

class AvatarCircle extends JPanel {
    String initials;
    boolean online;

    public AvatarCircle(String initials, boolean online) {
        this.initials = initials;
        this.online = online;
        setOpaque(false);
        setPreferredSize(new Dimension(72, 72));
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int s = Math.min(getWidth(), getHeight()) - 4;
        g2.setPaint(new GradientPaint(0, 0, new Color(255, 160, 120), s, s, new Color(160, 120, 255)));
        g2.fill(new Ellipse2D.Float(2, 2, s, s));
        g2.setColor(Color.WHITE);
        g2.setFont(UiKit.font(Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(initials, (s - fm.stringWidth(initials)) / 2 + 2, s / 2 + fm.getAscent() / 2);
        if (online) {
            g2.setColor(new Color(70, 200, 120));
            g2.fillOval(s - 14, s - 14, 16, 16);
            g2.setColor(Color.WHITE);
            g2.drawOval(s - 14, s - 14, 16, 16);
        }
        g2.dispose();
    }
}

// Big selectable option used instead of ugly native combo boxes.
class ChoicePill extends JButton {
    boolean selected;
    boolean hover;

    public ChoicePill(String text) {
        super(text);
        setFont(UiKit.font(Font.BOLD, 13f));
        setForeground(UiKit.INK);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(12, 14, 12, 14));
        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        setForeground(selected ? Color.WHITE : UiKit.INK);
        repaint();
    }

    public Dimension getPreferredSize() {
        return new Dimension(140, 46);
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        if (selected) {
            g2.setPaint(new GradientPaint(0, 0, UiKit.PILL_A, w, h, UiKit.PILL_B));
        } else if (hover) {
            g2.setColor(new Color(242, 242, 248));
        } else {
            g2.setColor(new Color(246, 246, 250));
        }
        g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
        if (!selected) {
            g2.setColor(new Color(228, 228, 236));
            g2.drawRoundRect(0, 0, w - 2, h - 2, 16, 16);
        }
        FontMetrics fm = g2.getFontMetrics(getFont());
        int tx = (w - fm.stringWidth(getText())) / 2;
        int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.setFont(getFont());
        g2.setColor(getForeground());
        g2.drawString(getText(), tx, ty);
        g2.dispose();
    }
}

// Soft field used inside modal forms.
class SoftField extends JTextField {
    public SoftField() {
        this("");
    }

    public SoftField(String text) {
        super(text);
        setOpaque(false);
        setBorder(new EmptyBorder(12, 14, 12, 14));
        setFont(UiKit.font(Font.PLAIN, 14f));
        setForeground(UiKit.INK);
        setCaretColor(UiKit.INK);
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(246, 246, 250));
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g2.setColor(new Color(228, 228, 236));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g2.dispose();
        super.paintComponent(g);
    }

    public Dimension getPreferredSize() {
        // Cap width so long text does not blow up modal dialogs.
        return new Dimension(280, 44);
    }

    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 44);
    }
}

// Multi-line soft field for editing plan tasks.
class SoftArea extends JTextArea {
    public SoftArea(String text) {
        super(text == null ? "" : text);
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(false);
        setBorder(new EmptyBorder(10, 12, 10, 12));
        setFont(UiKit.font(Font.PLAIN, 13f));
        setForeground(UiKit.INK);
        setCaretColor(UiKit.INK);
        setRows(3);
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(246, 246, 250));
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g2.setColor(new Color(228, 228, 236));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
        g2.dispose();
        super.paintComponent(g);
    }
}

/*
    Centered white modal used instead of plain JOptionPane dialogs.
    Title + subtitle on top, body in the middle, Cancel / primary at the bottom.
*/
class ModalSheet extends JDialog {
    boolean accepted;
    JPanel bodyHost;
    PillButton okBtn;
    PillButton cancelBtn;

    public ModalSheet(Window owner, String title, String subtitle, String okLabel) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        accepted = false;

        SoftPanel card = new SoftPanel(Color.WHITE, 26);
        card.setBorder(new EmptyBorder(22, 24, 20, 24));
        card.setLayout(new BorderLayout(0, 16));

        JLabel t = new JLabel(title);
        t.setFont(UiKit.font(Font.BOLD, 20));
        t.setForeground(UiKit.INK);
        JLabel s = new JLabel("<html><body style='width:320px'>" + (subtitle == null ? "" : subtitle)
                + "</body></html>");
        s.setFont(UiKit.font(Font.PLAIN, 12.5f));
        s.setForeground(UiKit.MUTED);

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.add(t);
        head.add(Box.createVerticalStrut(6));
        head.add(s);

        bodyHost = new JPanel(new BorderLayout());
        bodyHost.setOpaque(false);

        cancelBtn = new PillButton("Cancel", false);
        okBtn = new PillButton(okLabel == null ? "Save" : okLabel, true);
        cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                accepted = false;
                dispose();
            }
        });
        okBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                accepted = true;
                dispose();
            }
        });

        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        foot.setOpaque(false);
        foot.add(cancelBtn);
        foot.add(okBtn);

        card.add(head, BorderLayout.NORTH);
        card.add(bodyHost, BorderLayout.CENTER);
        card.add(foot, BorderLayout.SOUTH);

        JPanel wrap = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(236, 236, 242));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
                g2.setColor(new Color(210, 210, 220));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 28, 28);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(10, 10, 10, 10));
        wrap.add(card, BorderLayout.CENTER);
        setContentPane(wrap);
        getRootPane().putClientProperty("apple.awt.draggableWindowBackground", Boolean.TRUE);
        setSize(440, 360);
        setLocationRelativeTo(owner);
    }

    public void setBody(JComponent body) {
        bodyHost.removeAll();
        bodyHost.add(body, BorderLayout.CENTER);
        bodyHost.revalidate();
        bodyHost.repaint();
    }

    public void setOkLabel(String label) {
        okBtn.setText(label);
    }

    public boolean showModal() {
        pack();
        Dimension d = getPreferredSize();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        // Never wider/taller than the screen — long SoftField text used to explode width.
        int w = Math.min(Math.max(400, d.width), Math.min(480, screen.width - 80));
        int h = Math.min(Math.max(260, d.height), screen.height - 120);
        setSize(w, h);
        setLocationRelativeTo(getOwner());
        setVisible(true);
        return accepted;
    }

    public void setFixedSize(int width, int height) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.min(width, screen.width - 80);
        int h = Math.min(height, screen.height - 120);
        setPreferredSize(new Dimension(w, h));
        setSize(w, h);
    }

    public static void alert(Window owner, String title, String message) {
        ModalSheet m = new ModalSheet(owner, title, message, "OK");
        m.cancelBtn.setVisible(false);
        JPanel empty = new JPanel();
        empty.setOpaque(false);
        empty.setPreferredSize(new Dimension(360, 8));
        m.setBody(empty);
        m.showModal();
    }

    public static String askText(Window owner, String title, String subtitle, String initial) {
        SoftField field = new SoftField(initial == null ? "" : initial);
        ModalSheet m = new ModalSheet(owner, title, subtitle, "Save");
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(field, BorderLayout.CENTER);
        m.setBody(body);
        if (!m.showModal()) {
            return null;
        }
        String v = field.getText().trim();
        return v.length() == 0 ? null : v;
    }
}

/*
    Non-blocking “working” panel shown while AI builds a study pack.
    Updates on the EDT via setStatus so the user sees real steps.
*/
class WorkingSheet extends JDialog {
    JLabel titleLabel;
    JLabel statusLabel;
    JLabel detailLabel;
    JProgressBar bar;
    javax.swing.Timer pulse;
    int dots;
    String baseStatus = "Working";

    public WorkingSheet(Window owner, String title, String fileName) {
        super(owner, ModalityType.MODELESS);
        setUndecorated(true);
        setAlwaysOnTop(true);

        SoftPanel card = new SoftPanel(Color.WHITE, 26);
        card.setBorder(new EmptyBorder(24, 26, 22, 26));
        card.setLayout(new BorderLayout(0, 14));

        titleLabel = new JLabel(title);
        titleLabel.setFont(UiKit.font(Font.BOLD, 18));
        titleLabel.setForeground(UiKit.INK);

        detailLabel = new JLabel("<html><body style='width:300px'>File: "
                + (fileName == null ? "…" : fileName) + "</body></html>");
        detailLabel.setFont(UiKit.font(Font.PLAIN, 12f));
        detailLabel.setForeground(UiKit.MUTED);

        statusLabel = new JLabel("Starting…");
        statusLabel.setFont(UiKit.font(Font.PLAIN, 13.5f));
        statusLabel.setForeground(UiKit.INK);

        bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setBorderPainted(false);
        bar.setPreferredSize(new Dimension(320, 8));
        bar.setForeground(UiKit.PILL_A);
        bar.setBackground(new Color(235, 235, 242));

        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        mid.add(titleLabel);
        mid.add(Box.createVerticalStrut(6));
        mid.add(detailLabel);
        mid.add(Box.createVerticalStrut(16));
        mid.add(statusLabel);
        mid.add(Box.createVerticalStrut(12));
        mid.add(bar);
        mid.add(Box.createVerticalStrut(8));
        JLabel wait = new JLabel("At least 6 quiz + 6 cards — often under a minute.");
        wait.setFont(UiKit.font(Font.PLAIN, 11.5f));
        wait.setForeground(UiKit.MUTED);
        wait.setAlignmentX(Component.LEFT_ALIGNMENT);
        mid.add(wait);

        card.add(mid, BorderLayout.CENTER);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(new Color(236, 236, 242));
        wrap.setBorder(new EmptyBorder(8, 8, 8, 8));
        wrap.add(card, BorderLayout.CENTER);
        setContentPane(wrap);
        setSize(420, 240);
        setLocationRelativeTo(owner);

        dots = 0;
        pulse = new javax.swing.Timer(400, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dots = (dots + 1) % 4;
                String d = "";
                for (int i = 0; i < dots; i++) {
                    d = d + ".";
                }
                statusLabel.setText(baseStatus + d);
            }
        });
    }

    public void setStatus(String status) {
        if (status == null) {
            return;
        }
        baseStatus = status;
        dots = 0;
        statusLabel.setText(baseStatus);
    }

    public void openSheet() {
        setVisible(true);
        pulse.start();
        toFront();
    }

    public void closeSheet() {
        if (pulse != null) {
            pulse.stop();
        }
        setVisible(false);
        dispose();
    }
}
