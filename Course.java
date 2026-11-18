import java.util.ArrayList;

// One subject / course, e.g. Java or Assembly Language.
class Course {
    String id;
    String name;
    String category;
    ArrayList<StudyDoc> docs;

    public Course(String id, String name, String category) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.docs = new ArrayList<StudyDoc>();
    }

    public int docCount() {
        return docs.size();
    }
}
