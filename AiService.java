import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Properties;

/*
    Talks to an OpenAI-style chat API.
    Put your key in config.properties (copy from config.properties.example).
    If there is no key, imports and Ask show an error (no fake sample content).
*/
class AiService {
    String apiKey;
    String baseUrl;
    String model;
    boolean demoMode;
    HttpClient client;
    String configPathUsed;

    String visionModel;

    public AiService() {
        apiKey = "";
        // Token Plan Singapore (OpenAI-compatible)
        baseUrl = "https://token-plan.ap-southeast-1.maas.aliyuncs.com/compatible-mode/v1";
        model = "qwen4-max";
        visionModel = "qwen4-max";
        demoMode = true;
        configPathUsed = "";
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        loadConfig();
    }

    // Look in several places so the app still finds the key
    // even if it was not started from the StudyBuddy folder.
    private File findConfigFile() {
        String[] candidates = new String[] {
                "config.properties",
                System.getProperty("user.dir") + File.separator + "config.properties",
                System.getProperty("user.dir") + File.separator + "StudyBuddy" + File.separator + "config.properties",
                "/Users/tsebisotuke/Documents/JAVA Class/StudyBuddy/config.properties"
        };
        for (int i = 0; i < candidates.length; i++) {
            File f = new File(candidates[i]);
            if (f.isFile()) {
                return f;
            }
        }
        return null;
    }

    private boolean looksLikePlaceholder(String key) {
        if (key.length() == 0) {
            return true;
        }
        String upper = key.toUpperCase();
        return upper.contains("PASTE_YOUR") || upper.contains("YOUR_KEY_HERE");
    }

    private void loadConfig() {
        File file = findConfigFile();
        if (file == null) {
            demoMode = true;
            configPathUsed = "(not found)";
            System.out.println("StudyBuddy: config.properties not found — demo mode.");
            return;
        }
        try {
            Properties props = new Properties();
            FileInputStream in = new FileInputStream(file);
            props.load(in);
            in.close();
            configPathUsed = file.getAbsolutePath();

            String key = props.getProperty("api.key", "").trim();
            String url = props.getProperty("api.baseUrl", baseUrl).trim();
            String m = props.getProperty("api.model", model).trim();
            String vm = props.getProperty("api.visionModel", visionModel).trim();

            if (url.length() > 0) {
                baseUrl = url;
            }
            if (m.length() > 0) {
                model = m;
            }
            if (vm.length() > 0) {
                visionModel = vm;
            }
            if (!looksLikePlaceholder(key)) {
                if (key.startsWith("\"") && key.endsWith("\"") && key.length() > 2) {
                    key = key.substring(1, key.length() - 1).trim();
                }
                if (key.toLowerCase().startsWith("bearer ")) {
                    key = key.substring(7).trim();
                }
                apiKey = key;
                demoMode = false;

                // sk-sp- keys must use Token Plan URL (dashscope-intl → 401)
                if (apiKey.startsWith("sk-sp-") && !baseUrl.contains("token-plan")) {
                    baseUrl = "https://token-plan.ap-southeast-1.maas.aliyuncs.com/compatible-mode/v1";
                }
                System.out.println("StudyBuddy: API ready (" + model + ") from " + configPathUsed);
            } else {
                demoMode = true;
                System.out.println("StudyBuddy: no real api.key in " + configPathUsed + " — demo mode.");
            }
        } catch (IOException e) {
            demoMode = true;
            System.out.println("StudyBuddy: could not read config — demo mode. " + e.getMessage());
        }
    }

    public boolean isDemoMode() {
        return demoMode;
    }

    private void requireApi() throws IOException {
        if (demoMode) {
            throw new IOException(
                    "No API key loaded.\n"
                            + "Add api.key in config.properties and restart.\n"
                            + "Looked for: " + configPathUsed);
        }
    }

    public StudyPack buildFromText(String sourceName, String text) throws Exception {
        requireApi();
        String prompt = studyPrompt() + "\n\nCOURSE TEXT:\n" + text;
        String reply = chatText(prompt);
        return parsePack(sourceName, reply);
    }

    public StudyPack buildFromImage(String sourceName, String mime, String base64)
            throws Exception {
        requireApi();
        String prompt = studyPrompt()
                + "\n\nThe image is a course slide or notes page. Read it and build the study pack.";
        String reply = chatWithImage(prompt, mime, base64);
        return parsePack(sourceName, reply);
    }

