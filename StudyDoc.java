// One PDF or image under a course, plus the AI study pack and scores.
class StudyDoc {
    String id;
    String title;
    String sourceName;
    StudyPack pack;
    int bestQuizScore;
    int quizAttempts;
    int cardsReviewed;

    public StudyDoc(String id, String title, String sourceName, StudyPack pack) {
        this.id = id;
        this.title = title;
        this.sourceName = sourceName;
        this.pack = pack;
        this.bestQuizScore = 0;
        this.quizAttempts = 0;
        this.cardsReviewed = 0;
    }

    public int questionCount() {
        if (pack == null || pack.quiz == null) {
            return 0;
        }
        return pack.quiz.length;
    }

    public int cardCount() {
        if (pack == null || pack.cards == null) {
            return 0;
        }
        return pack.cards.length;
    }
}
