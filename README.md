# 🏆 FYP Sports Management System

**A comprehensive, production-ready sports management Android application** built with **Kotlin**, **MVVM architecture**, and modern Android development practices.

**Author:** Hussain Asghar  
**Project Type:** Final Year Project (FYP) - Computer Science  
**Last Updated:** April 2026

---

## 📋 Table of Contents

- [Project Overview](#project-overview)
- [Key Features](#key-features)
- [Supported Sports](#supported-sports)
- [Technical Stack](#technical-stack)
- [Project Architecture](#project-architecture)
- [Core Modules](#core-modules)
- [API Endpoints](#api-endpoints)
- [Installation & Setup](#installation--setup)
- [Build & Run](#build--run)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Development Guidelines](#development-guidelines)
- [Future Enhancements](#future-enhancements)
- [License](#license)

---

## 🎯 Project Overview

The **FYP Sports Management System** is an enterprise-level Android application designed to streamline sports tournament organization, player management, fixture scheduling, and real-time match scoring. The app provides a complete solution for sports clubs, organizations, and event managers to manage multiple sports simultaneously with an intuitive UI/UX.

### Use Cases:
- 🏅 **Tournament Organizers:** Create and manage tournaments with ease
- 👥 **Players:** Join teams, track statistics, and participate in tournaments
- 📊 **Scorers/Officials:** Real-time match scoring with comprehensive statistics
- 📸 **Media Managers:** Upload and organize tournament/match media
- 🎮 **Sports Enthusiasts:** Track tournaments, view player stats, and vote for favorites

---

## 🏅 Key Features

### 1. **User Management**
- ✅ Secure JWT-based authentication with role-based access control (RBAC)
- ✅ User account creation and profile management
- ✅ Multi-role support (Player, Organizer, Scorer, Admin)
- ✅ Account status tracking and permissions

### 2. **Tournament Management**
- ✅ Create, edit, and manage tournaments across multiple sports
- ✅ Season-wise tournament organization
- ✅ Tournament overview with statistics
- ✅ Points table (standings/leaderboards)
- ✅ Tournament status tracking (Upcoming, Ongoing, Completed)

### 3. **Fixture & Match Management**
- ✅ Dynamic fixture creation and scheduling
- ✅ Match status management (Scheduled, Live, Completed, Abandoned)
- ✅ Live match scoring with sport-specific scoring rules
- ✅ Match summary generation with statistics
- ✅ Team assignment and management per tournament

### 4. **Player & Team Management**
- ✅ Player registration and profile management
- ✅ Team creation with multiple players per team
- ✅ Player request approval system
- ✅ Team request management and validation
- ✅ Team player associations and roster management

### 5. **Real-Time Scoring System**
- ✅ **Cricket Scoring:** Innings, runs, wickets, balls tracking
- ✅ **Futsal Scoring:** Goals, fouls, time-based scoring
- ✅ **Badminton Scoring:** Points system, rally tracking
- ✅ **Volleyball Scoring:** Rally scoring system
- ✅ **Table Tennis Scoring:** Point-based tracking
- ✅ **Ludo Scoring:** Position-based rankings
- ✅ **Chess Scoring:** Win/Draw/Loss tracking
- ✅ **Tug of War Scoring:** Team strength metrics
- ✅ Ball-by-ball tracking (for applicable sports)
- ✅ Undo/Redo functionality
- ✅ Live scorecard display

### 6. **Statistics & Analytics**
- ✅ Player statistics (runs, wickets, goals, points, etc.)
- ✅ Tournament-specific player performance
- ✅ Comprehensive player stats dashboard
- ✅ Tournament aggregated statistics
- ✅ Points table with rankings

### 7. **Media Management**
- ✅ Season-wise media browsing with pagination
- ✅ Tournament-specific media galleries
- ✅ Sport-specific media collections
- ✅ Image/video upload functionality
- ✅ Media gallery with full-screen viewer
- ✅ Pagination support for large media collections

### 8. **Request & Approval System**
- ✅ Player join requests with approval workflow
- ✅ Team formation request management
- ✅ Request rejection capabilities
- ✅ Automatic notifications for organizers

### 9. **Fan Engagement**
- ✅ Player voting system for match highlights
- ✅ Vote tracking and analytics
- ✅ Fan favorites identification

### 10. **UI/UX Excellence**
- ✅ Modern Material Design 3 (Material 1.13.0)
- ✅ Brand-consistent color scheme (#E31212 primary red)
- ✅ Progress indicators for all async operations
- ✅ Empty state handling and visual feedback
- ✅ Toast-based error messaging
- ✅ Responsive layouts for all screen sizes
- ✅ Smooth animations and transitions

---

## 🏐 Supported Sports

| Sport | Scoring Type | Features |
|-------|-------------|----------|
| **Cricket** 🏏 | Innings-based | Runs, Wickets, Balls, Overs |
| **Futsal** ⚽ | Goal-based | Goals, Fouls, Time tracking |
| **Badminton** 🏸 | Rally-based | Points, Serves, Games |
| **Volleyball** 🏐 | Rally scoring | Points, Sets, Rotation |
| **Table Tennis** 🏓 | Point-based | Points, Games |
| **Ludo** 🎲 | Position-based | Rankings, Positions |
| **Chess** ♟️ | Win/Loss | W/L/D, Ratings |
| **Tug of War** 💪 | Team strength | Team scores, Attempts |

---

## 🔧 Technical Stack

### **Platform & Languages**
- **Platform:** Android 9.0+ (API 21+)
- **Primary Language:** Kotlin 2.2.10
- **Target SDK:** 36 (Android 15)
- **Compile SDK:** 36
- **Java Version:** 17

### **Architecture & Design Patterns**
- **Architecture Pattern:** MVVM (Model-View-ViewModel)
- **Repository Pattern:** Data layer abstraction
- **Coroutines Pattern:** Async/await operations
- **Dependency Injection Pattern:** Retrofit singleton

### **Core Libraries**

#### Networking
- Retrofit 3.0.0 - REST API client
- OkHttp 5.3.2 - HTTP networking with logging
- Gson 2.13.2 - JSON serialization/deserialization

#### UI Components
- Material 1.13.0 - Material Design 3 components
- ConstraintLayout 2.2.1 - Responsive layouts
- AndroidX AppCompat - Backward compatibility

#### Image Loading
- Glide 5.0.5 - Advanced image loading and caching
- Picasso 2.71828 - Image loading alternative

#### Asynchronous Programming
- Kotlin Coroutines 1.10.2 - Suspend functions, async/await
- Fragment KTX 1.8.9 - Fragment lifecycle extensions

#### Testing
- JUnit 4.13.2 - Unit testing framework
- AndroidX Test JUnit - Android instrumented testing
- Espresso 3.7.0 - UI testing framework

---

## 🏗️ Project Architecture

### **MVVM Architecture Overview**

The project follows **Model-View-ViewModel (MVVM)** architecture for clean separation of concerns:

- **View Layer:** Activities and Fragments handle UI display
- **ViewModel Layer:** Manages business logic and LiveData
- **Repository Layer:** Abstracts data sources
- **Network Layer:** Retrofit-based API integration

### **Data Flow**
1. User interaction on UI (Activity)
2. Activity calls ViewModel method
3. ViewModel launches Coroutine → Repository call
4. Repository calls ApiService (Retrofit)
5. API response → DTO mapping
6. ViewModel updates LiveData
7. UI observes and updates automatically

---

## 📦 Core Modules

### **Activities (24+ Activities)**

#### Authentication & Account Management
- `LoginActivity` - User login with credentials validation
- `AddAccountActivity` - New user registration
- `ManageAccountActivity` - Account overview
- `UpdateAccountActivity` - Profile editing

#### Tournament Management (5 Activities)
- `CreateTournamentActivity` - New tournament creation
- `EditTournamentActivity` - Tournament editing
- `TournamentDetailActivity` - Detailed view
- `TournamentOverviewActivity` - Overview with stats
- `TDetailActivity` - Additional tournament details

#### Match & Fixture Management (4 Activities)
- `CreateFixtureActivity` - Schedule new matches
- `UpdateFixtureActivity` - Modify existing matches
- `MatchesDetailActivity` - Match information
- `MatchSummaryActivity` - Post-match summary

#### Scoring System (8 Sport-Specific Activities)
- `CricketScoringActivity`, `FutsalScoringActivity`
- `BadmintionScoringActivity`, `VolleyBallScoringActivity`
- `TableTennisScoringActivity`, `LudoScoringActivity`
- `ChessScoringActivity`, `TugOfWarScoringActivity`

#### Media & Statistics (3 Activities)
- `SeasonMediaActivity` - Season media browsing
- `SportsMediaActivity` - Sport-specific galleries
- `HeavyStatsActivity` - Comprehensive player statistics

#### Sports & Seasons (4 Activities)
- `SportsActivity`, `SeasonsActivity`
- `SportsSelectionActivity`, `RequstsActivity`

#### Navigation (3 Activities)
- `HomeActivity` - Main dashboard
- `MainActivity` - App entry point
- `StartScoringActivity`, `ScorerActivity`

### **Data Transfer Objects (30+ DTOs)**

#### Authentication & Accounts
- `LoginRequest/LoginResponse`
- `AccountResponse`, `CreateAccountRequest`, `UpdateAccountRequest`

#### Tournaments
- `TournamentRequest/Response`, `TournamentUpdateRequest`
- `TournamentOverviewResponse`, `TournamentStatsDto`

#### Matches & Scoring
- `FixturesRequest/Response`, `MatchResponse`, `MatchDetail`
- `MatchSummaryDto`, `ScorecardResponse`, `Ball`

#### Players & Teams
- `PlayerDto`, `PlayerStatsDto`, `PlayerRequest/RequestDto`
- `TeamDTO`, `TeamPlayerDto`, `TeamRequest/RequestDto`

#### Media & Other
- `MediaDto`, `PtsTableDto`, `SeasonResponse`, `Sport`

### **API Service (309 Lines - 50+ Endpoints)**

Complete REST API integration covering:
- Authentication, Accounts, Tournaments, Matches
- Teams, Players, Requests, Media
- Statistics, Voting, Seasons

---

## 🔗 API Endpoints Summary

### **Base Structure**
```
POST   /account/login
POST   /tournament          GET  /tournament/{id}      PUT /tournament/{id}
POST   /match              GET  /match/{id}           PUT /match/start/{id}
GET    /match/summary/{mid} GET  /match/scoreCard/{mid}/{tid}
POST   /playerRequest       PUT  /playerRequest/approve/{id}
GET    /media/season/{id}   GET  /media/tournament/{id}
GET    /player/{id}/stats   POST /vote/{matchId}/{accountId}/{playerId}
```

**Total Endpoints:** 50+  
**Request Methods:** GET, POST, PUT, DELETE, Multipart  
**Authentication:** JWT-based

---

## 💻 Installation & Setup

### **Prerequisites**

| Requirement | Minimum | Recommended |
|------------|---------|------------|
| **Android Studio** | Arctic Fox | Latest (Koala+) |
| **JDK** | 11 | 17 |
| **Android SDK** | API 21 (5.0) | API 36 (15.0) |
| **Gradle** | 8.0+ | Latest |
| **Kotlin** | 1.8.0+ | 2.2.10 |

### **Step 1: Clone Repository**
```bash
git clone https://github.com/yourusername/FYPProject.git
cd FYPProject
```

### **Step 2: Configure local.properties**
```properties
sdk.dir=/path/to/your/android/sdk
BASE_URL=https://your-api-server.com/api/
```

### **Step 3: Build & Run**
```bash
./gradlew build          # Build project
./gradlew installDebug   # Install on device/emulator
./gradlew :app:run      # Run app
```

---

## 🚀 Build & Run

### **Debug Build**
```bash
./gradlew assembleDebug    # Create debug APK
./gradlew installDebug     # Install on device
```

### **Release Build**
```bash
./gradlew assembleRelease  # Create release APK
```

### **From Android Studio**
- **Run:** `Shift + F10` (Windows)
- **Debug:** `Shift + F9` (Windows)

---

## 📁 Project Structure

```
FYPProject/
├── app/src/main/
│   ├── java/com/example/fypproject/
│   │   ├── Activity/          (24+ Activities)
│   │   ├── DTO/               (30+ Data Models)
│   │   ├── Network/           (API Integration)
│   │   ├── Adapter/           (RecyclerView Adapters)
│   │   ├── Fragment/          (8+ Sport Fragments)
│   │   ├── Scoring/           (Sport-specific Logic)
│   │   ├── ScoringDTO/        (Scoring Models)
│   │   ├── Sockets/           (WebSocket)
│   │   └── Utils/             (Utilities)
│   ├── res/
│   │   ├── layout/           (Activities/Fragments)
│   │   ├── drawable/         (Images, Vectors)
│   │   ├── values/           (Colors, Strings, Themes)
│   │   └── mipmap/           (App Icons)
│   └── AndroidManifest.xml   (139 lines)
├── gradle/libs.versions.toml (Dependency Management)
├── build.gradle.kts          (Root Config)
├── settings.gradle.kts       (Project Settings)
└── local.properties          (Local Config - Git ignored)
```

---

## ⚙️ Configuration

### **local.properties**
```properties
sdk.dir=/path/to/android/sdk
BASE_URL=https://your-api-server.com/api/
DEBUG_MODE=true
```

### **Build Configuration**
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 36 (Android 15)
- **Compile SDK:** 36
- **Java Version:** 17
- **Kotlin:** 2.2.10

### **Permissions**
- `INTERNET` - API calls
- `ACCESS_NETWORK_STATE` - Network status
- `CAMERA` - Media capture

### **Color Scheme**
- **Primary:** #E31212 (Red)
- **Background:** #F5F5F5 (Light Gray)
- **Material Design 3** with dark theme support

---

## 📝 Development Guidelines

### **Code Standards**
```kotlin
// ✅ Follow Kotlin conventions
val tournamentDetails = getTournamentDetails(id)

// ✅ MVVM pattern
viewModelScope.launch {
    try {
        val data = repository.getTournament(id)
        _tournament.value = data
    } catch (e: Exception) {
        handleError(e)
    }
}

// ✅ ViewBinding (no findViewById!)
binding.apply {
    button.setOnClickListener { }
}
```

### **UI/UX Principles**
- Modern Material Design 3
- Progress indicators for all async operations
- User-friendly error messages
- Responsive layouts for all screen sizes
- Consistent color scheme (#E31212)

---

## 🚧 Future Enhancements

### **Phase 2: Data Persistence**
- [ ] Room database for offline caching
- [ ] Local data synchronization

### **Phase 3: Real-Time Features**
- [ ] Push notifications
- [ ] WebSocket live updates
- [ ] Real-time chat

### **Phase 4: Analytics**
- [ ] Advanced statistics
- [ ] Performance predictions
- [ ] Heat maps

### **Phase 5: Social & Engagement**
- [ ] Social sharing
- [ ] Comments system
- [ ] Achievement badges

### **Phase 6: Video & Streaming**
- [ ] Live streaming
- [ ] Match replays
- [ ] Video highlights

### **Phase 7: Technical**
- [ ] Dependency Injection (Hilt)
- [ ] Unit test coverage (>80%)
- [ ] CI/CD pipeline
- [ ] Crash analytics (Firebase)

---

## 📊 Project Statistics

| Metric | Count |
|--------|-------|
| **Activities** | 24+ |
| **Fragments** | 8+ |
| **DTOs** | 30+ |
| **API Endpoints** | 50+ |
| **Sports Supported** | 8 |
| **Lines of Code** | 10,000+ |
| **Dependencies** | 15+ |

---

## 🤝 Contributing

### **Steps**
1. Fork repository
2. Create feature branch: `git checkout -b feature/amazing-feature`
3. Follow code standards
4. Test thoroughly
5. Submit PR with description

### **Code Standards for Contributors**
- ✅ Kotlin conventions
- ✅ Meaningful names
- ✅ MVVM pattern
- ✅ Progress indicators
- ✅ Error handling
- ✅ Unit tests

---

## 📄 License

**Final Year Project (FYP)** - Computer Science Degree  
**Copyright © 2026 Hussain Asghar**

---

## 📞 Support

- **Issues:** Create GitHub issue with details
- **Email:** [your-email@example.com]
- **University:** [Your University Name]

---

**Last Updated:** April 22, 2026 | **Status:** ✅ Active Development | **Maintenance:** Actively Maintained

*This README is continuously updated as the project evolves.*
