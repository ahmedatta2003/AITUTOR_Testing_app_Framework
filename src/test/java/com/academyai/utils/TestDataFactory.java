package com.academyai.utils;

import com.github.javafaker.Faker;

import java.util.Locale;
import java.util.Random;

/**
 * Generates realistic fake data for AcademyAI test scenarios.
 */
public class TestDataFactory {

    private static final Faker faker = new Faker(new Locale("en"));
    private static final Random random = new Random();

    private TestDataFactory() {}

    // ── Student Registration ──────────────────────────────

    public static String randomStudentEmail() {
        return "student." + faker.internet().uuid().substring(0, 8)
                + "@academyai.edu.eg";
    }

    public static String randomName() {
        return faker.name().fullName();
    }

    public static String randomPassword() {
        // At least 8 chars, uppercase, digit
        return "Test@" + faker.number().digits(4) + "Aa";
    }

    public static String randomUniversityId() {
        return "STU-" + faker.number().digits(6);
    }

    public static String randomFacultyName() {
        String[] faculties = {
            "Faculty of Computer Science",
            "Faculty of Engineering",
            "Faculty of Business Administration",
            "Faculty of Science",
            "Faculty of Arts"
        };
        return faculties[random.nextInt(faculties.length)];
    }

    public static int randomAcademicYear() {
        return random.nextInt(4) + 1; // 1–4
    }

    // ── AI Chat Messages ──────────────────────────────────

    public static String randomAcademicQuestion() {
        String[] questions = {
            "Can you explain the concept of Big O notation in algorithms?",
            "What is the difference between supervised and unsupervised learning?",
            "Explain the OSI model layers with examples.",
            "What are SOLID principles in software engineering?",
            "How does a relational database differ from a NoSQL database?",
            "What is the difference between process and thread?",
            "Explain polymorphism in object-oriented programming.",
            "What is REST API and how does it work?",
            "Describe the software development life cycle (SDLC).",
            "What are design patterns? Give examples."
        };
        return questions[random.nextInt(questions.length)];
    }

    public static String randomInvalidAiQuery() {
        String[] invalidQueries = {
            "",
            "   ",
            "a".repeat(10001), // exceeds max length
            "<script>alert('xss')</script>",
            "SELECT * FROM users; DROP TABLE users;--"
        };
        return invalidQueries[random.nextInt(invalidQueries.length)];
    }

    // ── Social Post ───────────────────────────────────────

    public static String randomPostContent() {
        return faker.lorem().sentence(random.nextInt(15) + 5);
    }

    public static String randomHashtag() {
        String[] tags = {
            "#ComputerScience", "#SoftwareTesting", "#University",
            "#AcademyAI", "#ElShorouk", "#StudyTips", "#Algorithms"
        };
        return tags[random.nextInt(tags.length)];
    }

    public static String randomPostTitle() {
        return faker.lorem().words(random.nextInt(5) + 2)
                .toString()
                .replace("[", "")
                .replace("]", "")
                .trim();
    }

    // ── Chat Messages ─────────────────────────────────────

    public static String randomChatMessage() {
        String[] messages = {
            "Hey! Did you understand today's lecture?",
            "Can we study together for the exam?",
            "What time is the Software Testing class tomorrow?",
            "I found a great resource for data structures!",
            "Did you finish the assignment?",
            "Professor postponed the quiz to next week.",
            "Group study session at 8 PM?",
            "Can you share your notes from yesterday?"
        };
        return messages[random.nextInt(messages.length)];
    }

    // ── Lecture Schedule ──────────────────────────────────

    public static String randomCourseName() {
        String[] courses = {
            "Software Engineering",
            "Data Structures & Algorithms",
            "Database Systems",
            "Computer Networks",
            "Operating Systems",
            "Software Testing & QA",
            "Artificial Intelligence",
            "Mobile Application Development",
            "Web Technologies",
            "Cybersecurity Fundamentals"
        };
        return courses[random.nextInt(courses.length)];
    }

    public static String randomCourseCode() {
        return "CS" + faker.number().digits(3);
    }

    public static String randomRoom() {
        return "Hall-" + (char)('A' + random.nextInt(5)) + random.nextInt(9);
    }

    public static String randomDayOfWeek() {
        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday"};
        return days[random.nextInt(days.length)];
    }

    public static String randomLectureTime() {
        int[] hours = {8, 10, 12, 14, 16};
        int hour = hours[random.nextInt(hours.length)];
        return String.format("%02d:00", hour);
    }

    // ── Notification ──────────────────────────────────────

    public static String randomNotificationTitle() {
        String[] titles = {
            "New Assignment Posted",
            "Lecture Schedule Updated",
            "Quiz Reminder",
            "Grade Published",
            "Peer Message Received"
        };
        return titles[random.nextInt(titles.length)];
    }
}
