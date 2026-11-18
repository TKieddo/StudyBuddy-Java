import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

/*
    Study Buddy AI — course dashboard.

    Subjects (Java, Assembly, …) hold docs (PDFs / images).
    Each doc has summary, quiz, flashcards, and a study plan.
    Profile + scores sit on the right.
*/
public class StudyBuddyApp extends JFrame {
    AiService ai = new AiService();
    LibraryStore store = new LibraryStore(ai);

    Course selectedCourse;
    StudyDoc selectedDoc;
    String categoryFilter = "All Courses";

    CardLayout pages = new CardLayout();
    JPanel pageHost;
    JPanel overviewGrid;
    JPanel courseDocsPanel;
    JLabel courseTitleLabel;
    JLabel courseHintLabel;
    JLabel welcomeLabel;
    JLabel statusLabel;
    JLabel profileNameLabel;
    JLabel profileRoleLabel;
    JLabel statDoneNum;
    JLabel statQuizNum;
    JLabel statCardsNum;
    JLabel statBestNum;
    JPanel askChatList;
    JScrollPane askChatScroll;
    JTextField askField;
    JPanel thinkingRow;
    JLabel thinkingLabel;
    javax.swing.Timer thinkingTimer;
    int thinkingDots;
    PillButton askSendBtn;
    JLabel askContextLabel;

    JEditorPane summaryPane;
    JPanel planWeekGrid;
    JLabel planDocLabel;
    JLabel quizProgress;
    JTextArea quizQuestion;
    PillButton[] answerButtons = new PillButton[4];
    JLabel quizFeedback;
    int quizIndex;
    int quizScore;
    String[] shuffledAnswers;
    String currentCorrect;
    boolean answered;
    boolean quizCountedThisRun;

    JLabel cardProgress;
    JLabel cardSideHint;
    JLabel cardHowTo;
    JLabel cardFlipTip;
    JLabel contextBar;
    JTextArea cardFace;
    SoftPanel flashFace;
    int cardIndex;
    boolean showingBack;

    NavButton[] navButtons;
    String[] navIds = {"overview", "courses", "docs", "quiz", "cards", "plan"};
    String[] navLabels = {"Overview", "Courses", "My docs", "Quizzes", "Flashcards", "Schedule"};

    public StudyBuddyApp() {
        setTitle("Study Buddy AI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 780);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBackground(UiKit.BG);
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildProfile(), BorderLayout.EAST);

        setContentPane(root);

        if (store.getCourses().size() > 0) {
            selectedCourse = store.getCourses().get(0);
            if (selectedCourse.docs.size() > 0) {
                selectedDoc = selectedCourse.docs.get(0);
            }
        }
        refreshAll();
        showPage("overview");
    }

    // ---------- Sidebar ----------

    private JPanel buildSidebar() {
        SoftPanel side = new SoftPanel(UiKit.SIDEBAR, 28);
        side.setPreferredSize(new Dimension(180, 0));
        side.setBorder(new EmptyBorder(14, 10, 14, 10));
        side.setLayout(new BorderLayout(0, 12));

        JLabel brand = new JLabel("Study Buddy AI");
        brand.setFont(UiKit.font(Font.BOLD, 16));
        brand.setForeground(UiKit.INK);
        brand.setBorder(new EmptyBorder(4, 8, 16, 8));

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        navButtons = new NavButton[navIds.length];
        for (int i = 0; i < navIds.length; i++) {
            final String id = navIds[i];
            navButtons[i] = new NavButton(navLabels[i]);
            navButtons[i].setAlignmentX(Component.LEFT_ALIGNMENT);
            navButtons[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            navButtons[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showPage(id);
                }
            });
            nav.add(navButtons[i]);
            nav.add(Box.createVerticalStrut(4));
        }

