import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;

/*
    Saves courses and docs in the data folder so they stay after you close the app.
    Format is plain text so it is easy to open and check.
*/
class LibraryStore {
    static final String ROOT = "data";

    ArrayList<Course> courses = new ArrayList<Course>();
    UserProfile profile = new UserProfile();
    AiService ai;

    public LibraryStore(AiService ai) {
        this.ai = ai;
        loadAll();
        // Empty library is fine — student adds subjects and imports files.
    }

    public ArrayList<Course> getCourses() {
        return courses;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public Course findCourse(String id) {
        for (int i = 0; i < courses.size(); i++) {
            if (courses.get(i).id.equals(id)) {
                return courses.get(i);
            }
        }
        return null;
    }

    public Course addCourse(String name, String category) {
        String id = safeId(name);
        // make unique
        String base = id;
        int n = 2;
        while (findCourse(id) != null) {
            id = base + n;
            n = n + 1;
        }
        Course c = new Course(id, name, category);
        courses.add(c);
        saveAll();
        return c;
    }

    public void addDoc(Course course, StudyDoc doc) {
        course.docs.add(doc);
        saveAll();
        refreshProfileTotals();
    }

    public void recordQuiz(StudyDoc doc, int score, int total) {
        doc.quizAttempts = doc.quizAttempts + 1;
        if (score > doc.bestQuizScore) {
            doc.bestQuizScore = score;
        }
        profile.quizzesTaken = profile.quizzesTaken + 1;
        int percent = 0;
        if (total > 0) {
            percent = (score * 100) / total;
        }
        if (percent > profile.bestPercent) {
            profile.bestPercent = percent;
        }
        refreshProfileTotals();
        saveAll();
    }

    public void recordCardFlip() {
        profile.cardsFlipped = profile.cardsFlipped + 1;
        saveProfile();
    }

    public void setProfileName(String name) {
        profile.name = name;
        saveProfile();
    }

    private void refreshProfileTotals() {
        int done = 0;
        for (int i = 0; i < courses.size(); i++) {
            Course c = courses.get(i);
            for (int j = 0; j < c.docs.size(); j++) {
                if (c.docs.get(j).quizAttempts > 0) {
                    done = done + 1;
                }
            }
        }
        profile.coursesCompleted = done;
    }

    public void saveAll() {
        try {
            Files.createDirectories(Paths.get(ROOT));
            saveProfile();
            PrintWriter list = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(ROOT + "/courses.list"), StandardCharsets.UTF_8));
            for (int i = 0; i < courses.size(); i++) {
                Course c = courses.get(i);
                list.println(c.id + "|" + c.name + "|" + c.category);
                Path dir = Paths.get(ROOT, "courses", c.id);
                Files.createDirectories(dir);
                for (int j = 0; j < c.docs.size(); j++) {
                    saveDoc(c, c.docs.get(j));
                }
            }
            list.close();
        } catch (IOException e) {
            System.out.println("Save failed: " + e.getMessage());
        }
    }

    private void saveDoc(Course course, StudyDoc doc) throws IOException {
        Path file = Paths.get(ROOT, "courses", course.id, doc.id + ".pack");
        PrintWriter out = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file.toFile()), StandardCharsets.UTF_8));
        out.println("TITLE|" + doc.title);
        out.println("SOURCE|" + doc.sourceName);
        out.println("BEST|" + doc.bestQuizScore);
        out.println("ATTEMPTS|" + doc.quizAttempts);
        out.println("CARDS|" + doc.cardsReviewed);
        out.println("===SUMMARY===");
        out.println(doc.pack.summary);
        out.println("===QUIZ===");
        for (int i = 0; i < doc.pack.quiz.length; i++) {
            QuizQuestion q = doc.pack.quiz[i];
            out.println("Q: " + q.question);
            out.println("A: " + q.correct);
            for (int w = 0; w < q.wrongs.length; w++) {
                out.println("W: " + q.wrongs[w]);
            }
        }
        out.println("===CARDS===");
        for (int i = 0; i < doc.pack.cards.length; i++) {
            out.println("F: " + doc.pack.cards[i].front);
            out.println("B: " + doc.pack.cards[i].back);
        }
        out.println("===PLAN===");
        for (int i = 0; i < doc.pack.plan.length; i++) {
            out.println("D: " + doc.pack.plan[i].day + " | " + doc.pack.plan[i].task);
        }
        out.close();
    }

    private void saveProfile() {
        try {
            Files.createDirectories(Paths.get(ROOT));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(ROOT + "/profile.txt"), StandardCharsets.UTF_8));
            out.println("name=" + profile.name);
            out.println("role=" + profile.role);
            out.println("quizzesTaken=" + profile.quizzesTaken);
            out.println("cardsFlipped=" + profile.cardsFlipped);
            out.println("bestPercent=" + profile.bestPercent);
            out.println("coursesCompleted=" + profile.coursesCompleted);
            out.close();
        } catch (IOException e) {
            System.out.println("Profile save failed: " + e.getMessage());
        }
    }

    private void loadAll() {
        courses = new ArrayList<Course>();
        loadProfile();
        File listFile = new File(ROOT + "/courses.list");
        if (!listFile.exists()) {
            return;
        }
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(listFile), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|", 3);
                if (p.length >= 3) {
                    Course c = new Course(p[0], p[1], p[2]);
                    loadDocs(c);
                    courses.add(c);
                }
            }
            br.close();
            refreshProfileTotals();
        } catch (IOException e) {
            System.out.println("Load failed: " + e.getMessage());
        }
    }

    private void loadDocs(Course course) {
        File dir = new File(ROOT + "/courses/" + course.id);
        if (!dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().endsWith(".pack")) {
                StudyDoc doc = readDoc(files[i]);
                if (doc != null) {
                    course.docs.add(doc);
                }
            }
        }
    }

    private StudyDoc readDoc(File file) {
        try {
            String raw = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            String title = meta(raw, "TITLE|");
            String source = meta(raw, "SOURCE|");
            int best = parseInt(meta(raw, "BEST|"), 0);
            int attempts = parseInt(meta(raw, "ATTEMPTS|"), 0);
            int cards = parseInt(meta(raw, "CARDS|"), 0);
            String id = file.getName().replace(".pack", "");
            StudyPack pack = ai.parsePack(source, raw);
            StudyDoc doc = new StudyDoc(id, title.length() > 0 ? title : id, source, pack);
            doc.bestQuizScore = best;
            doc.quizAttempts = attempts;
            doc.cardsReviewed = cards;
            return doc;
        } catch (Exception e) {
            return null;
        }
    }

    private String meta(String raw, String key) {
        String[] lines = raw.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith(key)) {
                return lines[i].substring(key.length()).trim();
            }
        }
        return "";
    }

    private int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private void loadProfile() {
        profile = new UserProfile();
        File f = new File(ROOT + "/profile.txt");
        if (!f.exists()) {
            return;
        }
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(f), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                int eq = line.indexOf('=');
                if (eq < 0) {
                    continue;
                }
                String k = line.substring(0, eq);
                String v = line.substring(eq + 1);
                if (k.equals("name")) {
                    profile.name = v;
                } else if (k.equals("role")) {
                    profile.role = v;
                } else if (k.equals("quizzesTaken")) {
                    profile.quizzesTaken = parseInt(v, 0);
                } else if (k.equals("cardsFlipped")) {
                    profile.cardsFlipped = parseInt(v, 0);
                } else if (k.equals("bestPercent")) {
                    profile.bestPercent = parseInt(v, 0);
                } else if (k.equals("coursesCompleted")) {
                    profile.coursesCompleted = parseInt(v, 0);
                }
            }
            br.close();
        } catch (IOException e) {
            // keep defaults
        }
    }

    static String safeId(String name) {
        String s = name.trim().toLowerCase();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                b.append(c);
            } else if (c == ' ' || c == '-' || c == '_') {
                b.append('-');
            }
        }
        if (b.length() == 0) {
            return "course";
        }
        return b.toString();
    }
}