    public String ask(String question, StudyPack pack, String openCourse, String allSubjects) throws Exception {
        requireApi();

        StringBuilder ctx = new StringBuilder();
        ctx.append("You are a friendly study assistant. The student may study MANY subjects "
                + "(not only Java).\n");
        if (allSubjects != null && allSubjects.length() > 0) {
            ctx.append("Their subjects include: ").append(allSubjects).append(".\n");
        }
        ctx.append("FORMAT YOUR REPLY LIKE THIS (important):\n");
        ctx.append("- Use short paragraphs with a blank line between them\n");
        ctx.append("- Use bullet points with '- ' when listing ideas\n");
        ctx.append("- Do NOT dump one long dense paragraph\n");
        ctx.append("- Do NOT use markdown # headings or **bold**\n");
        ctx.append("- Keep it easy to scan on a phone-sized chat bubble\n");
        ctx.append("- For greetings (hi/hello/hey): 1-2 short friendly lines only. "
                + "Do NOT mention Java or any subject unless they ask. "
                + "Do NOT say you are ready to dig into a topic.\n");
        ctx.append("- Only talk about a subject when the student asks about it "
                + "or clearly wants study help.\n\n");
        if (pack != null) {
            ctx.append("A document is selected");
            if (openCourse != null && openCourse.length() > 0) {
                ctx.append(" under subject \"").append(openCourse).append("\"");
            }
            ctx.append(" (use this ONLY for study questions, not for greetings):\n");
            ctx.append("Source: ").append(pack.sourceName).append("\n\n");
            ctx.append("SUMMARY:\n").append(pack.summary).append("\n\n");
        } else {
            ctx.append("No document is open yet. If they want study help, "
                    + "ask which subject to open — do not assume Java.\n\n");
        }
        ctx.append("STUDENT:\n").append(question);

        return tidyReply(chatText(ctx.toString()));
    }

    // Make replies easier to read in the chat bubble.
    private String tidyReply(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace("\r\n", "\n").trim();
        s = s.replace("**", "");
        s = s.replaceAll("(?m)^#+\\s*", "");
        s = s.replaceAll("(?m)^\\*\\s+", "- ");
        s = s.replaceAll("(?m)^•\\s+", "- ");
        // blank line before a bullet block if missing
        s = s.replaceAll("([^\\n])\\n(- )", "$1\n\n$2");
        // collapse 3+ newlines to 2
        s = s.replaceAll("\\n{3,}", "\n\n");
        return s.trim();
    }

    private String studyPrompt() {
        return "You are helping a first-year Java student. From the material, create a study pack.\n"
                + "Reply in EXACTLY this format (no markdown):\n"
                + "===SUMMARY===\n"
                + "OVERVIEW:\n"
                + "(one short paragraph, 3-5 sentences)\n"
                + "\n"
                + "KEY POINTS:\n"
                + "- point one\n"
                + "- point two\n"
                + "- point three\n"
                + "- point four\n"
                + "- point five\n"
                + "\n"
                + "REMEMBER:\n"
                + "- one important tip\n"
                + "- another tip\n"
                + "(use blank lines between sections; use - for bullets)\n"
                + "===QUIZ===\n"
                + "Q: question text\n"
                + "A: correct answer\n"
                + "W: wrong answer\n"
                + "W: wrong answer\n"
                + "W: wrong answer\n"
                + "(Write AT LEAST 6 quiz questions — more is fine if the material has more ideas)\n"
                + "===CARDS===\n"
                + "F: a full question, e.g. What is a method in Java?\n"
                + "B: short clear answer\n"
                + "(Write AT LEAST 6 flashcards — more is fine; never put only one word on the front)\n"
                + "===PLAN===\n"
                + "D: Day 1 | what to practice\n"
                + "D: Day 2 | what to practice\n"
                + "(7 days)\n"
                + "Keep language simple. Focus on the course ideas in the material.\n"
                + "Minimum: 6 quiz questions and 6 cards. Add extra when the notes cover more topics.";
    }

    private String chatText(String userText) throws Exception {
        String body = "{"
                + "\"model\":\"" + jsonEscape(model) + "\","
                + "\"messages\":[{"
                + "\"role\":\"user\","
                + "\"content\":\"" + jsonEscape(userText) + "\""
                + "}]"
                + "}";
        return postChat(body);
    }

    private String chatWithImage(String userText, String mime, String base64) throws Exception {
        String dataUrl = "data:" + mime + ";base64," + base64;
        String body = "{"
                + "\"model\":\"" + jsonEscape(visionModel) + "\","
                + "\"messages\":[{"
                + "\"role\":\"user\","
                + "\"content\":["
                + "{\"type\":\"text\",\"text\":\"" + jsonEscape(userText) + "\"},"
                + "{\"type\":\"image_url\",\"image_url\":{\"url\":\"" + jsonEscape(dataUrl) + "\"}}"
                + "]"
                + "}]"
                + "}";
        return postChat(body);
    }

