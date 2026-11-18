// Everything the AI builds from one PDF or image.
class StudyPack {
    String sourceName;
    String summary;
    QuizQuestion[] quiz;
    FlashCard[] cards;
    PlanItem[] plan;

    public StudyPack(String sourceName, String summary, QuizQuestion[] quiz,
                     FlashCard[] cards, PlanItem[] plan) {
        this.sourceName = sourceName;
        this.summary = summary;
        this.quiz = quiz;
        this.cards = cards;
        this.plan = plan;
    }
}
