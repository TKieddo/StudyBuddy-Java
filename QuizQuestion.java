// One multiple-choice quiz question.
class QuizQuestion {
    String question;
    String correct;
    String[] wrongs;

    public QuizQuestion(String question, String correct, String[] wrongs) {
        this.question = question;
        this.correct = correct;
        this.wrongs = wrongs;
    }
}