    private String postChat(String jsonBody) throws Exception {
        String url = baseUrl;
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        url = url + "/chat/completions";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(180))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String body = response.body();
            if (response.statusCode() == 401) {
                throw new IOException(
                        "API key rejected (401).\n"
                                + "Use the sk-pp- key from My Subscriptions with\n"
                                + "dashscope-intl compatible-mode URL.");
            }
            if (response.statusCode() == 404
                    || (body != null && body.contains("model_not_found"))) {
                throw new IOException(
                        "Model not found: " + model + "\n"
                                + "Pick a model from your Pro Plan list in config.properties\n"
                                + "(try qwen-plus or qwen-max-latest).");
            }
            throw new IOException("API error " + response.statusCode() + ": " + shortMsg(body));
        }
        return extractAssistantText(response.body());
    }

    // Very small JSON helper: find "content":"..." in the first message.
    private String extractAssistantText(String json) throws IOException {
        String marker = "\"content\":";
        int start = json.indexOf(marker);
        if (start < 0) {
            throw new IOException("Could not read AI reply.");
        }
        start = start + marker.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\n')) {
            start = start + 1;
        }
        if (start >= json.length() || json.charAt(start) != '"') {
            throw new IOException("Could not read AI reply text.");
        }
        start = start + 1;
        StringBuilder out = new StringBuilder();
        boolean escape = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                if (c == 'n') {
                    out.append('\n');
                } else if (c == 't') {
                    out.append('\t');
                } else if (c == 'r') {
                    out.append('\r');
                } else if (c == '"') {
                    out.append('"');
                } else if (c == '\\') {
                    out.append('\\');
                } else {
                    out.append(c);
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                out.append(c);
            }
        }
        return out.toString().trim();
    }

    private String jsonEscape(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                b.append("\\\\");
            } else if (c == '"') {
                b.append("\\\"");
            } else if (c == '\n') {
                b.append("\\n");
            } else if (c == '\r') {
                b.append("\\r");
            } else if (c == '\t') {
                b.append("\\t");
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private String shortMsg(String body) {
        if (body == null) {
            return "";
        }
        if (body.length() > 300) {
            return body.substring(0, 300) + "...";
        }
        return body;
    }

    StudyPack parsePack(String sourceName, String reply) {
        String summary = section(reply, "===SUMMARY===", "===QUIZ===");
        String quizBlock = section(reply, "===QUIZ===", "===CARDS===");
        String cardBlock = section(reply, "===CARDS===", "===PLAN===");
        String planBlock = section(reply, "===PLAN===", null);

        if (summary.length() == 0) {
            summary = reply;
        }

        QuizQuestion[] quiz = parseQuiz(quizBlock);
        FlashCard[] cards = parseCards(cardBlock);
        PlanItem[] plan = parsePlan(planBlock);

        return new StudyPack(sourceName, summary.trim(), quiz, cards, plan);
    }

    private String section(String text, String startTag, String endTag) {
        int start = text.indexOf(startTag);
        if (start < 0) {
            return "";
        }
        start = start + startTag.length();
        int end;
        if (endTag == null) {
            end = text.length();
        } else {
            end = text.indexOf(endTag, start);
            if (end < 0) {
                end = text.length();
            }
        }
        return text.substring(start, end).trim();
    }

    private QuizQuestion[] parseQuiz(String block) {
        ArrayList<QuizQuestion> list = new ArrayList<QuizQuestion>();
        String[] lines = block.split("\n");
        String q = "";
        String a = "";
        ArrayList<String> wrongs = new ArrayList<String>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("Q:")) {
                if (q.length() > 0 && a.length() > 0) {
                    list.add(new QuizQuestion(q, a, wrongs.toArray(new String[0])));
                }
                q = line.substring(2).trim();
                a = "";
                wrongs = new ArrayList<String>();
            } else if (line.startsWith("A:")) {
                a = line.substring(2).trim();
            } else if (line.startsWith("W:")) {
                wrongs.add(line.substring(2).trim());
            }
        }
        if (q.length() > 0 && a.length() > 0) {
            list.add(new QuizQuestion(q, a, wrongs.toArray(new String[0])));
        }
        if (list.size() == 0) {
            list.add(new QuizQuestion("No quiz was parsed. Try again with a clearer PDF.",
                    "Try import again", new String[] {"Skip", "Ignore", "Delete"}));
        }
        return list.toArray(new QuizQuestion[0]);
    }

    private FlashCard[] parseCards(String block) {
        ArrayList<FlashCard> list = new ArrayList<FlashCard>();
        String[] lines = block.split("\n");
        String front = "";
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("F:")) {
                front = line.substring(2).trim();
            } else if (line.startsWith("B:") && front.length() > 0) {
                list.add(new FlashCard(front, line.substring(2).trim()));
                front = "";
            }
        }
        if (list.size() == 0) {
            list.add(new FlashCard("No cards parsed", "Import again or check the AI reply format"));
        }
        return list.toArray(new FlashCard[0]);
    }

    private PlanItem[] parsePlan(String block) {
        ArrayList<PlanItem> list = new ArrayList<PlanItem>();
        String[] lines = block.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("D:")) {
                String rest = line.substring(2).trim();
                int bar = rest.indexOf("|");
                if (bar >= 0) {
                    list.add(new PlanItem(rest.substring(0, bar).trim(), rest.substring(bar + 1).trim()));
                } else {
                    list.add(new PlanItem("Day", rest));
                }
            }
        }
        if (list.size() == 0) {
            list.add(new PlanItem("Day 1", "Read the summary"));
            list.add(new PlanItem("Day 2", "Do the quiz"));
            list.add(new PlanItem("Day 3", "Practice flashcards"));
        }
        return list.toArray(new PlanItem[0]);
    }

}
