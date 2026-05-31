# AcademyAI – API Automation Framework

## Overview

**AcademyAI** هو تطبيق Flutter أكاديمي شامل موجه لطلبة الجامعات (أكاديمية الشروق)، يتضمن:
- 🤖 **AI Academic Assistant** – شات ذكاء اصطناعي للمساعدة الأكاديمية
- 💬 **Student Chat** – تواصل مباشر وجماعي بين الطلبة
- 📅 **Lecture Schedule** – جدول المحاضرات الأسبوعي
- 📱 **Social Feed** – تغذية اجتماعية أكاديمية
- 🔐 **Auth** – تسجيل، تسجيل دخول، إدارة الملف الشخصي

هذا الـ Framework يغطي **API Automation** كامل لكل هذه الوحدات.

---

## Stack

| Tool | Purpose |
|---|---|
| **Rest Assured 5.4** | HTTP Client & API assertions |
| **TestNG 7.9** | Test runner, groups, parallel execution |
| **Allure 2.25** | Rich HTML test reports |
| **Maven** | Build, dependency management, profiles |
| **Java Faker** | Realistic test data generation |
| **AssertJ** | Fluent, readable assertions |
| **Owner** | Config management via properties files |
| **Lombok** | Reduce boilerplate in models |

---

## Project Structure

```
AcademyAI_Testing_Framework/
├── pom.xml
├── run.sh                              ← CLI runner (Linux/Mac)
│
└── src/test/
    ├── java/com/academyai/
    │   ├── api/
    │   │   ├── auth/
    │   │   │   └── AuthTests.java          (14 tests)
    │   │   ├── ai/
    │   │   │   └── AiChatTests.java        (15 tests)
    │   │   ├── chat/
    │   │   │   └── StudentChatTests.java   (23 tests)
    │   │   ├── social/
    │   │   │   └── SocialFeedTests.java    (21 tests)
    │   │   └── schedule/
    │   │       └── LectureScheduleTests.java (18 tests)
    │   ├── performance/
    │   │   └── PerformanceTests.java       (10 tests)
    │   ├── config/
    │   │   ├── FrameworkConfig.java
    │   │   └── ConfigManager.java
    │   ├── utils/
    │   │   ├── BaseApiTest.java
    │   │   └── TestDataFactory.java
    │   ├── models/
    │   │   └── ApiModels.java
    │   └── listeners/
    │       └── AllureTestListener.java
    │
    └── resources/
        ├── config.properties
        ├── testng-suite.xml            ← Full regression
        ├── testng-smoke.xml            ← Smoke only
        └── allure/
            └── categories.json
```

**Total: 101 test cases** across 6 test classes

---

## APIs Tested

### 🔐 Auth (`/api/v1/auth`)
| # | Test | Method | Endpoint |
|---|------|--------|----------|
| 1 | Register new student | POST | `/auth/register` |
| 2 | Register duplicate email → 409 | POST | `/auth/register` |
| 3 | Register missing fields → 400 | POST | `/auth/register` |
| 4 | Register invalid email → 422 | POST | `/auth/register` |
| 5 | Login valid → JWT token | POST | `/auth/login` |
| 6 | Login wrong password → 401 | POST | `/auth/login` |
| 7 | Login non-existent → 404 | POST | `/auth/login` |
| 8 | Login empty body → 400 | POST | `/auth/login` |
| 9 | GET profile → 200 | GET | `/auth/profile` |
| 10 | GET profile no token → 401 | GET | `/auth/profile` |
| 11 | GET profile invalid token → 401 | GET | `/auth/profile` |
| 12 | Update profile → 200 | PUT | `/auth/profile` |
| 13 | Refresh token → new JWT | POST | `/auth/refresh` |
| 14 | Logout → token invalidated | POST | `/auth/logout` |

