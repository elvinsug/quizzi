# Quizzi — Classroom Live Quiz System

A Kahoot-style live quiz system for the IM2073 course at NTU. Consists of:

1. **Web App (Host/Instructor)** — Java Servlets + JSP on Apache Tomcat
2. **Android App (Student/Player)** — Native Android (Java)
3. **Web Player (Student/Player)** — Mobile-responsive browser interface

## Prerequisites

- **Java JDK 8+** (for compiling servlets and Android)
- **Apache Tomcat 9+**
- **MySQL 8+**
- **Android Studio** (for building the Android app)

## 1. Database Setup

```bash
mysql -u root -p < database/schema.sql
```

This creates the `quizzi` database with 5 tables: `quizzes`, `questions`, `game_sessions`, `players`, `responses`.

### Configure DB Credentials

Edit `quizzi-web/WEB-INF/classes/com/quizzi/DBUtil.java` and update the connection parameters if needed:

```java
private static final String URL = "jdbc:mysql://localhost:3306/quizzi?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
private static final String USER = "root";
private static final String PASSWORD = "";  // Set your MySQL password
```

## 2. Web App Setup (Tomcat)

### Download MySQL Connector

Download `mysql-connector-j-9.1.0.jar` from [MySQL Downloads](https://dev.mysql.com/downloads/connector/j/) and place it in:

```
quizzi-web/WEB-INF/lib/mysql-connector-j-9.1.0.jar
```

### Compile Servlets

```bash
./compile.sh /path/to/tomcat
# Example: ./compile.sh /usr/local/tomcat
```

The script compiles all `.java` files in `WEB-INF/classes/com/quizzi/` against `servlet-api.jar` and the MySQL connector.

### Deploy to Tomcat

Option A — Copy the webapp folder:
```bash
cp -r quizzi-web $CATALINA_HOME/webapps/quizzi
```

Option B — Symlink (for development):
```bash
ln -s $(pwd)/quizzi-web $CATALINA_HOME/webapps/quizzi
```

### Start Tomcat

```bash
$CATALINA_HOME/bin/startup.sh
```

Open `http://localhost:8080/quizzi/` in your browser. You should see the Quizzi dashboard.

## 3. Android App Setup

1. Open `quizzi-android/` in **Android Studio**
2. Wait for Gradle sync to complete
3. Configure the server URL in `utils/Constants.java`:
   - **Emulator**: `http://10.0.2.2:8080/quizzi` (default — routes to host machine)
   - **Physical device on same WiFi**: `http://YOUR_IP:8080/quizzi`
4. Build and run on an emulator or device (min SDK 26 / Android 8.0)

## 4. How to Use

### Create a Quiz (Instructor)

1. Go to `http://localhost:8080/quizzi/`
2. Click **"+ Create Quiz"**
3. Enter title, description, and add questions with 2–4 answer options
4. Mark the correct answer for each question
5. Set time limit (10/15/20/30 seconds) and points (Standard 1000 / Double 2000)
6. Click **"Save Quiz"**

### Host a Live Game (Instructor)

1. On the dashboard, click **"Host Live"** on a quiz
2. The lobby screen shows a **6-digit Game PIN** and **QR code**
3. Share the PIN with students (project the lobby screen)
4. Watch players join in real-time
5. Click **"Start Game"** when ready

### Join as a Student

**Android App:**
1. Open the Quizzi app
2. Enter the 6-digit Game PIN (or scan QR code)
3. Enter a nickname
4. Wait in the lobby, then answer questions as they appear

**Web Browser:**
1. Go to `http://HOST_IP:8080/quizzi/play/`
2. Enter the 6-digit Game PIN
3. Enter a nickname
4. The game plays entirely in the browser

### Game Flow

```
Lobby → Question (countdown timer) → Answer → Waiting
→ Score Reveal (correct/wrong + points) → Leaderboard
→ Next Question ... → Final Podium (top 3)
```

The host controls the pace by clicking through: Start → Show Results → Show Leaderboard → Next Question → ... → Finish Game.

## Scoring

Uses the Kahoot points formula:

```
points = floor((1 - (response_time_ms / (time_limit_ms)) / 2) * points_possible)
```

- Wrong answers always score 0
- Fastest correct answer gets ~100% of points
- Slowest correct answer gets ~50% of points

**Streak Bonuses:**
- 2 correct in a row: +100
- 3 in a row: +200
- 4 in a row: +300
- 5+ in a row: +500

## Project Structure

```
quizzi/
├── quizzi-web/           Tomcat webapp
│   ├── WEB-INF/
│   │   ├── classes/com/quizzi/   Java servlets (10 files)
│   │   ├── lib/                  MySQL connector JAR
│   │   └── web.xml
│   ├── css/quizzi.css    Design system
│   ├── js/polling.js     Shared polling engine
│   ├── index.jsp         Dashboard
│   ├── create-quiz.jsp   Quiz editor
│   ├── lobby.jsp         Game lobby (PIN + QR + player list)
│   ├── host-game.jsp     Live game host view
│   ├── leaderboard.jsp   Standalone leaderboard page
│   ├── podium.jsp        Final results podium
│   └── play/             Web player (3 JSP files)
├── quizzi-android/       Android Studio project
│   └── app/src/main/
│       ├── java/com/quizzi/
│       │   ├── activities/   7 activities
│       │   ├── models/       3 POJOs
│       │   ├── network/      ApiClient + PollingService
│       │   └── utils/        Constants + QRScannerHelper
│       └── res/              Layouts, colors, styles, drawables
├── database/schema.sql   MySQL schema
├── compile.sh            Servlet compilation script
└── README.md
```

## API Endpoints

| Method | URL | Purpose |
|--------|-----|---------|
| POST | `/api/quiz` | Create quiz with questions |
| GET | `/api/quizzes` | List all quizzes |
| POST | `/api/start?quizId=X` | Start game session, get PIN |
| GET | `/api/join?pin=X&nickname=Y` | Join game |
| GET | `/api/status?sessionId=X&playerId=Y` | Poll game state (every 1.5s) |
| POST | `/api/next?sessionId=X&action=Y` | Advance game state |
| GET | `/select?sessionId=X&questionId=Y&playerId=Z&choice=C&time=T` | Submit answer |
| GET | `/display?sessionId=X&questionId=Y` | Get response distribution |
| GET | `/api/leaderboard?sessionId=X` | Get full leaderboard |
| GET | `/api/results?sessionId=X&playerId=Y&questionId=Z` | Get player's result |

## Troubleshooting

- **Android can't connect**: Ensure `usesCleartextTraffic="true"` is in AndroidManifest.xml. Use `10.0.2.2` for emulator or your machine's IP for physical devices.
- **Servlet compilation fails**: Make sure `CATALINA_HOME` points to your Tomcat installation and `servlet-api.jar` exists in `$CATALINA_HOME/lib/`.
- **MySQL connection refused**: Check MySQL is running, credentials in `DBUtil.java` are correct, and the `quizzi` database exists.
