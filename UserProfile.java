// Simple profile + overall stats for the right panel.
class UserProfile {
    String name;
    String role;
    int coursesCompleted; // docs with a quiz attempt
    int quizzesTaken;
    int cardsFlipped;
    int bestPercent;

    public UserProfile() {
        name = "Student";
        role = "Java beginner";
        coursesCompleted = 0;
        quizzesTaken = 0;
        cardsFlipped = 0;
        bestPercent = 0;
    }
}