### 🤖 AI Chat (`/api/v1/ai`)
| # | Test | Validates |
|---|------|-----------|
| 1 | Valid academic question → 200 + reply | Functionality |
| 2 | Data Structures response → relevant keywords | **AI Quality** |
| 3 | Arabic language → Arabic chars in response | **AI Quality** |
| 4 | Clear question → confidence ≥ 0.5 | **AI Quality** |
| 5 | Follow-up in same session → context maintained | Multi-turn |
| 6 | Session history → messages list | Session |
| 7 | All sessions list | Session |
| 8 | Non-existent session → 404 | Edge Case |
| 9 | Empty message → 400 | Validation |
| 10 | 5001 chars → 422 | Validation |
| 11 | Prompt injection → safe response | **Security** |
| 12 | XSS payload → not reflected | **Security** |
| 13 | No auth → 401 | Auth |
| 14 | Simple query → response < 10s | **Performance** |
| 15 | Delete session → 200 + 404 after | Lifecycle |

### 💬 Student Chat (`/api/v1/chat`)
| # | Test | Validates |
|---|------|-----------|
| 1 | Get conversations → 200 | Smoke |
| 2 | Create direct conversation → 201 | Functionality |
| 3 | Get conversation by ID | Functionality |
| 4 | Non-existent conversation → 404 | Edge Case |
| 5 | Create conversation with self → 400 | Validation |
| 6 | Send text message → 201 | **Core** |
| 7 | Get message history (paginated) | Pagination |
| 8 | Mark message read → is_read=true | Status |
| 9 | Mark all read | Status |
| 10 | Delete own message | Lifecycle |
| 11 | Empty message → 400 | Validation |
| 12 | 2001 chars → 422 | Validation |
| 13 | Send to foreign conversation → 403 | **Security** |
| 14 | No auth → 401 | Auth |
| 15 | Create group chat → 201 | Group |
| 16 | Send to group → 201 | Group |
| 17 | Add member to group | Group |
| 18 | Remove member from group | Group |
| 19 | Group with 1 member → 400 | Validation |
| 20 | Search students by name | Search |
| 21 | Empty search → 400 | Validation |
| 22 | Typing indicator | Real-time |
| 23 | Unread count | Badge |

### 📱 Social Feed (`/api/v1/social`)
| # | Test | Validates |
|---|------|-----------|
| 1 | Create text post → 201 | Core |
| 2 | Anonymous post → author hidden | Feature |
| 3 | Empty content → 400 | Validation |
| 4 | Exceeds 3000 chars → 422 | Validation |
| 5 | Get personal feed (paginated) | Core |
| 6 | Faculty-filtered feed | Filter |
| 7 | Trending posts | Feature |
| 8 | Get post by ID | CRUD |
| 9 | Non-existent post → 404 | Edge Case |
| 10 | Like post → count++ | Reaction |
| 11 | Double-like → 409 | Validation |
| 12 | Unlike post → count-- | Reaction |
| 13 | Add comment → 201 | Comment |
| 14 | Get comments list | Comment |
| 15 | Empty comment → 400 | Validation |
| 16 | Delete own comment | Lifecycle |
| 17 | Update own post → 200 | CRUD |
| 18 | Update other's post → 403 | **Security** |
| 19 | Delete own post → 200 | CRUD |
| 20 | Filter by hashtag | Search |
| 21 | No auth → 401 | Auth |

### 📅 Lecture Schedule (`/api/v1/schedule`)
| # | Test | Validates |
|---|------|-----------|
| 1 | Weekly schedule → 200 | Core |
| 2 | Specific week number | Filter |
| 3 | Faculty-filtered schedule | Filter |
| 4 | Invalid week number → 400 | Validation |
| 5 | Today's lectures → 200 | Core |
| 6 | Lecture times format HH:mm | Schema |
| 7 | By specific date | Filter |
| 8 | Invalid date format → 400 | Validation |
| 9 | Lecture by ID → full details | Detail |
| 10 | Non-existent lecture → 404 | Edge Case |
| 11 | Online lectures have meeting_link | Feature |
| 12 | Search courses by keyword | Search |
| 13 | Single-char query → 400 | Validation |
| 14 | Courses by instructor | Filter |
| 15 | Upcoming reminders (24h) | Feature |
| 16 | No auth → 401 | Auth |
| 17 | Department-filtered schedule | Filter |
| 18 | Response schema validation | Schema |