        PillButton addCourse = new PillButton("+ Add subject", true);
        addCourse.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addSubjectDialog();
            }
        });

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(addCourse);

        side.add(brand, BorderLayout.NORTH);
        side.add(nav, BorderLayout.CENTER);
        side.add(bottom, BorderLayout.SOUTH);
        return side;
    }

    // ---------- Center pages ----------

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setOpaque(false);

        contextBar = new JLabel("Overview · pick a subject, then open a document");
        contextBar.setFont(UiKit.font(Font.PLAIN, 12));
        contextBar.setForeground(UiKit.MUTED);
        contextBar.setBorder(new EmptyBorder(0, 2, 4, 2));

        pageHost = new JPanel(pages);
        pageHost.setOpaque(false);

        pageHost.add(buildOverviewPage(), "overview");
        pageHost.add(buildCoursePage(), "course");
        pageHost.add(buildDocsPage(), "docs");
        pageHost.add(buildSummaryPage(), "summary");
        pageHost.add(buildQuizPage(), "quiz");
        pageHost.add(buildCardsPage(), "cards");
        pageHost.add(buildPlanPage(), "plan");

        center.add(contextBar, BorderLayout.NORTH);
        center.add(pageHost, BorderLayout.CENTER);
        return center;
    }

    private JPanel buildOverviewPage() {
        JPanel page = new JPanel(new BorderLayout(12, 12));
        page.setOpaque(false);

        welcomeLabel = new JLabel("Welcome back");
        welcomeLabel.setFont(UiKit.font(Font.BOLD, 24));
        welcomeLabel.setForeground(UiKit.INK);

        statusLabel = new JLabel("Subjects hold your docs. Each doc has its own summary, quiz, cards, and plan.");
        statusLabel.setForeground(UiKit.MUTED);
        statusLabel.setFont(UiKit.font(Font.PLAIN, 12));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(welcomeLabel, BorderLayout.NORTH);
        head.add(statusLabel, BorderLayout.SOUTH);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);
        chips.setName("categoryChips");
        String[] cats = {"All Courses", "IT Courses", "Other"};
        for (int i = 0; i < cats.length; i++) {
            final String cat = cats[i];
            CategoryChip chip = new CategoryChip(cat);
            chip.setSelected(cat.equals(categoryFilter));
            chip.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    categoryFilter = cat;
                    refreshOverview();
                }
            });
            chips.add(chip);
        }

        // WrapLayout keeps cards small — GridLayout was stretching them full height
        overviewGrid = new JPanel(new WrapLayout(FlowLayout.LEFT, 14, 14));
        overviewGrid.setOpaque(false);
        overviewGrid.setBorder(new EmptyBorder(4, 0, 4, 0));

        JScrollPane scroll = new JScrollPane(overviewGrid);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JPanel north = new JPanel(new BorderLayout(8, 10));
        north.setOpaque(false);
        north.add(head, BorderLayout.NORTH);
        north.add(chips, BorderLayout.SOUTH);

        page.add(north, BorderLayout.NORTH);
        page.add(scroll, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildCoursePage() {
        JPanel page = new JPanel(new BorderLayout(12, 12));
        page.setOpaque(false);

        courseTitleLabel = new JLabel("Course");
        courseTitleLabel.setFont(UiKit.font(Font.BOLD, 26));
        courseTitleLabel.setForeground(UiKit.INK);

        courseHintLabel = new JLabel("Import a PDF or slide image to build summary, quiz, and cards.");
        courseHintLabel.setFont(UiKit.font(Font.PLAIN, 12.5f));
        courseHintLabel.setForeground(UiKit.MUTED);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        courseTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        courseHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBlock.add(courseTitleLabel);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(courseHintLabel);

        // Buttons sit on their own row so they never cover the title.
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        PillButton back = new PillButton("Back", false);
        back.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showPage("overview");
            }
        });
        PillButton addPdf = new PillButton("Scan PDF", true);
        addPdf.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                importIntoSelected(true);
            }
        });
        PillButton addImg = new PillButton("Scan image", false);
        addImg.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                importIntoSelected(false);
            }
        });
        actions.add(back);
        actions.add(addImg);
        actions.add(addPdf);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(titleBlock, BorderLayout.NORTH);
        top.add(actions, BorderLayout.SOUTH);

        courseDocsPanel = new JPanel();
        courseDocsPanel.setOpaque(false);
        courseDocsPanel.setLayout(new BoxLayout(courseDocsPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(courseDocsPanel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        page.add(top, BorderLayout.NORTH);
        page.add(scroll, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildDocsPage() {
        JPanel page = new JPanel(new BorderLayout(12, 12));
        page.setOpaque(false);
        JLabel t = new JLabel("All documents");
        t.setFont(UiKit.font(Font.BOLD, 22));
        JLabel hint = new JLabel("To scan notes: Overview → open a subject → Add PDF to scan / Add image to scan");
        hint.setForeground(UiKit.MUTED);
        hint.setFont(UiKit.font(Font.PLAIN, 12));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(t, BorderLayout.NORTH);
        head.add(hint, BorderLayout.SOUTH);
        page.add(head, BorderLayout.NORTH);
        // placeholder; filled via refresh using course page pattern in a named panel
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setName("allDocsList");
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        page.add(scroll, BorderLayout.CENTER);
        page.putClientProperty("list", list);
        return page;
    }

    private JPanel buildSummaryPage() {
        JPanel page = new JPanel(new BorderLayout(12, 12));
        page.setOpaque(false);
        JLabel t = new JLabel("Summary");
        t.setFont(UiKit.font(Font.BOLD, 22));

        summaryPane = new JEditorPane();
        summaryPane.setEditable(false);
        summaryPane.setContentType("text/html");
                summaryPane.setText(summaryToHtml("Open a subject, add a PDF or image, then open that document to see its summary."));
        summaryPane.setBorder(new EmptyBorder(8, 12, 8, 12));
        summaryPane.setBackground(Color.WHITE);
        summaryPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        summaryPane.setFont(UiKit.font(Font.PLAIN, 14f));

        SoftPanel card = new SoftPanel(Color.WHITE, 22);
        card.setBorder(new EmptyBorder(6, 6, 6, 6));
        JScrollPane sp = new JScrollPane(summaryPane);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        card.add(sp, BorderLayout.CENTER);

        page.add(t, BorderLayout.NORTH);
        page.add(card, BorderLayout.CENTER);
        page.add(docActionBar(), BorderLayout.SOUTH);
        return page;
    }

    // Turn OVERVIEW / KEY POINTS / bullets into readable HTML.
    private String summaryToHtml(String raw) {
        if (raw == null) {
            raw = "";
        }
        String[] lines = raw.replace("\r\n", "\n").split("\n");
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:Inter, Avenir Next, Helvetica Neue, SansSerif; font-size:14pt; color:#1c1c23; margin:8px;'>");

        boolean inList = false;
        StringBuilder paragraph = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0) {
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                flushParagraph(html, paragraph);
                continue;
            }

            String upper = line.toUpperCase();
            if (upper.equals("OVERVIEW:") || upper.equals("KEY POINTS:") || upper.equals("REMEMBER:")
                    || upper.startsWith("OVERVIEW") || upper.startsWith("KEY POINTS")
                    || upper.startsWith("REMEMBER")) {
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                flushParagraph(html, paragraph);
                String title = line;
                if (title.endsWith(":")) {
                    title = title.substring(0, title.length() - 1);
                }
                html.append("<h3 style='margin:18px 0 8px 0; font-size:15pt;'>")
                        .append(escapeHtml(title)).append("</h3>");
                continue;
            }

            if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• ")) {
                flushParagraph(html, paragraph);
                if (!inList) {
                    html.append("<ul style='margin:6px 0 10px 18px; padding:0;'>");
                    inList = true;
                }
                html.append("<li style='margin:0 0 8px 0;'>")
                        .append(escapeHtml(line.substring(2).trim())).append("</li>");
                continue;
            }

            if (inList) {
                html.append("</ul>");
                inList = false;
            }
            if (paragraph.length() > 0) {
                paragraph.append(" ");
            }
            paragraph.append(line);
        }
        if (inList) {
            html.append("</ul>");
        }
        flushParagraph(html, paragraph);
        html.append("</body></html>");
        return html.toString();
    }

    private void flushParagraph(StringBuilder html, StringBuilder paragraph) {
        if (paragraph.length() == 0) {
            return;
        }
        html.append("<p style='margin:0 0 12px 0; line-height:1.45;'>")
                .append(escapeHtml(paragraph.toString())).append("</p>");
        paragraph.setLength(0);
    }

    private JPanel buildQuizPage() {
        JPanel page = new JPanel(new BorderLayout(12, 12));
        page.setOpaque(false);
        JLabel t = new JLabel("Quiz");
        t.setFont(UiKit.font(Font.BOLD, 26));

        SoftPanel card = new SoftPanel(Color.WHITE, 24);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        JPanel inner = new JPanel(new BorderLayout(10, 10));
        inner.setOpaque(false);

        quizProgress = new JLabel("Pick a document first");
        quizProgress.setForeground(UiKit.MUTED);
        quizQuestion = new JTextArea(" ");
        UiKit.styleArea(quizQuestion, 17);
        quizQuestion.setFont(UiKit.font(Font.BOLD, 17));

        JPanel answers = new JPanel(new GridLayout(4, 1, 8, 8));
        answers.setOpaque(false);
        for (int i = 0; i < 4; i++) {
            final int index = i;
            answerButtons[i] = new PillButton("Answer", false);
            answerButtons[i].setHorizontalAlignment(SwingConstants.LEFT);
            styleQuizOptionIdle(answerButtons[i], i);
            answerButtons[i].addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    checkAnswer(index);
                }
            });
            answers.add(answerButtons[i]);
        }
        quizFeedback = new JLabel(" ");
        quizFeedback.setForeground(UiKit.PILL_B);
        quizFeedback.setFont(UiKit.font(Font.BOLD, 13));

        PillButton next = new PillButton("Next question", true);
        next.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                nextQuestion();
            }
        });
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(quizFeedback, BorderLayout.CENTER);
        bottom.add(next, BorderLayout.EAST);

        inner.add(quizProgress, BorderLayout.NORTH);
        inner.add(quizQuestion, BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout(8, 8));
        south.setOpaque(false);
        south.add(answers, BorderLayout.CENTER);
        south.add(bottom, BorderLayout.SOUTH);
        inner.add(south, BorderLayout.SOUTH);
        card.add(inner, BorderLayout.CENTER);

        page.add(t, BorderLayout.NORTH);
        page.add(card, BorderLayout.CENTER);
        page.add(docActionBar(), BorderLayout.SOUTH);
        return page;
    }

    private JPanel buildCardsPage() {
        JPanel page = new JPanel(new BorderLayout(10, 10));
        page.setOpaque(false);

        JLabel t = new JLabel("Flashcards");
        t.setFont(UiKit.font(Font.BOLD, 22));

        cardProgress = new JLabel("Pick a document first");
        cardProgress.setForeground(UiKit.MUTED);
        cardProgress.setFont(UiKit.font(Font.PLAIN, 12));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(t, BorderLayout.WEST);
        head.add(cardProgress, BorderLayout.EAST);

        // Big colorful flashcard — question pastels, answer side green
        flashFace = new SoftPanel(new Color(242, 234, 255), 26) {
            public Dimension getPreferredSize() {
                return new Dimension(420, 260);
            }

            public Dimension getMaximumSize() {
                return getPreferredSize();
            }

            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };
        flashFace.setBorder(new EmptyBorder(22, 24, 20, 24));
        flashFace.setLayout(new BorderLayout(10, 10));
        flashFace.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        flashFace.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                flipCard();
            }
        });

        cardSideHint = new JLabel("QUESTION", SwingConstants.CENTER);
        cardSideHint.setFont(UiKit.font(Font.BOLD, 12));
        cardSideHint.setForeground(new Color(120, 90, 220));

        cardHowTo = new JLabel(" ", SwingConstants.CENTER);
        cardHowTo.setFont(UiKit.font(Font.PLAIN, 11));
        cardHowTo.setForeground(UiKit.MUTED);

        cardFace = new JTextArea("Open a document to practice cards.");
        UiKit.styleArea(cardFace, 17);
        cardFace.setFont(UiKit.font(Font.BOLD, 17));
        cardFace.setEditable(false);
        cardFace.setHighlighter(null);
        cardFace.setOpaque(false);
        cardFace.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                flipCard();
            }
        });

        cardFlipTip = new JLabel("Tap or Flip for the answer", SwingConstants.CENTER);
        cardFlipTip.setFont(UiKit.font(Font.PLAIN, 12));
        cardFlipTip.setForeground(new Color(120, 100, 180));

        JPanel topBits = new JPanel();
        topBits.setOpaque(false);
        topBits.setLayout(new BoxLayout(topBits, BoxLayout.Y_AXIS));
        cardSideHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardHowTo.setAlignmentX(Component.CENTER_ALIGNMENT);
        topBits.add(cardSideHint);
        topBits.add(Box.createVerticalStrut(4));
        topBits.add(cardHowTo);

        flashFace.add(topBits, BorderLayout.NORTH);
        flashFace.add(cardFace, BorderLayout.CENTER);
        flashFace.add(cardFlipTip, BorderLayout.SOUTH);

        JPanel centerWrap = new JPanel(new GridBagLayout());
        centerWrap.setOpaque(false);
        centerWrap.add(flashFace);

        PillButton flip = new PillButton("Flip", true);
        flip.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                flipCard();
            }
        });
        PillButton prev = new PillButton("Back", false);
        prev.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                prevCard();
            }
        });
        PillButton next = new PillButton("Next", false);
        next.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                nextCard();
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttons.setOpaque(false);
        buttons.add(prev);
        buttons.add(flip);
        buttons.add(next);

        JPanel mid = new JPanel(new BorderLayout(8, 12));
        mid.setOpaque(false);
        mid.add(centerWrap, BorderLayout.CENTER);
        mid.add(buttons, BorderLayout.SOUTH);

        page.add(head, BorderLayout.NORTH);
        page.add(mid, BorderLayout.CENTER);
        page.add(docActionBar(), BorderLayout.SOUTH);
        return page;
    }

    private JPanel buildPlanPage() {
        JPanel page = new JPanel(new BorderLayout(12, 12));
        page.setOpaque(false);

        JLabel t = new JLabel("Schedule");
        t.setFont(UiKit.font(Font.BOLD, 22));
        t.setForeground(UiKit.INK);

        planDocLabel = new JLabel("Today gathers all subjects · open a doc to see its full week");
        planDocLabel.setForeground(UiKit.MUTED);
        planDocLabel.setFont(UiKit.font(Font.PLAIN, 12.5f));

        JLabel hint = new JLabel("Each document has its own 7-day plan. Today = Java + Assembly + … in one list.");
        hint.setFont(UiKit.font(Font.PLAIN, 11.5f));
        hint.setForeground(UiKit.MUTED);

        PillButton editPlan = new PillButton("Edit open plan", true);
        editPlan.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                editOwnPlan();
            }
        });

        JPanel titleCol = new JPanel();
        titleCol.setOpaque(false);
        titleCol.setLayout(new BoxLayout(titleCol, BoxLayout.Y_AXIS));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        planDocLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleCol.add(t);
        titleCol.add(Box.createVerticalStrut(4));
        titleCol.add(planDocLabel);
        titleCol.add(Box.createVerticalStrut(4));
        titleCol.add(hint);

        JPanel head = new JPanel(new BorderLayout(12, 0));
        head.setOpaque(false);
        head.add(titleCol, BorderLayout.CENTER);
        head.add(editPlan, BorderLayout.EAST);

        // Scroll body: Today (all subjects) + full week for the open document
        planWeekGrid = new JPanel();
        planWeekGrid.setOpaque(false);
        planWeekGrid.setLayout(new BoxLayout(planWeekGrid, BoxLayout.Y_AXIS));

        SoftPanel listCard = new SoftPanel(Color.WHITE, 22);
        listCard.setBorder(new EmptyBorder(14, 14, 14, 14));
        JScrollPane scroll = new JScrollPane(planWeekGrid);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        listCard.add(scroll, BorderLayout.CENTER);

        page.add(head, BorderLayout.NORTH);
        page.add(listCard, BorderLayout.CENTER);
        page.add(docActionBar(), BorderLayout.SOUTH);
        refreshPlanCalendar();
        return page;
    }

    // One schedule stop: left label + purple timeline line/dot + colored card.
    private JPanel timelineRow(String leftTop, String leftBottom, String cardTitle,
                               String cardBody, int colorIndex, boolean active, boolean last) {
        final Color fill = UiKit.planCardColor(colorIndex);
        final Color lineColor = new Color(180, 160, 230);
        final Color dotColor = active ? new Color(120, 90, 220) : lineColor;

        JPanel wrap = new JPanel(new BorderLayout(10, 0));
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setPreferredSize(new Dimension(64, 40));
        JLabel lt = new JLabel(leftTop);
        lt.setFont(UiKit.font(Font.BOLD, 13f));
        lt.setForeground(active ? new Color(120, 90, 220) : UiKit.INK);
        JLabel lb = new JLabel(leftBottom == null ? " " : leftBottom);
        lb.setFont(UiKit.font(Font.PLAIN, 11f));
        lb.setForeground(UiKit.MUTED);
        lt.setAlignmentX(Component.LEFT_ALIGNMENT);
        lb.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(Box.createVerticalStrut(10));
        left.add(lt);
        left.add(lb);

        JPanel rail = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                int dotY = 22;
                g2.setStroke(new BasicStroke(2.5f));
                g2.setColor(lineColor);
                g2.drawLine(cx, 0, cx, dotY - 8);
                if (!last) {
                    g2.drawLine(cx, dotY + 8, cx, getHeight());
                }
                if (active) {
                    g2.setColor(new Color(120, 90, 220, 70));
                    g2.fillOval(cx - 9, dotY - 9, 18, 18);
                    g2.setColor(dotColor);
                    g2.fillOval(cx - 5, dotY - 5, 10, 10);
                    g2.setColor(Color.WHITE);
                    g2.fillOval(cx - 2, dotY - 2, 4, 4);
                } else {
                    g2.setColor(Color.WHITE);
                    g2.fillOval(cx - 6, dotY - 6, 12, 12);
                    g2.setColor(dotColor);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval(cx - 6, dotY - 6, 12, 12);
                }
                g2.dispose();
            }
        };
        rail.setOpaque(false);
        rail.setPreferredSize(new Dimension(24, 40));

        SoftPanel card = new SoftPanel(fill, 18);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setLayout(new BorderLayout(0, 6));

        JLabel title = new JLabel(cardTitle == null ? "" : cardTitle);
        title.setFont(UiKit.font(Font.BOLD, 14f));
        title.setForeground(UiKit.INK);

        JTextArea body = new JTextArea(cardBody == null ? "" : cardBody);
        body.setFont(UiKit.font(Font.PLAIN, 12.5f));
        body.setForeground(new Color(55, 55, 70));
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setEditable(false);
        body.setFocusable(false);
        body.setOpaque(false);
        body.setBorder(null);

        card.add(title, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        JPanel mid = new JPanel(new BorderLayout(10, 0));
        mid.setOpaque(false);
        mid.add(rail, BorderLayout.WEST);
        mid.add(card, BorderLayout.CENTER);

        wrap.add(left, BorderLayout.WEST);
        wrap.add(mid, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel planDayRow(String day, String task, int index, boolean isToday, boolean isLast) {
        String leftTop = "Day " + (index + 1);
        String leftBottom = isToday ? "today" : " ";
        String title = day;
        return timelineRow(leftTop, leftBottom, title, task, index, isToday, isLast);
    }

    private String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private JPanel docActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        PillButton sum = new PillButton("Summary", false);
        sum.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showPage("summary");
            }
        });
        PillButton q = new PillButton("Quiz", false);
        q.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showPage("quiz");
            }
        });
        PillButton c = new PillButton("Flashcards", false);
        c.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showPage("cards");
            }
        });
        PillButton p = new PillButton("Plan", false);
        p.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showPage("plan");
            }
        });
        bar.add(sum);
        bar.add(q);
        bar.add(c);
        bar.add(p);
        return bar;
    }

    // ---------- Profile / ask ----------

    private JPanel buildProfile() {
        SoftPanel side = new SoftPanel(Color.WHITE, 32);
        side.setPreferredSize(new Dimension(340, 0));
        side.setBorder(new EmptyBorder(16, 14, 14, 14));
        side.setLayout(new BorderLayout(0, 14));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        AvatarCircle avatar = new AvatarCircle("ST", true);
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        profileNameLabel = new JLabel("Student", SwingConstants.CENTER);
        profileNameLabel.setFont(UiKit.font(Font.BOLD, 18));
        profileNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        profileRoleLabel = new JLabel("CS student", SwingConstants.CENTER);
        profileRoleLabel.setForeground(UiKit.MUTED);
        profileRoleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        PillButton edit = new PillButton("Edit profile", false);
        edit.setAlignmentX(Component.CENTER_ALIGNMENT);
        edit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String name = ModalSheet.askText(StudyBuddyApp.this,
                        "Edit profile",
                        "This name shows on your profile card.",
                        store.getProfile().name);
                if (name != null) {
                    store.setProfileName(name);
                    refreshProfile();
                }
            }
        });

        top.add(avatar);
        top.add(Box.createVerticalStrut(8));
        top.add(profileNameLabel);
        top.add(profileRoleLabel);
        top.add(Box.createVerticalStrut(8));
        top.add(edit);
        top.add(Box.createVerticalStrut(14));

        SoftPanel stats = new SoftPanel(new Color(248, 248, 252), 20);
        stats.setBorder(new EmptyBorder(12, 12, 12, 12));
        stats.setLayout(new GridLayout(2, 2, 8, 8));
        stats.add(statBlock("0", "Docs quizzed", "done"));
        stats.add(statBlock("0", "Quizzes done", "quiz"));
        stats.add(statBlock("0", "Card flips", "cards"));
        stats.add(statBlock("0%", "Best score", "best"));
        top.add(stats);

        // Premium Ask assistant card
        SoftPanel ask = new SoftPanel(new Color(248, 248, 252), 24);
        ask.setBorder(new EmptyBorder(14, 14, 14, 14));
        ask.setLayout(new BorderLayout(10, 10));

        JLabel ah = new JLabel("✦  Ask assistant");
        ah.setFont(UiKit.font(Font.BOLD, 15));
        ah.setForeground(UiKit.INK);
        JLabel asub = new JLabel("Answers use the open document below");
        asub.setFont(UiKit.font(Font.PLAIN, 11));
        asub.setForeground(UiKit.MUTED);

        askContextLabel = new JLabel(" ");
        askContextLabel.setFont(UiKit.font(Font.BOLD, 11.5f));
        askContextLabel.setForeground(new Color(90, 90, 110));
        askContextLabel.setBorder(new EmptyBorder(6, 0, 0, 0));

        JPanel askHead = new JPanel();
        askHead.setOpaque(false);
        askHead.setLayout(new BoxLayout(askHead, BoxLayout.Y_AXIS));
        ah.setAlignmentX(Component.LEFT_ALIGNMENT);
        asub.setAlignmentX(Component.LEFT_ALIGNMENT);
        askContextLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        askHead.add(ah);
        askHead.add(Box.createVerticalStrut(2));
        askHead.add(asub);
        askHead.add(askContextLabel);

        askChatList = new JPanel();
        askChatList.setOpaque(false);
        askChatList.setLayout(new BoxLayout(askChatList, BoxLayout.Y_AXIS));
        askChatList.setBorder(new EmptyBorder(2, 2, 2, 6));

        SoftPanel chatCard = new SoftPanel(new Color(242, 242, 246), 18);
        chatCard.setBorder(new EmptyBorder(8, 8, 8, 8));
        askChatScroll = new JScrollPane(askChatList);
        askChatScroll.setBorder(null);
        askChatScroll.setOpaque(false);
        askChatScroll.getViewport().setOpaque(false);
        askChatScroll.getVerticalScrollBar().setUnitIncrement(14);
        askChatScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        askChatScroll.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        chatCard.add(askChatScroll, BorderLayout.CENTER);

        // First bubble after scroll exists so width can be measured.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                addAskBubble("Open a document, then ask about that material.", false);
            }
        });

        askField = new RoundTextField();
        ((RoundTextField) askField).arc = 20;
        askField.putClientProperty("JTextField.placeholderText", "Ask anything…");

        askSendBtn = new PillButton("Send", true);
        askSendBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendAsk();
            }
        });
        askField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendAsk();
            }
        });

        SoftPanel inputShell = new SoftPanel(Color.WHITE, 22);
        inputShell.setBorder(new EmptyBorder(6, 6, 6, 6));
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(askField, BorderLayout.CENTER);
        row.add(askSendBtn, BorderLayout.EAST);
        inputShell.add(row, BorderLayout.CENTER);

        ask.add(askHead, BorderLayout.NORTH);
        ask.add(chatCard, BorderLayout.CENTER);
        ask.add(inputShell, BorderLayout.SOUTH);

        side.add(top, BorderLayout.NORTH);
        side.add(ask, BorderLayout.CENTER);
        return side;
    }

    private JPanel statBlock(String big, String small, String which) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel b = new JLabel(big);
        b.setFont(UiKit.font(Font.BOLD, 20));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel s = new JLabel(small);
        s.setFont(UiKit.font(Font.PLAIN, 11));
        s.setForeground(UiKit.MUTED);
        s.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(b);
        p.add(s);
        if (which.equals("done")) {
            statDoneNum = b;
        } else if (which.equals("quiz")) {
            statQuizNum = b;
        } else if (which.equals("cards")) {
            statCardsNum = b;
        } else {
            statBestNum = b;
        }
        return p;
    }

    // ---------- Navigation / refresh ----------

    private void showPage(String id) {
        if (id.equals("courses")) {
            categoryFilter = "All Courses";
            refreshOverview();
            pages.show(pageHost, "overview");
            setNavActive("courses");
            updateContextBar("Courses");
            return;
        }
        if (id.equals("overview")) {
            refreshOverview();
            pages.show(pageHost, "overview");
            setNavActive("overview");
            updateContextBar("Overview");
            return;
        }
        if (id.equals("docs")) {
            refreshAllDocs();
            pages.show(pageHost, "docs");
            setNavActive("docs");
            updateContextBar("My docs");
            return;
        }
        if (id.equals("plan")) {
            // Schedule shows Today across all subjects even with no doc open.
            refreshPlanCalendar();
            pages.show(pageHost, "plan");
            setNavActive("plan");
            updateContextBar("Schedule");
            return;
        }
        if (id.equals("quiz") || id.equals("cards") || id.equals("summary")) {
            if (selectedDoc == null) {
                statusLabel.setText("Open a course and pick a document first.");
                ModalSheet.alert(this, "Open a document first",
                        "Quiz, flashcards, and the summary belong to one document.<br><br>"
                                + "1) Overview → open a subject<br>"
                                + "2) Open or add a document<br>"
                                + "3) Then use Quiz / Flashcards / Summary");
                showPage("overview");
                return;
            }
            loadDocIntoViews();
            pages.show(pageHost, id);
            setNavActive(id.equals("summary") ? "docs" : id);
            if (id.equals("quiz")) {
                updateContextBar("Quiz");
            } else if (id.equals("cards")) {
                updateContextBar("Flashcards");
            } else {
                updateContextBar("Summary");
            }
            return;
        }
        if (id.equals("course")) {
            refreshCoursePage();
            pages.show(pageHost, "course");
            setNavActive("courses");
            updateContextBar("Subject");
            return;
        }
        pages.show(pageHost, id);
        setNavActive(id);
        updateContextBar(id);
    }

    private void setNavActive(String id) {
        for (int i = 0; i < navIds.length; i++) {
            navButtons[i].setActive(navIds[i].equals(id));
        }
    }

    private void refreshAll() {
        refreshOverview();
        refreshCoursePage();
        refreshProfile();
        loadDocIntoViews();
        refreshAskContext();
        UserProfile p = store.getProfile();
        welcomeLabel.setText("Hey " + p.name + ", welcome back");
        if (ai.isDemoMode()) {
            statusLabel.setText("API key missing — add api.key in config.properties.");
        } else {
            statusLabel.setText("API connected. Add subjects and import your PDFs.");
        }
    }

    // Shows which document Ask will use for study answers.
    private void refreshAskContext() {
        if (askContextLabel == null) {
            return;
        }
        if (selectedDoc != null && selectedCourse != null) {
            askContextLabel.setText("Using: " + selectedCourse.name + " › " + selectedDoc.title);
            askContextLabel.setForeground(new Color(70, 70, 95));
        } else if (selectedCourse != null) {
            askContextLabel.setText("Subject: " + selectedCourse.name + " · open a document first");
            askContextLabel.setForeground(UiKit.MUTED);
        } else {
            askContextLabel.setText("No document open · pick one under a subject");
            askContextLabel.setForeground(UiKit.MUTED);
        }
    }

    private void refreshProfile() {
        UserProfile p = store.getProfile();
        profileNameLabel.setText(p.name);
        profileRoleLabel.setText(p.role);
        refreshProfileStats();
        welcomeLabel.setText("Hey " + p.name + ", welcome back");
    }

    private void refreshOverview() {
        overviewGrid.removeAll();
        ArrayList<Course> list = store.getCourses();
        int shown = 0;
        for (int i = 0; i < list.size(); i++) {
            Course c = list.get(i);
            if (!categoryFilter.equals("All Courses") && !c.category.equals(categoryFilter)) {
                continue;
            }
            overviewGrid.add(courseCard(c, i));
            shown = shown + 1;
        }
        if (shown == 0) {
            JLabel empty = new JLabel("No subjects yet. Click + Add subject.");
            empty.setForeground(UiKit.MUTED);
            overviewGrid.add(empty);
        }
        markCategoryChips();
        overviewGrid.revalidate();
        overviewGrid.repaint();
    }

    private void markCategoryChips() {
        // find chips panel from overview page
        for (int i = 0; i < pageHost.getComponentCount(); i++) {
            markChipsRecursive(pageHost.getComponent(i));
        }
    }

    private void markChipsRecursive(Component c) {
        if (c instanceof CategoryChip) {
            CategoryChip chip = (CategoryChip) c;
            chip.setSelected(chip.getText().equals(categoryFilter));
        } else if (c instanceof Container) {
            Component[] kids = ((Container) c).getComponents();
            for (int i = 0; i < kids.length; i++) {
                markChipsRecursive(kids[i]);
            }
        }
    }

    private JPanel courseCard(final Course course, int colorIndex) {
        final int cardW = 200;
        final int cardH = 230;

        SoftPanel card = new SoftPanel(Color.WHITE, 20) {
            public Dimension getPreferredSize() {
                return new Dimension(cardW, cardH);
            }

            public Dimension getMinimumSize() {
                return getPreferredSize();
            }

            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setLayout(new BorderLayout(6, 6));

        JPanel thumb = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, UiKit.tagColor(colorIndex),
                        getWidth(), getHeight(), UiKit.tagColor(colorIndex + 2)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(255, 255, 255, 70));
                g2.fillOval(getWidth() / 3, -20, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(UiKit.font(Font.BOLD, 36));
                String letter = course.name.substring(0, 1).toUpperCase();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(letter, (getWidth() - fm.stringWidth(letter)) / 2,
                        (getHeight() + fm.getAscent()) / 2 - 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        thumb.setOpaque(false);
        thumb.setPreferredSize(new Dimension(180, 120));

        JLabel tag = new JLabel(shortCategory(course.category));
        tag.setOpaque(true);
        tag.setBackground(new Color(255, 255, 255, 230));
        tag.setBorder(new EmptyBorder(3, 8, 3, 8));
        tag.setFont(UiKit.font(Font.BOLD, 10));
        JPanel tagRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tagRow.setOpaque(false);
        tagRow.add(tag);
        thumb.add(tagRow, BorderLayout.NORTH);

        JLabel title = new JLabel("<html><body style='width:150px'><b>" + course.name + "</b></body></html>");
        title.setFont(UiKit.font(Font.PLAIN, 13));
        JLabel meta = new JLabel(course.docCount() + " docs  ·  ★ ready");
        meta.setForeground(UiKit.MUTED);
        meta.setFont(UiKit.font(Font.PLAIN, 11));

        JPanel text = new JPanel(new BorderLayout(0, 4));
        text.setOpaque(false);
        text.add(title, BorderLayout.NORTH);
        text.add(meta, BorderLayout.SOUTH);

        card.add(thumb, BorderLayout.CENTER);
        card.add(text, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                selectedCourse = course;
                if (course.docs.size() > 0) {
                    selectedDoc = course.docs.get(0);
                } else {
                    selectedDoc = null;
                }
                showPage("course");
            }
        });
        return card;
    }

    private String shortCategory(String category) {
        if (category == null) {
            return "Course";
        }
        if (category.length() > 16) {
            return category.substring(0, 14) + "…";
        }
        return category;
    }

    private void refreshCoursePage() {
        if (courseTitleLabel == null) {
            return;
        }
        if (selectedCourse == null) {
            courseTitleLabel.setText("No course selected");
            if (courseHintLabel != null) {
                courseHintLabel.setText("Pick a subject from Overview first.");
            }
            courseDocsPanel.removeAll();
            return;
        }
        courseTitleLabel.setText(selectedCourse.name);
        if (courseHintLabel != null) {
            int n = selectedCourse.docs.size();
            courseHintLabel.setText(n == 0
                    ? "No documents yet — scan a PDF or image to add one."
                    : n + (n == 1 ? " document" : " documents") + " in this subject.");
        }
        courseDocsPanel.removeAll();
        for (int i = 0; i < selectedCourse.docs.size(); i++) {
            courseDocsPanel.add(docRow(selectedCourse, selectedCourse.docs.get(i)));
            courseDocsPanel.add(Box.createVerticalStrut(10));
        }
        if (selectedCourse.docs.size() == 0) {
            SoftPanel emptyCard = new SoftPanel(Color.WHITE, 18);
            emptyCard.setBorder(new EmptyBorder(20, 20, 20, 20));
            emptyCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
            JLabel empty = new JLabel("<html><b>Nothing here yet</b><br>"
                    + "Use Scan PDF or Scan image above to import notes.</html>");
            empty.setFont(UiKit.font(Font.PLAIN, 13f));
            empty.setForeground(UiKit.MUTED);
            emptyCard.add(empty, BorderLayout.CENTER);
            courseDocsPanel.add(emptyCard);
        }
        courseDocsPanel.revalidate();
        courseDocsPanel.repaint();
    }

    private void refreshAllDocs() {
        Component[] comps = pageHost.getComponents();
        JPanel list = null;
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] instanceof JPanel) {
                Object prop = ((JPanel) comps[i]).getClientProperty("list");
                if (prop instanceof JPanel) {
                    list = (JPanel) prop;
                }
            }
        }
        if (list == null) {
            return;
        }
        list.removeAll();
        ArrayList<Course> courses = store.getCourses();
        for (int i = 0; i < courses.size(); i++) {
            Course c = courses.get(i);
            for (int j = 0; j < c.docs.size(); j++) {
                list.add(docRow(c, c.docs.get(j)));
                list.add(Box.createVerticalStrut(10));
            }
        }
        list.revalidate();
        list.repaint();
    }

    private JPanel docRow(final Course course, final StudyDoc doc) {
        SoftPanel row = new SoftPanel(Color.WHITE, 18);
        row.setBorder(new EmptyBorder(12, 14, 12, 14));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        row.setPreferredSize(new Dimension(400, 72));
        row.setLayout(new BorderLayout(10, 0));

        JLabel title = new JLabel("<html><b>" + doc.title + "</b><br>"
                + course.name + " · " + doc.questionCount() + " quiz · "
                + doc.cardCount() + " cards · best " + doc.bestQuizScore
                + "</html>");
        title.setFont(UiKit.font(Font.PLAIN, 13));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        PillButton open = new PillButton("Open", true);
        open.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedCourse = course;
                selectedDoc = doc;
                showPage("summary");
            }
        });
        PillButton quiz = new PillButton("Quiz", false);
        quiz.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedCourse = course;
                selectedDoc = doc;
                showPage("quiz");
            }
        });
        PillButton cards = new PillButton("Cards", false);
        cards.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedCourse = course;
                selectedDoc = doc;
                showPage("cards");
            }
        });
        buttons.add(cards);
        buttons.add(quiz);
        buttons.add(open);

        row.add(title, BorderLayout.CENTER);
        row.add(buttons, BorderLayout.EAST);
        return row;
    }

    private void loadDocIntoViews() {
        if (selectedDoc == null || selectedDoc.pack == null) {
            return;
        }
        summaryPane.setText(summaryToHtml(selectedDoc.pack.summary));
        summaryPane.setCaretPosition(0);
        refreshPlanCalendar();
        quizIndex = 0;
        quizScore = 0;
        answered = false;
        quizCountedThisRun = false;
        showQuestion();
        cardIndex = 0;
        showingBack = false;
        showCard();
        refreshProfileStats();
    }

    // Monday = Day 1 … Sunday = Day 7 in each document plan.
    private int todayPlanIndex() {
        int dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        if (dow == Calendar.SUNDAY) {
            return 6;
        }
        return dow - Calendar.MONDAY;
    }

    private String todayWeekdayName() {
        String[] names = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        return names[todayPlanIndex()];
    }

    private void refreshPlanCalendar() {
        if (planWeekGrid == null) {
            return;
        }
        planWeekGrid.removeAll();
        int today = todayPlanIndex();

        // ---- TODAY timeline (all subjects) ----
        JLabel todayTitle = new JLabel("Today · " + todayWeekdayName());
        todayTitle.setFont(UiKit.font(Font.BOLD, 16f));
        todayTitle.setForeground(UiKit.INK);
        todayTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel todaySub = new JLabel("Timeline across subjects · plan day " + (today + 1));
        todaySub.setFont(UiKit.font(Font.PLAIN, 12f));
        todaySub.setForeground(UiKit.MUTED);
        todaySub.setAlignmentX(Component.LEFT_ALIGNMENT);
        planWeekGrid.add(todayTitle);
        planWeekGrid.add(Box.createVerticalStrut(2));
        planWeekGrid.add(todaySub);
        planWeekGrid.add(Box.createVerticalStrut(12));

        ArrayList<String[]> todayItems = new ArrayList<String[]>();
        ArrayList<Course> courses = store.getCourses();
        for (int i = 0; i < courses.size(); i++) {
            Course c = courses.get(i);
            for (int j = 0; j < c.docs.size(); j++) {
                StudyDoc d = c.docs.get(j);
                if (d.pack == null || d.pack.plan == null || d.pack.plan.length == 0) {
                    continue;
                }
                String task = "Review";
                if (today < d.pack.plan.length && d.pack.plan[today].task != null) {
                    task = d.pack.plan[today].task;
                }
                todayItems.add(new String[] {c.name, d.title, task});
            }
        }
        if (todayItems.size() == 0) {
            JLabel empty = new JLabel("No plans yet — import a PDF under a subject to build a week plan.");
            empty.setFont(UiKit.font(Font.PLAIN, 12.5f));
            empty.setForeground(UiKit.MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            planWeekGrid.add(empty);
        } else {
            for (int i = 0; i < todayItems.size(); i++) {
                String[] item = todayItems.get(i);
                boolean last = i == todayItems.size() - 1;
                planWeekGrid.add(timelineRow(
                        item[0],
                        "doc",
                        item[1],
                        item[2],
                        i,
                        i == 0,
                        last));
                if (!last) {
                    planWeekGrid.add(Box.createVerticalStrut(4));
                }
            }
        }

        planWeekGrid.add(Box.createVerticalStrut(20));

        // ---- FULL WEEK timeline for the open document ----
        JLabel weekTitle = new JLabel("This week · open document");
        weekTitle.setFont(UiKit.font(Font.BOLD, 15f));
        weekTitle.setForeground(UiKit.INK);
        weekTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        planWeekGrid.add(weekTitle);
        planWeekGrid.add(Box.createVerticalStrut(4));

        if (selectedDoc == null || selectedDoc.pack == null) {
            planDocLabel.setText("Today = all subjects · open a document to see its full 7-day timeline");
            JLabel tip = new JLabel("<html>Open any document to see its complete week on the timeline below.<br>"
                    + "Edit plan only changes that open document.</html>");
            tip.setFont(UiKit.font(Font.PLAIN, 12.5f));
            tip.setForeground(UiKit.MUTED);
            tip.setAlignmentX(Component.LEFT_ALIGNMENT);
            planWeekGrid.add(Box.createVerticalStrut(6));
            planWeekGrid.add(tip);
        } else {
            String courseBit = selectedCourse == null ? "" : selectedCourse.name + " › ";
            planDocLabel.setText("Open week: " + courseBit + selectedDoc.title);
            JLabel openHint = new JLabel(courseBit + selectedDoc.title);
            openHint.setFont(UiKit.font(Font.PLAIN, 12.5f));
            openHint.setForeground(UiKit.MUTED);
            openHint.setAlignmentX(Component.LEFT_ALIGNMENT);
            planWeekGrid.add(openHint);
            planWeekGrid.add(Box.createVerticalStrut(10));

            PlanItem[] plan = selectedDoc.pack.plan;
            for (int i = 0; i < 7; i++) {
                String day = "Day " + (i + 1);
                String task = "Rest / review";
                if (i < plan.length) {
                    day = plan[i].day;
                    task = plan[i].task;
                }
                boolean isToday = i == today;
                boolean isLast = i == 6;
                planWeekGrid.add(planDayRow(day, task, i, isToday, isLast));
                if (!isLast) {
                    planWeekGrid.add(Box.createVerticalStrut(4));
                }
            }
        }

        planWeekGrid.revalidate();
        planWeekGrid.repaint();
    }

    // Student can rewrite the 7 days; AI only suggests the first version.
    private void editOwnPlan() {
        if (selectedDoc == null || selectedDoc.pack == null) {
            ModalSheet.alert(this, "No document open",
                    "Open a document first, then you can edit its 7-day plan.");
            return;
        }
        PlanItem[] old = selectedDoc.pack.plan;
        SoftField[] dayFields = new SoftField[7];
        SoftArea[] taskFields = new SoftArea[7];

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        for (int i = 0; i < 7; i++) {
            String day = "Day " + (i + 1);
            String task = "";
            if (i < old.length) {
                day = old[i].day;
                task = old[i].task;
            }
            dayFields[i] = new SoftField(day);
            taskFields[i] = new SoftArea(task);

            SoftPanel card = new SoftPanel(UiKit.planCardColor(i), 16);
            card.setBorder(new EmptyBorder(10, 12, 10, 12));
            card.setLayout(new BorderLayout(8, 8));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

            JLabel dayTag = new JLabel("Day " + (i + 1));
            dayTag.setFont(UiKit.font(Font.BOLD, 11f));
            dayTag.setForeground(UiKit.tagColor(i));

            JPanel top = new JPanel(new BorderLayout(8, 0));
            top.setOpaque(false);
            top.add(dayTag, BorderLayout.WEST);
            top.add(dayFields[i], BorderLayout.CENTER);

            card.add(top, BorderLayout.NORTH);
            card.add(taskFields[i], BorderLayout.CENTER);
            form.add(card);
            if (i < 6) {
                form.add(Box.createVerticalStrut(8));
            }
        }

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(400, 360));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        ModalSheet modal = new ModalSheet(this, "Edit 7-day plan",
                "Update each day. Scroll to see all seven — window stays on screen.",
                "Save plan");
        modal.setFixedSize(460, 560);
        modal.setBody(scroll);
        if (!modal.showModal()) {
            return;
        }
        PlanItem[] next = new PlanItem[7];
        for (int i = 0; i < 7; i++) {
            String d = dayFields[i].getText().trim();
            String t = taskFields[i].getText().trim();
            if (d.length() == 0) {
                d = "Day " + (i + 1);
            }
            if (t.length() == 0) {
                t = "Review";
            }
            next[i] = new PlanItem(d, t);
        }
        selectedDoc.pack.plan = next;
        store.saveAll();
        refreshPlanCalendar();
        statusLabel.setText("Your plan was saved.");
    }

    // ---------- Actions ----------

    private void addSubjectDialog() {
        SoftField name = new SoftField();
        final String[] selectedCat = {"IT Courses"};

        JLabel nameLbl = new JLabel("Subject name");
        nameLbl.setFont(UiKit.font(Font.BOLD, 12f));
        nameLbl.setForeground(UiKit.MUTED);
        JLabel catLbl = new JLabel("Category");
        catLbl.setFont(UiKit.font(Font.BOLD, 12f));
        catLbl.setForeground(UiKit.MUTED);

        // Big tap targets instead of the tiny native combo arrow.
        final ChoicePill itPill = new ChoicePill("IT Courses");
        final ChoicePill otherPill = new ChoicePill("Other");
        itPill.setSelected(true);
        itPill.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedCat[0] = "IT Courses";
                itPill.setSelected(true);
                otherPill.setSelected(false);
            }
        });
        otherPill.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedCat[0] = "Other";
                otherPill.setSelected(true);
                itPill.setSelected(false);
            }
        });

        JPanel catRow = new JPanel(new GridLayout(1, 2, 10, 0));
        catRow.setOpaque(false);
        catRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        catRow.add(itPill);
        catRow.add(otherPill);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        catLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        catRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        name.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        form.add(nameLbl);
        form.add(Box.createVerticalStrut(6));
        form.add(name);
        form.add(Box.createVerticalStrut(14));
        form.add(catLbl);
        form.add(Box.createVerticalStrut(8));
        form.add(catRow);

        ModalSheet modal = new ModalSheet(this, "Add subject",
                "Create a folder for PDFs and slides. Each import becomes its own document.",
                "Create subject");
        modal.setBody(form);
        if (modal.showModal() && name.getText().trim().length() > 0) {
            Course c = store.addCourse(name.getText().trim(), selectedCat[0]);
            selectedCourse = c;
            selectedDoc = null;
            refreshAll();
            showPage("course");
        }
    }

    // Native macOS / system Open dialog (Finder-style). JFileChooser looks weird here.
    private File pickStudyFile(boolean pdf) {
        FileDialog dialog = new FileDialog(this,
                pdf ? "Choose a PDF to scan" : "Choose an image to scan",
                FileDialog.LOAD);
        if (pdf) {
            dialog.setFilenameFilter(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".pdf");
                }
            });
        } else {
            dialog.setFilenameFilter(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    String n = name.toLowerCase();
                    return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
                            || n.endsWith(".gif") || n.endsWith(".webp");
                }
            });
        }
        File home = new File(System.getProperty("user.home"));
        File docs = new File(home, "Documents");
        if (docs.isDirectory()) {
            dialog.setDirectory(docs.getAbsolutePath());
        } else if (home.isDirectory()) {
            dialog.setDirectory(home.getAbsolutePath());
        }
        dialog.setVisible(true);
        String dir = dialog.getDirectory();
        String name = dialog.getFile();
        if (dir == null || name == null) {
            return null;
        }
        return new File(dir, name);
    }

    private void importIntoSelected(boolean pdf) {
        if (selectedCourse == null) {
            ModalSheet.alert(this, "Pick a subject first",
                    "Open a subject from Overview, then scan a PDF or image into it.");
            return;
        }

        final File file = pickStudyFile(pdf);
        if (file == null) {
            return;
        }
        if (pdf && !FileImport.isPdf(file)) {
            ModalSheet.alert(this, "Wrong file type", "Please choose a PDF file.");
            return;
        }
        if (!pdf && !FileImport.isImage(file)) {
            ModalSheet.alert(this, "Wrong file type", "Please choose an image (PNG or JPG).");
            return;
        }

        final WorkingSheet working = new WorkingSheet(this, "Building study pack", file.getName());
        working.setStatus(pdf ? "Reading PDF text" : "Preparing image");
        working.openSheet();
        statusLabel.setText("Building study pack from " + file.getName() + "…");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<StudyDoc, String>() {
            protected StudyDoc doInBackground() throws Exception {
                StudyPack pack;
                if (FileImport.isPdf(file)) {
                    publish("Reading PDF text");
                    String text = FileImport.readPdfText(file);
                    if (text.length() < 20) {
                        throw new Exception("Could not read text from that PDF.");
                    }
                    publish("Sending notes to AI (summary + quiz + cards)");
                    pack = ai.buildFromText(file.getName(), text);
                } else {
                    publish("Encoding image");
                    String mime = FileImport.imageMime(file);
                    String b64 = FileImport.imageBase64(file);
                    publish("Sending image to AI (summary + quiz + cards)");
                    pack = ai.buildFromImage(file.getName(), mime, b64);
                }
                publish("Saving document under " + selectedCourse.name);
                String title = file.getName();
                String id = LibraryStore.safeId(selectedCourse.id + "-" + title + "-"
                        + (selectedCourse.docs.size() + 1));
                return new StudyDoc(id, title, file.getName(), pack);
            }

            protected void process(java.util.List<String> chunks) {
                if (chunks.size() > 0) {
                    working.setStatus(chunks.get(chunks.size() - 1));
                }
            }

            protected void done() {
                working.closeSheet();
                setCursor(Cursor.getDefaultCursor());
                try {
                    StudyDoc doc = get();
                    store.addDoc(selectedCourse, doc);
                    selectedDoc = doc;
                    refreshAll();
                    showPage("summary");
                    statusLabel.setText("Added " + doc.title + " to " + selectedCourse.name);
                } catch (Exception ex) {
                    statusLabel.setText("Import failed.");
                    ModalSheet.alert(StudyBuddyApp.this, "Import failed",
                            ex.getMessage() == null ? ex.toString() : ex.getMessage());
                }
            }
        }.execute();
    }

    private void showQuestion() {
        if (selectedDoc == null) {
            return;
        }
        QuizQuestion[] quiz = selectedDoc.pack.quiz;
        if (quizIndex >= quiz.length) {
            quizProgress.setText("Finished. Score: " + quizScore + " / " + quiz.length);
            quizQuestion.setText("Quiz done. Next restarts.");
            // Stats are recorded when the last question is answered (see checkAnswer).
            finishQuizIfNeeded();
            for (int i = 0; i < 4; i++) {
                answerButtons[i].setEnabled(false);
            }
            return;
        }
        answered = false;
        QuizQuestion q = quiz[quizIndex];
        currentCorrect = q.correct;
        quizProgress.setText(selectedDoc.title + " · Q " + (quizIndex + 1) + "/" + quiz.length
                + " · Score " + quizScore);
        quizQuestion.setText(q.question);
        quizFeedback.setText(" ");
        ArrayList<String> opts = new ArrayList<String>();
        opts.add(q.correct);
        for (int i = 0; i < q.wrongs.length; i++) {
            opts.add(q.wrongs[i]);
        }
        while (opts.size() < 4) {
            opts.add("None of these");
        }
        Collections.shuffle(opts);
        shuffledAnswers = opts.toArray(new String[0]);
        for (int i = 0; i < 4; i++) {
            answerButtons[i].setEnabled(true);
            answerButtons[i].setText(shuffledAnswers[i]);
            styleQuizOptionIdle(answerButtons[i], i);
        }
    }

    private void styleQuizOptionIdle(PillButton btn, int index) {
        Color fill = UiKit.planCardColor(index);
        Color border = UiKit.tagColor(index);
        btn.setColorOverride(fill, UiKit.INK, border);
    }

    private void styleQuizOptionCorrect(PillButton btn) {
        btn.setColorOverride(new Color(200, 240, 215), new Color(15, 110, 70),
                new Color(40, 170, 110));
    }

    private void styleQuizOptionWrong(PillButton btn) {
        btn.setColorOverride(new Color(255, 228, 228), new Color(150, 40, 40),
                new Color(220, 90, 90));
    }

    private void styleQuizOptionMuted(PillButton btn) {
        btn.setColorOverride(new Color(240, 240, 244), new Color(140, 140, 150),
                new Color(220, 220, 228));
    }

    private void checkAnswer(int index) {
        if (selectedDoc == null || answered || quizIndex >= selectedDoc.pack.quiz.length) {
            return;
        }
        answered = true;
        for (int i = 0; i < 4; i++) {
            answerButtons[i].setEnabled(false);
        }
        boolean ok = shuffledAnswers[index].equals(currentCorrect);
        // Color every option: green = correct, red = wrong pick, grey = others
        for (int i = 0; i < 4; i++) {
            if (shuffledAnswers[i].equals(currentCorrect)) {
                styleQuizOptionCorrect(answerButtons[i]);
            } else if (i == index) {
                styleQuizOptionWrong(answerButtons[i]);
            } else {
                styleQuizOptionMuted(answerButtons[i]);
            }
        }
        if (ok) {
            quizScore = quizScore + 1;
            quizFeedback.setText("Correct.");
            quizFeedback.setForeground(new Color(30, 150, 100));
        } else {
            quizFeedback.setText("Correct answer is highlighted in green.");
            quizFeedback.setForeground(new Color(200, 80, 80));
        }

        // Count stats as soon as the last question is answered (not after an extra Next).
        if (quizIndex >= selectedDoc.pack.quiz.length - 1) {
            finishQuizIfNeeded();
            quizFeedback.setText(quizFeedback.getText() + "  ·  Stats updated.");
        }
    }

    private void finishQuizIfNeeded() {
        if (selectedDoc == null || quizCountedThisRun) {
            return;
        }
        quizCountedThisRun = true;
        store.recordQuiz(selectedDoc, quizScore, selectedDoc.pack.quiz.length);
        refreshProfileStats();
    }

    private void nextQuestion() {
        if (selectedDoc == null) {
            return;
        }
        if (quizIndex >= selectedDoc.pack.quiz.length) {
            quizIndex = 0;
            quizScore = 0;
            quizCountedThisRun = false;
        } else {
            quizIndex = quizIndex + 1;
        }
        showQuestion();
    }

    private void showCard() {
        if (selectedDoc == null || cardFace == null) {
            return;
        }
        FlashCard[] cards = selectedDoc.pack.cards;
        if (cards.length == 0) {
            return;
        }
        if (cardIndex >= cards.length) {
            cardIndex = 0;
        }
        if (cardIndex < 0) {
            cardIndex = cards.length - 1;
        }
        FlashCard c = cards[cardIndex];
        String where = pathLabel();
        cardProgress.setText((cardIndex + 1) + " / " + cards.length + "  ·  " + where);
        if (showingBack) {
            // Answer side — green
            cardSideHint.setText("ANSWER");
            cardSideHint.setForeground(new Color(20, 130, 90));
            cardHowTo.setText(" ");
            cardFace.setText(c.back);
            cardFace.setFont(UiKit.font(Font.BOLD, 16));
            cardFace.setForeground(new Color(20, 90, 60));
            flashFace.fill = new Color(220, 245, 230);
            if (cardFlipTip != null) {
                cardFlipTip.setText("Tap or Flip to see the question again");
                cardFlipTip.setForeground(new Color(40, 140, 100));
            }
        } else {
            // Question side — soft color that changes per card
            Color pastel = UiKit.planCardColor(cardIndex);
            Color accent = UiKit.tagColor(cardIndex);
            cardSideHint.setText("QUESTION");
            cardSideHint.setForeground(accent);
            cardHowTo.setText(" ");
            cardFace.setText(promptForCard(c.front));
            cardFace.setFont(UiKit.font(Font.BOLD, 17));
            cardFace.setForeground(UiKit.INK);
            flashFace.fill = pastel;
            if (cardFlipTip != null) {
                cardFlipTip.setText("Tap or Flip for the answer");
                cardFlipTip.setForeground(accent);
            }
        }
        flashFace.repaint();
        updateContextBar("Flashcards");
    }

    // Old packs sometimes only saved a word like "Method" — turn that into a clear prompt.
    private String promptForCard(String front) {
        if (front == null || front.trim().length() == 0) {
            return "What is this term?";
        }
        String f = front.trim();
        if (f.endsWith("?")) {
            return f;
        }
        if (f.toLowerCase().startsWith("what ") || f.toLowerCase().startsWith("how ")
                || f.toLowerCase().startsWith("why ") || f.toLowerCase().startsWith("explain")) {
            return f;
        }
        return "What is \"" + f + "\"?";
    }

    private String pathLabel() {
        String course = selectedCourse == null ? "?" : selectedCourse.name;
        String doc = selectedDoc == null ? "?" : selectedDoc.title;
        return course + " › " + doc;
    }

    private void updateContextBar(String pageName) {
        if (contextBar == null) {
            return;
        }
        if (selectedCourse == null) {
            contextBar.setText(pageName + " · pick a subject on Overview, then open a document");
        } else if (selectedDoc == null) {
            contextBar.setText(pageName + " · " + selectedCourse.name + " · add or open a document first");
        } else {
            contextBar.setText(pageName + " · " + pathLabel()
                    + "  (quiz / cards / plan belong to this document)");
        }
        refreshAskContext();
    }

    private void flipCard() {
        if (selectedDoc == null) {
            return;
        }
        if (!showingBack) {
            store.recordCardFlip();
            selectedDoc.cardsReviewed = selectedDoc.cardsReviewed + 1;
            refreshProfileStats();
        }
        showingBack = !showingBack;
        showCard();
    }

    private void nextCard() {
        if (selectedDoc == null) {
            return;
        }
        cardIndex = cardIndex + 1;
        showingBack = false;
        showCard();
    }

    private void prevCard() {
        if (selectedDoc == null) {
            return;
        }
        cardIndex = cardIndex - 1;
        showingBack = false;
        showCard();
    }

    private void refreshProfileStats() {
        UserProfile p = store.getProfile();
        if (statDoneNum != null) {
            statDoneNum.setText(String.valueOf(p.coursesCompleted));
        }
        if (statQuizNum != null) {
            statQuizNum.setText(String.valueOf(p.quizzesTaken));
        }
        if (statCardsNum != null) {
            statCardsNum.setText(String.valueOf(p.cardsFlipped));
        }
        if (statBestNum != null) {
            statBestNum.setText(p.bestPercent + "%");
        }
    }

    private void sendAsk() {
        final String q = askField.getText().trim();
        if (q.length() == 0) {
            return;
        }
        if (thinkingRow != null) {
            return; // already waiting on a reply
        }
        askField.setText("");
        addAskBubble(q, true);
        showThinking();
        final StudyPack pack = selectedDoc == null ? null : selectedDoc.pack;
        final String openCourse = selectedCourse == null ? "" : selectedCourse.name;
        final String allSubjects = subjectNamesList();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        if (askSendBtn != null) {
            askSendBtn.setEnabled(false);
        }
        askField.setEnabled(false);
        if (statusLabel != null) {
            statusLabel.setText("Assistant is writing a reply…");
        }

        new SwingWorker<String, Void>() {
            protected String doInBackground() throws Exception {
                return ai.ask(q, pack, openCourse, allSubjects);
            }

            protected void done() {
                hideThinking();
                setCursor(Cursor.getDefaultCursor());
                if (askSendBtn != null) {
                    askSendBtn.setEnabled(true);
                }
                askField.setEnabled(true);
                askField.requestFocusInWindow();
                try {
                    addAskBubble(get(), false);
                    if (statusLabel != null) {
                        statusLabel.setText("API connected. Ask another question anytime.");
                    }
                } catch (Exception ex) {
                    addAskBubble("Error: " + ex.getMessage(), false);
                    if (statusLabel != null) {
                        statusLabel.setText("Ask failed — try again.");
                    }
                }
            }
        }.execute();
    }

    private void showThinking() {
        hideThinking();
        thinkingDots = 0;
        thinkingLabel = new JLabel("Thinking");
        thinkingLabel.setFont(UiKit.font(Font.PLAIN, 12f));
        thinkingLabel.setForeground(UiKit.MUTED);
        thinkingLabel.setBorder(new EmptyBorder(8, 12, 8, 12));

        // Use ChatBubble sizing rules so Thinking never stretches full-width and clips.
        SoftPanel bubble = new SoftPanel(new Color(236, 236, 242), 16);
        bubble.setBorder(new EmptyBorder(2, 2, 2, 2));
        bubble.add(thinkingLabel, BorderLayout.CENTER);
        bubble.setPreferredSize(new Dimension(110, 38));
        bubble.setMaximumSize(new Dimension(110, 38));
        bubble.setMinimumSize(new Dimension(110, 38));

        thinkingRow = new JPanel();
        thinkingRow.setOpaque(false);
        thinkingRow.setLayout(new BoxLayout(thinkingRow, BoxLayout.X_AXIS));
        thinkingRow.add(bubble);
        thinkingRow.add(Box.createHorizontalGlue());
        thinkingRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        thinkingRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        askChatList.add(thinkingRow);
        askChatList.add(Box.createVerticalStrut(4));
        askChatList.revalidate();
        askChatList.repaint();
        scrollAskToBottom();

        thinkingTimer = new javax.swing.Timer(450, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                thinkingDots = (thinkingDots + 1) % 4;
                String dots = "";
                for (int i = 0; i < thinkingDots; i++) {
                    dots = dots + ".";
                }
                if (thinkingLabel != null) {
                    thinkingLabel.setText("Thinking" + dots);
                }
            }
        });
        thinkingTimer.start();
    }

    private void hideThinking() {
        if (thinkingTimer != null) {
            thinkingTimer.stop();
            thinkingTimer = null;
        }
        if (thinkingRow != null && askChatList != null) {
            // remove thinking row and the strut after it if present
            int idx = -1;
            Component[] kids = askChatList.getComponents();
            for (int i = 0; i < kids.length; i++) {
                if (kids[i] == thinkingRow) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                askChatList.remove(idx);
                if (idx < askChatList.getComponentCount()) {
                    Component next = askChatList.getComponent(idx);
                    if (next instanceof Box.Filler) {
                        askChatList.remove(idx);
                    }
                }
            }
            thinkingRow = null;
            thinkingLabel = null;
            askChatList.revalidate();
            askChatList.repaint();
        }
    }

    private void scrollAskToBottom() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (askChatScroll != null) {
                    JScrollBar bar = askChatScroll.getVerticalScrollBar();
                    bar.setValue(bar.getMaximum());
                }
            }
        });
    }

    private String subjectNamesList() {
        ArrayList<Course> courses = store.getCourses();
        if (courses == null || courses.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < courses.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(courses.get(i).name);
        }
        return sb.toString();
    }

    private int askChatWidth() {
        // Leave room for scrollbar + SoftPanel inset so bubbles never clip on the right.
        if (askChatScroll != null) {
            int w = askChatScroll.getViewport().getExtentSize().width;
            if (w < 40) {
                w = askChatScroll.getWidth();
            }
            if (w < 40 && askChatScroll.getParent() != null) {
                w = askChatScroll.getParent().getWidth() - 24;
            }
            if (w >= 80) {
                return Math.max(100, w - 28);
            }
        }
        return 200;
    }

    private void addAskBubble(String text, boolean mine) {
        if (askChatList == null) {
            return;
        }
        ChatBubble bubble = new ChatBubble(text, mine, askChatWidth());
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        if (mine) {
            row.add(Box.createHorizontalGlue());
            row.add(bubble);
        } else {
            row.add(bubble);
            row.add(Box.createHorizontalGlue());
        }
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        int h = bubble.getPreferredSize().height + 4;
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        // Do not force row width = viewport — that made content wider than the clip area.
        askChatList.add(row);
        askChatList.add(Box.createVerticalStrut(4));
        askChatList.revalidate();
        askChatList.repaint();
        scrollAskToBottom();
    }

    private JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        return sp;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                }
                UiKit.applyAppFont();
                new StudyBuddyApp().setVisible(true);
            }
        });
    }
}
