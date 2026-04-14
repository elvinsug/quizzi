# Quizzi — Classroom Live Quiz System

A Kahoot-style live quiz platform built for the IM2073 course at NTU. Instructors create quizzes and host live game sessions; students join via a 6-digit PIN on their phones or laptops and compete in real-time.

The system has three components:

| Component | Tech Stack | Purpose |
|-----------|-----------|---------|
| **Web App** (Host/Instructor) | Java Servlets + JSP on Apache Tomcat | Create quizzes, host live games, display leaderboards |
| **Web Player** (Student) | JSP — mobile-responsive browser UI | Join and play games from any browser |
| **Android App** (Student) | Native Android (Java) + ZXing QR | Join and play games on Android devices |

---

## Table of Contents

- [Quick Start — Web App (macOS)](#quick-start--web-app-macos)
- [Quick Start — Android App](#quick-start--android-app)
- [Architecture Overview](#architecture-overview)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Setup Guide — macOS](#setup-guide--macos)
- [Setup Guide — Windows](#setup-guide--windows)
- [Database Setup (Both Platforms)](#database-setup-both-platforms)
- [Configuring DB Credentials](#configuring-db-credentials)
- [Compiling and Deploying the Web App](#compiling-and-deploying-the-web-app)
- [Android App Setup](#android-app-setup)
- [How to Use](#how-to-use)
- [Scoring System](#scoring-system)
- [API Reference](#api-reference)
- [Database Schema](#database-schema)
- [Troubleshooting](#troubleshooting)

---

## Quick Start — Web App (macOS)

> Assumes you have Homebrew installed. For detailed instructions or Windows setup, see the full guides below.

```bash
# 1. Install prerequisites
brew install openjdk@17 mysql tomcat

# 2. Start MySQL
brew services start mysql

# 3. Set your MySQL root password (if you haven't already)
mysql_secure_installation

# 4. Create the database
mysql -u root -p < database/schema.sql

# 5. Set your DB password in the source code
#    Edit quizzi-web/WEB-INF/classes/com/quizzi/DBUtil.java
#    and set: private static final String PASSWORD = "YOUR_PASSWORD";

# 6. Download the MySQL connector JAR
mkdir -p quizzi-web/WEB-INF/lib
curl -L -o quizzi-web/WEB-INF/lib/mysql-connector-j-9.1.0.jar \
  "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.1.0/mysql-connector-j-9.1.0.jar"

# 7. Find your Tomcat path (Homebrew may install tomcat or tomcat@10)
#    Apple Silicon: /opt/homebrew/opt/tomcat/libexec (or tomcat@10)
#    Intel:         /usr/local/opt/tomcat/libexec
export CATALINA_HOME=/opt/homebrew/opt/tomcat/libexec

# 8. Compile the servlets
chmod +x compile.sh
./compile.sh $CATALINA_HOME

# 9. Deploy (symlink for development)
ln -s $(pwd)/quizzi-web $CATALINA_HOME/webapps/quizzi

# 10. Start Tomcat
$CATALINA_HOME/bin/catalina.sh run
```

Open [http://localhost:8080/quizzi/](http://localhost:8080/quizzi/) — you should see the Quizzi dashboard.

> **Using Tomcat 10?** Tomcat 10+ uses Jakarta EE (`jakarta.servlet`) instead of Java EE (`javax.servlet`). You must replace all `import javax.servlet` with `import jakarta.servlet` in every Java file under `WEB-INF/classes/com/quizzi/`, and update `web.xml` to use the `https://jakarta.ee/xml/ns/jakartaee` namespace. See the [Troubleshooting](#troubleshooting) section for details.

## Quick Start — Android App

1. **Open the project** — Launch Android Studio and open the `quizzi-android/` folder
2. **Wait for Gradle sync** — Android Studio will download all dependencies automatically (this may take a few minutes on first open)
3. **Create an emulator** (if you don't have one):
   - Go to **Tools > Device Manager**
   - Click **Create Device**
   - Select a phone (e.g. Pixel 6), click **Next**
   - Choose a system image with **API 26+** (download it if needed), click **Next > Finish**
4. **Check the server URL** — The file `app/src/main/java/com/quizzi/utils/Constants.java` is pre-configured for the Android emulator:
   ```java
   public static final String BASE_URL = "http://10.0.2.2:8080/quizzi";
   ```
   - This `10.0.2.2` address routes to your host machine's `localhost` from inside the emulator — no changes needed if your Tomcat server is running locally.
   - If using a **physical device**, change this to your machine's IP address (e.g. `http://192.168.1.100:8080/quizzi`). Both devices must be on the same WiFi network.
5. **Run the app** — Select your emulator from the device dropdown at the top, then click the green **Run** button (▶)
6. **Make sure Tomcat is running** — The Android app connects to the web server, so the web app must be started first (see above)

> **No Android Studio?** Students can also join games through the **web player** at `http://HOST_IP:8080/quizzi/play/` on any mobile browser — no app install required.

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────┐
│                     Apache Tomcat 9+                      │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  quizzi-web (WAR-style deployment)                  │  │
│  │                                                     │  │
│  │  JSP Pages (UI)          Java Servlets (API)        │  │
│  │  ├── index.jsp           ├── CreateQuizServlet      │  │
│  │  ├── create-quiz.jsp     ├── ListQuizServlet        │  │
│  │  ├── lobby.jsp           ├── StartGameServlet       │  │
│  │  ├── host-game.jsp       ├── JoinGameServlet        │  │
│  │  ├── leaderboard.jsp     ├── GameStatusServlet      │  │
│  │  ├── podium.jsp          ├── NextQuestionServlet    │  │
│  │  └── play/ (web player)  ├── SelectServlet          │  │
│  │      ├── index.jsp       ├── DisplayServlet         │  │
│  │      ├── nickname.jsp    ├── ResultsServlet         │  │
│  │      └── game.jsp        └── LeaderboardServlet     │  │
│  └─────────────────────────────────────────────────────┘  │
│                            │ JDBC                         │
│                            ▼                              │
│                    ┌──────────────┐                       │
│                    │   MySQL 8+   │                       │
│                    │   (quizzi)   │                       │
│                    └──────────────┘                       │
└──────────────────────────────────────────────────────────┘
        ▲ HTTP                              ▲ HTTP
        │                                   │
  ┌─────┴─────┐                     ┌───────┴───────┐
  │  Browser   │                     │  Android App  │
  │ (Host UI   │                     │  (Student)    │
  │  + Player) │                     │               │
  └────────────┘                     └───────────────┘
```

All clients communicate with the Tomcat backend over HTTP. The Android app and web player poll the server every 1.5 seconds for game state updates.

---

## Project Structure

```
quizzi/
│
├── README.md                          This file
├── compile.sh                         Shell script to compile Java servlets
│
├── database/
│   └── schema.sql                     MySQL schema (5 tables)
│
├── quizzi-web/                        Tomcat web application
│   ├── WEB-INF/
│   │   ├── web.xml                    Java EE 4.0 deployment descriptor
│   │   ├── classes/com/quizzi/        Java source files
│   │   │   ├── DBUtil.java            JDBC connection helper
│   │   │   ├── CreateQuizServlet.java POST /api/quiz
│   │   │   ├── ListQuizServlet.java   GET  /api/quizzes
│   │   │   ├── StartGameServlet.java  POST /api/start
│   │   │   ├── JoinGameServlet.java   GET  /api/join
│   │   │   ├── GameStatusServlet.java GET  /api/status
│   │   │   ├── NextQuestionServlet.java POST /api/next
│   │   │   ├── SelectServlet.java     GET  /select
│   │   │   ├── DisplayServlet.java    GET  /display
│   │   │   ├── ResultsServlet.java    GET  /api/results
│   │   │   └── LeaderboardServlet.java GET /api/leaderboard
│   │   └── lib/                       Place mysql-connector-j-*.jar here
│   ├── css/quizzi.css                 Design system stylesheet
│   ├── js/polling.js                  Shared polling engine (host + player)
│   ├── index.jsp                      Dashboard — list quizzes, host games
│   ├── create-quiz.jsp                Quiz editor — add questions & options
│   ├── lobby.jsp                      Game lobby — shows PIN, QR, player list
│   ├── host-game.jsp                  Live game host view
│   ├── leaderboard.jsp                Standalone leaderboard page
│   ├── podium.jsp                     Final results — top 3 podium
│   └── play/                          Web player (student-facing)
│       ├── index.jsp                  Enter game PIN
│       ├── nickname.jsp               Enter nickname
│       └── game.jsp                   Play the game in-browser
│
└── quizzi-android/                    Android Studio project
    ├── build.gradle                   Root Gradle config (AGP 8.2.0)
    ├── settings.gradle                Project settings
    ├── gradle.properties              Gradle properties
    └── app/
        ├── build.gradle               App module (compileSdk 34, minSdk 26)
        └── src/main/
            ├── AndroidManifest.xml
            ├── java/com/quizzi/
            │   ├── activities/         Screen controllers
            │   │   ├── JoinGameActivity.java      Enter PIN / scan QR
            │   │   ├── LobbyActivity.java         Wait for game to start
            │   │   ├── QuestionActivity.java      Answer questions
            │   │   ├── WaitingResultActivity.java Wait for results
            │   │   ├── ScoreRevealActivity.java   Show score after question
            │   │   ├── RankActivity.java          Show ranking
            │   │   └── GameOverActivity.java      Final results
            │   ├── models/             Data classes
            │   │   ├── GameState.java
            │   │   ├── Player.java
            │   │   └── Question.java
            │   ├── network/            HTTP communication
            │   │   ├── ApiClient.java             HTTP client wrapper
            │   │   └── PollingService.java        Background polling (1.5s)
            │   └── utils/
            │       ├── Constants.java             Server URL, timeouts
            │       └── QRScannerHelper.java       ZXing QR integration
            └── res/                    Android resources
                ├── layout/             XML layouts for each activity
                ├── drawable/           Icons and shapes
                └── values/             Colors, styles, strings
```

---

## Prerequisites

You need the following installed on your machine:

| Software | Version | Purpose |
|----------|---------|---------|
| Java JDK | 8 or higher | Compile servlets and build Android app |
| Apache Tomcat | 9 or higher | Run the web application |
| MySQL | 8 or higher | Store quizzes, sessions, and responses |
| Android Studio | Latest | Build and run the Android app (optional) |

---

## Setup Guide — macOS

### Step 1: Install Java JDK

```bash
# Using Homebrew (recommended)
brew install openjdk@17

# Add to your shell profile (~/.zshrc)
export JAVA_HOME=$(/usr/libexec/java_home)
export PATH="$JAVA_HOME/bin:$PATH"

# Reload your shell
source ~/.zshrc

# Verify
java -version
javac -version
```

### Step 2: Install MySQL

```bash
# Install via Homebrew
brew install mysql

# Start MySQL service
brew services start mysql

# Secure the installation (set root password)
mysql_secure_installation

# Verify it's running
mysql -u root -p -e "SELECT VERSION();"
```

### Step 3: Install Apache Tomcat

```bash
# Install via Homebrew
brew install tomcat

# Homebrew installs Tomcat to:
#   /opt/homebrew/opt/tomcat/libexec   (Apple Silicon)
#   /usr/local/opt/tomcat/libexec      (Intel)

# Set CATALINA_HOME in your ~/.zshrc
export CATALINA_HOME=/opt/homebrew/opt/tomcat/libexec   # Apple Silicon
# OR
export CATALINA_HOME=/usr/local/opt/tomcat/libexec      # Intel

source ~/.zshrc

# Verify
ls $CATALINA_HOME/lib/servlet-api.jar
```

### Step 4: Download MySQL Connector JAR

1. Go to [MySQL Connector/J Downloads](https://dev.mysql.com/downloads/connector/j/)
2. Select **Platform Independent**
3. Download the `.tar.gz` or `.zip` archive
4. Extract it and copy the JAR file:

```bash
cp mysql-connector-j-9.1.0.jar quizzi-web/WEB-INF/lib/
```

> If the `lib/` directory doesn't exist, create it first: `mkdir -p quizzi-web/WEB-INF/lib/`

### Step 5: Set Up the Database

```bash
mysql -u root -p < database/schema.sql
```

### Step 6: Configure DB Credentials

Edit `quizzi-web/WEB-INF/classes/com/quizzi/DBUtil.java` and set your MySQL password:

```java
private static final String PASSWORD = "";  // <-- set your MySQL root password here
```

### Step 7: Compile the Servlets

```bash
chmod +x compile.sh
./compile.sh
```

The script auto-detects `CATALINA_HOME` from common Homebrew locations. If it can't find Tomcat, pass the path explicitly:

```bash
./compile.sh /opt/homebrew/opt/tomcat/libexec
```

### Step 8: Deploy to Tomcat

**Option A — Copy (production-like):**
```bash
cp -r quizzi-web $CATALINA_HOME/webapps/quizzi
```

**Option B — Symlink (recommended for development):**
```bash
ln -s $(pwd)/quizzi-web $CATALINA_HOME/webapps/quizzi
```

### Step 9: Start Tomcat

```bash
$CATALINA_HOME/bin/catalina.sh run
```

> Use `catalina.sh run` to see logs in the terminal. Use `catalina.sh start` to run in the background.

### Step 10: Verify

Open [http://localhost:8080/quizzi/](http://localhost:8080/quizzi/) in your browser. You should see the Quizzi dashboard.

---

## Setup Guide — Windows

### Step 1: Install Java JDK

1. Download JDK 17 from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [Adoptium](https://adoptium.net/)
2. Run the installer
3. Set environment variables:
   - Open **Start > Environment Variables** (search "Edit the system environment variables")
   - Under **System variables**, click **New**:
     - Variable name: `JAVA_HOME`
     - Variable value: `C:\Program Files\Java\jdk-17` (adjust to your install path)
   - Edit the `Path` variable and add: `%JAVA_HOME%\bin`
4. Open a **new** Command Prompt and verify:

```cmd
java -version
javac -version
```

### Step 2: Install MySQL

1. Download [MySQL Installer for Windows](https://dev.mysql.com/downloads/installer/)
2. Run the installer and choose **Server only** (or Full if you want MySQL Workbench)
3. During setup:
   - Set the root password (remember this)
   - Configure MySQL to run as a Windows Service (so it starts automatically)
4. Verify it's running:

```cmd
mysql -u root -p -e "SELECT VERSION();"
```

> If `mysql` is not found, add `C:\Program Files\MySQL\MySQL Server 8.0\bin` to your `Path` environment variable.

### Step 3: Install Apache Tomcat

1. Download Tomcat 9 from [Apache Tomcat Downloads](https://tomcat.apache.org/download-90.cgi)
2. Download the **64-bit Windows zip** (not the installer)
3. Extract to a location like `C:\apache-tomcat-9`
4. Set environment variable:
   - Variable name: `CATALINA_HOME`
   - Variable value: `C:\apache-tomcat-9` (adjust to your extract path)
5. Add `%CATALINA_HOME%\bin` to your `Path`
6. Verify:

```cmd
dir %CATALINA_HOME%\lib\servlet-api.jar
```

### Step 4: Download MySQL Connector JAR

1. Go to [MySQL Connector/J Downloads](https://dev.mysql.com/downloads/connector/j/)
2. Select **Platform Independent**
3. Download and extract the archive
4. Copy the JAR:

```cmd
mkdir quizzi-web\WEB-INF\lib
copy mysql-connector-j-9.1.0.jar quizzi-web\WEB-INF\lib\
```

### Step 5: Set Up the Database

```cmd
mysql -u root -p < database\schema.sql
```

### Step 6: Configure DB Credentials

Edit `quizzi-web\WEB-INF\classes\com\quizzi\DBUtil.java` in any text editor and set your MySQL password:

```java
private static final String PASSWORD = "";  // <-- set your MySQL root password here
```

### Step 7: Compile the Servlets

Windows cannot run `compile.sh` directly. Use this command instead:

```cmd
javac -cp "%CATALINA_HOME%\lib\servlet-api.jar;quizzi-web\WEB-INF\lib\mysql-connector-j-9.1.0.jar" -d quizzi-web\WEB-INF\classes quizzi-web\WEB-INF\classes\com\quizzi\*.java
```

> **Key difference:** Windows uses `;` (semicolon) as the classpath separator, while macOS/Linux uses `:` (colon).

### Step 8: Deploy to Tomcat

```cmd
xcopy /E /I quizzi-web %CATALINA_HOME%\webapps\quizzi
```

> For development, you can also create a junction link:
> ```cmd
> mklink /J %CATALINA_HOME%\webapps\quizzi %cd%\quizzi-web
> ```

### Step 9: Start Tomcat

```cmd
%CATALINA_HOME%\bin\catalina.bat run
```

> Use `catalina.bat run` to see logs in the terminal. Use `startup.bat` to run in the background.

### Step 10: Verify

Open [http://localhost:8080/quizzi/](http://localhost:8080/quizzi/) in your browser. You should see the Quizzi dashboard.

---

## Database Setup (Both Platforms)

The schema creates 5 tables in a database called `quizzi`:

| Table | Purpose |
|-------|---------|
| `quizzes` | Quizzes created by the instructor (title, description) |
| `questions` | Questions with 2-4 options, correct answer, time limit, points |
| `game_sessions` | Live game instances with a 6-digit PIN and status tracking |
| `players` | Students who joined a session (nickname, total score) |
| `responses` | Individual answer submissions (choice, response time, points) |

To reset the database (drops and recreates all tables):

```bash
mysql -u root -p -e "DROP DATABASE quizzi;"
mysql -u root -p < database/schema.sql
```

---

## Configuring DB Credentials

All database configuration lives in a single file:

**`quizzi-web/WEB-INF/classes/com/quizzi/DBUtil.java`**

```java
private static final String URL = "jdbc:mysql://localhost:3306/quizzi?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
private static final String USER = "root";
private static final String PASSWORD = "";  // Set your MySQL password
```

After changing this file, you must recompile the servlets (Step 7) and redeploy.

---

## Compiling and Deploying the Web App

Every time you change a `.java` file, you need to recompile and redeploy:

**macOS:**
```bash
./compile.sh
# If using symlink deployment, changes are instant after compile
# If using copy deployment:
rm -rf $CATALINA_HOME/webapps/quizzi
cp -r quizzi-web $CATALINA_HOME/webapps/quizzi
```

**Windows:**
```cmd
javac -cp "%CATALINA_HOME%\lib\servlet-api.jar;quizzi-web\WEB-INF\lib\mysql-connector-j-9.1.0.jar" -d quizzi-web\WEB-INF\classes quizzi-web\WEB-INF\classes\com\quizzi\*.java

xcopy /E /I /Y quizzi-web %CATALINA_HOME%\webapps\quizzi
```

JSP and CSS/JS changes do **not** require recompilation — just refresh the browser (if using symlink/junction, they're applied immediately).

---

## Android App Setup

1. Open `quizzi-android/` in **Android Studio**
2. Wait for Gradle sync to complete (it downloads dependencies automatically)
3. Configure the server URL in `app/src/main/java/com/quizzi/utils/Constants.java`:

```java
public static final String BASE_URL = "http://10.0.2.2:8080/quizzi";  // Emulator → host machine
```

| Scenario | BASE_URL |
|----------|----------|
| Android Emulator | `http://10.0.2.2:8080/quizzi` (default — routes to host machine's localhost) |
| Physical device on same WiFi | `http://YOUR_MACHINE_IP:8080/quizzi` (find your IP with `ipconfig` or `ifconfig`) |

4. Build and run on an emulator or device (minimum SDK 26 / Android 8.0)

### Android Dependencies

These are managed by Gradle and downloaded automatically:

| Library | Version | Purpose |
|---------|---------|---------|
| AndroidX AppCompat | 1.6.1 | Backwards-compatible UI components |
| Material Components | 1.11.0 | Material Design widgets |
| ConstraintLayout | 2.1.4 | Flexible layout system |
| ZXing Android Embedded | 4.3.0 | QR code scanning |

---

## How to Use

### Create a Quiz (Instructor)

1. Open [http://localhost:8080/quizzi/](http://localhost:8080/quizzi/)
2. Click **"+ Create Quiz"**
3. Enter a title and description
4. Add questions with 2-4 answer options
5. Mark the correct answer for each question
6. Set time limit (10/15/20/30 seconds) and points (Standard 1000 / Double 2000)
7. Click **"Save Quiz"**

### Host a Live Game (Instructor)

1. On the dashboard, click **"Host Live"** on any quiz
2. The lobby screen displays a **6-digit Game PIN** and **QR code**
3. Project the lobby screen and share the PIN with students
4. Watch players join in real-time
5. Click **"Start Game"** when ready

### Join as a Student

**Via Web Browser:**
1. Go to `http://HOST_IP:8080/quizzi/play/`
2. Enter the 6-digit Game PIN
3. Enter a nickname
4. Play the game in the browser

**Via Android App:**
1. Open the Quizzi app
2. Enter the 6-digit Game PIN or scan the QR code
3. Enter a nickname
4. Wait in the lobby, then answer questions as they appear

### Game Flow

```
Lobby → Question (countdown timer) → Answer → Waiting for Results
  → Score Reveal (correct/wrong + points earned)
  → Leaderboard (top players)
  → Next Question ... → Final Podium (top 3 players)
```

The host controls the pace by advancing through each stage manually.

---

## Scoring System

Points are calculated using a Kahoot-style formula based on response speed:

```
points = floor((1 - (response_time_ms / time_limit_ms) / 2) * points_possible)
```

- Wrong answers always score **0 points**
- Fastest correct answer earns **~100%** of the possible points
- Slowest correct answer earns **~50%** of the possible points

### Streak Bonuses

| Consecutive Correct | Bonus Points |
|---------------------|-------------|
| 2 in a row | +100 |
| 3 in a row | +200 |
| 4 in a row | +300 |
| 5+ in a row | +500 |

---

## API Reference

All API endpoints are served by the Tomcat webapp at the `/quizzi` context path.

| Method | Endpoint | Purpose | Key Parameters |
|--------|----------|---------|----------------|
| `POST` | `/api/quiz` | Create a quiz with questions | JSON body |
| `GET` | `/api/quizzes` | List all quizzes | — |
| `POST` | `/api/start?quizId=X` | Start a game session | `quizId` |
| `GET` | `/api/join?pin=X&nickname=Y` | Join a game | `pin`, `nickname` |
| `GET` | `/api/status?sessionId=X&playerId=Y` | Poll game state | `sessionId`, `playerId` |
| `POST` | `/api/next?sessionId=X&action=Y` | Advance game state | `sessionId`, `action` |
| `GET` | `/select?sessionId=X&questionId=Y&playerId=Z&choice=C&time=T` | Submit an answer | `sessionId`, `questionId`, `playerId`, `choice`, `time` |
| `GET` | `/display?sessionId=X&questionId=Y` | Get response distribution | `sessionId`, `questionId` |
| `GET` | `/api/leaderboard?sessionId=X` | Get full leaderboard | `sessionId` |
| `GET` | `/api/results?sessionId=X&playerId=Y&questionId=Z` | Get a player's result | `sessionId`, `playerId`, `questionId` |

---

## Database Schema

```sql
quizzes            questions              game_sessions
┌────────────┐     ┌──────────────────┐   ┌─────────────────────┐
│ id (PK)    │◄──┐ │ id (PK)          │   │ id (PK)             │
│ title      │   └─│ quiz_id (FK)     │┌─►│ quiz_id (FK)        │
│ description│     │ question_text    ││  │ game_pin (unique)   │
│ created_at │     │ option_a/b/c/d   ││  │ status (enum)       │
└────────────┘     │ correct_answer   ││  │ current_question_   │
                   │ time_limit_seconds││  │   order             │
                   │ points_possible  ││  │ question_started_at │
                   │ question_order   ││  │ created_at          │
                   └──────────────────┘│  └─────────────────────┘
                                       │           │
               players                 │  responses │
               ┌───────────────────┐   │  ┌────────┴───────────┐
               │ id (PK)           │   │  │ id (PK)            │
               │ game_session_id   │───┘  │ game_session_id(FK)│
               │   (FK)            │      │ question_id (FK)   │
               │ nickname          │◄─────│ player_id (FK)     │
               │ total_score       │      │ choice             │
               └───────────────────┘      │ response_time_ms   │
                                          │ points_earned      │
                                          │ submitted_at       │
                                          └────────────────────┘
```

Game session statuses: `waiting` → `active` → `showing_question` → `showing_results` → `showing_leaderboard` → `finished`

---

## Troubleshooting

### Servlet compilation fails

- **macOS:** Make sure `CATALINA_HOME` is set or pass the path to `compile.sh`. Verify `servlet-api.jar` exists at `$CATALINA_HOME/lib/servlet-api.jar`.
- **Windows:** Ensure `%CATALINA_HOME%` is set correctly. Remember to use `;` (semicolon) as the classpath separator, not `:`.
- Both: Ensure the MySQL Connector JAR is in `quizzi-web/WEB-INF/lib/`.

### MySQL connection refused

- Check that MySQL is running (`brew services list` on macOS, `services.msc` on Windows).
- Verify the credentials in `DBUtil.java` match your MySQL root password.
- Confirm the `quizzi` database exists: `mysql -u root -p -e "SHOW DATABASES;"`.

### Android can't connect to the server

- Ensure `usesCleartextTraffic="true"` is set in `AndroidManifest.xml` (required for HTTP on Android 9+).
- **Emulator:** Use `10.0.2.2` as the host (this routes to your machine's localhost).
- **Physical device:** Use your machine's local IP address (find it with `ifconfig` on macOS or `ipconfig` on Windows). Both devices must be on the same WiFi network.
- Ensure Tomcat is running and accessible at the configured URL.

### Tomcat won't start

- Check that port 8080 is not already in use: `lsof -i :8080` (macOS) or `netstat -ano | findstr 8080` (Windows).
- Check Tomcat logs at `$CATALINA_HOME/logs/catalina.out`.

### Tomcat 10 — "javax.servlet not found" or servlets not loading

Tomcat 10+ uses the **Jakarta EE** namespace (`jakarta.servlet`) instead of the old Java EE namespace (`javax.servlet`). If you installed Tomcat via `brew install tomcat` or `brew install tomcat@10`, you likely have Tomcat 10.

**Fix the Java files:** Replace all `javax.servlet` imports with `jakarta.servlet` in every `.java` file under `quizzi-web/WEB-INF/classes/com/quizzi/`:

```bash
# macOS/Linux — bulk replace
cd quizzi-web/WEB-INF/classes/com/quizzi/
sed -i '' 's/import javax\.servlet/import jakarta.servlet/g' *.java
```

```cmd
:: Windows (PowerShell)
Get-ChildItem quizzi-web\WEB-INF\classes\com\quizzi\*.java |
  ForEach-Object { (Get-Content $_) -replace 'import javax\.servlet', 'import jakarta.servlet' | Set-Content $_ }
```

**Fix web.xml:** Update the namespace in `quizzi-web/WEB-INF/web.xml`:

```xml
<!-- Change the opening <web-app> tag to: -->
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
         https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
         version="6.0"
         metadata-complete="false">
```

After making these changes, recompile the servlets and restart Tomcat.

### JSP EL parsing errors with JavaScript template literals

If you see errors like `Failed to parse the expression [${someJsVar}]`, the JSP engine is interpreting JavaScript `${...}` template literal syntax as EL expressions. Fix this by escaping the dollar sign with a backslash (`\${...}`) inside JavaScript template literals in `.jsp` files. The `\` tells the JSP engine to output a literal `${`, which JavaScript then interprets normally.

### JSP changes not reflected

- If you deployed by copying files, you need to copy again after changes.
- If you used a symlink/junction, just refresh the browser.
- Clear the Tomcat work directory if JSPs are cached: `rm -rf $CATALINA_HOME/work/Catalina/localhost/quizzi/` (macOS) or delete the equivalent folder on Windows.

---

## macOS vs. Windows — Quick Reference

| Task | macOS | Windows |
|------|-------|---------|
| Install Java | `brew install openjdk@17` | Oracle/Adoptium installer |
| Install MySQL | `brew install mysql` | MySQL Installer for Windows |
| Install Tomcat | `brew install tomcat` | Download zip, extract manually |
| Start MySQL | `brew services start mysql` | Runs as Windows Service (auto) |
| Start Tomcat | `$CATALINA_HOME/bin/catalina.sh run` | `%CATALINA_HOME%\bin\catalina.bat run` |
| Compile servlets | `./compile.sh` | `javac -cp "...;..." ...` (semicolons) |
| Classpath separator | `:` (colon) | `;` (semicolon) |
| Deploy (copy) | `cp -r quizzi-web $CATALINA_HOME/webapps/quizzi` | `xcopy /E /I quizzi-web %CATALINA_HOME%\webapps\quizzi` |
| Deploy (link) | `ln -s $(pwd)/quizzi-web ...` | `mklink /J ...` |
| Find your IP | `ifconfig` or `ipconfig getifaddr en0` | `ipconfig` |
| Check port 8080 | `lsof -i :8080` | `netstat -ano \| findstr 8080` |