### ⚡ Performance Tests
| # | Test | Type |
|---|------|------|
| 1 | All critical endpoints SLA (data-driven) | Response Time |
| 2 | 50 concurrent → /auth/profile | Load |
| 3 | 30 concurrent → /schedule/today | Load |
| 4 | 40 concurrent → /social/feed | Load |
| 5 | 20 concurrent → /ai/chat | Load |
| 6 | 25 concurrent → /chat/messages | Load |
| 7 | Throughput ≥ 10 RPS / 10 seconds | Throughput |
| 8 | Endurance 30s stable response times | Endurance |
| 9 | Spike: 5→100→5 users, measure recovery | Spike |
| 10 | 30 concurrent logins | Load |

---

## Running Tests

### Prerequisites
- Java 11+
- Maven 3.8+
- Allure CLI (for reports): `npm install -g allure-commandline`

### Commands

```bash
# Full regression suite
mvn clean test -P regression

# Smoke tests only (fastest)
mvn clean test -P smoke

# Run by group
mvn clean test -Dgroups=auth
mvn clean test -Dgroups=ai
mvn clean test -Dgroups=chat
mvn clean test -Dgroups=social
mvn clean test -Dgroups=schedule
mvn clean test -Dgroups=performance

# Using shell script
chmod +x run.sh
./run.sh smoke
./run.sh all
./run.sh ai
./run.sh serve       # opens Allure in browser

# Generate Allure report
mvn allure:report    # → target/site/allure-maven-plugin/index.html
mvn allure:serve     # serves locally in browser

# Override config via system property
mvn clean test -Dbase.url=https://staging.academyai.edu.eg -Dgroups=smoke
```

---

## Allure Report

After running tests:

```
target/
└── allure-results/     ← raw JSON results
└── site/
    └── allure-maven-plugin/
        └── index.html  ← open in browser
```

**Report Sections:**
- **Overview** – pass/fail/skip donut chart
- **Suites** – Auth / AI / Chat / Social / Schedule / Performance
- **Behaviors** – Epics → Features → Stories hierarchy
- **Timeline** – parallel execution visualization
- **Categories** – grouped failure types
- **Graphs** – response time trends

---

## Performance Thresholds (SLA)

| Endpoint Category | Max Response Time | Concurrent Users Tested |
|---|---|---|
| Auth (Login, Profile) | 2,000 ms | 50 |
| AI Chat | 10,000 ms | 20 |
| Student Chat | 1,000 ms | 25 |
| Lecture Schedule | 1,500 ms | 30 |
| Social Feed | 2,000 ms | 40 |

Throughput target: **≥ 10 RPS** on schedule endpoint.
Error rate limit: **< 1%** under load, **< 5%** during endurance.

---

## Config Override

Edit `src/test/resources/config.properties` or pass via `-D`:

```bash
mvn test -Dbase.url=https://prod.academyai.edu.eg \
         -Dstudent.email=realstudent@academyai.edu.eg \
         -Dstudent.password=RealPass@123 \
         -Dperf.ai.max.response.ms=15000
```

---

## Test Groups

| Group | Description |
|---|---|
| `smoke` | Critical path – runs first, fastest |
| `auth` | Authentication & authorization |
| `ai` | AI chat functionality & quality |
| `chat` | Student-to-student messaging |
| `social` | Social feed CRUD |
| `schedule` | Lecture timetable |
| `performance` | Load, spike, throughput, endurance |
| `security` | Auth bypass, injection, XSS |

---

*Framework: AcademyAI API Automation | Stack: Rest Assured + TestNG + Allure + Maven*
