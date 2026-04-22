# 🚀 QUICK REFERENCE GUIDE - FYP Sports Management System

**For Quick Lookup | API & Development Reference**

---

## 📚 Table of Contents

- [Project Quick Facts](#project-quick-facts)
- [Quick Setup](#quick-setup)
- [API Quick Reference](#api-quick-reference)
- [File Structure](#file-structure)
- [Key Activities](#key-activities)
- [Common Tasks](#common-tasks)
- [Troubleshooting](#troubleshooting)

---

## 📊 Project Quick Facts

| Property | Value |
|----------|-------|
| **App Name** | FYP Sports Management System |
| **Package** | com.example.fypproject |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 36 (Android 15) |
| **Language** | Kotlin 2.2.10 |
| **Architecture** | MVVM |
| **Build System** | Gradle |
| **Total Activities** | 24+ |
| **Total DTOs** | 30+ |
| **Sports** | 8 |
| **Status** | ✅ Production Ready |

---

## ⚡ Quick Setup

### 1. Clone & Configure
```bash
git clone <repo-url>
cd FYPProject
```

### 2. Update local.properties
```properties
sdk.dir=C:\\Users\\[YourUsername]\\AppData\\Local\\Android\\Sdk
BASE_URL=https://your-api-server.com/api/
```

### 3. Build & Run
```bash
./gradlew build
./gradlew installDebug
```

**Or use Android Studio:**
- `Shift + F10` → Run
- `Shift + F9` → Debug

### 4. Check Prerequisites
- ✅ Android Studio (Latest)
- ✅ JDK 17
- ✅ Android SDK 36
- ✅ Gradle 8.0+

---

## 🔗 API Quick Reference

### Base URL
```
https://your-api-server.com/api/
```

### Core Endpoints

#### Authentication
```
POST   /account/login                        → LoginResponse
```

#### Accounts
```
POST   /account                              → AccountResponse
GET    /account                              → List<AccountResponse>
GET    /account/{id}                         → AccountResponse
PUT    /account/{id}                         → Update account
DELETE /account/{id}                         → Delete account
```

#### Tournaments
```
POST   /tournament                           → Create tournament
GET    /tournament/{id}                      → Get tournament
PUT    /tournament/{id}                      → Update tournament
GET    /tournament/overview/{id}             → TournamentOverviewResponse
GET    /tournament/namesAndIds               → List<Map<Long, String>>
```

#### Matches
```
POST   /match                                → Create match
GET    /match/{id}                           → Get match
GET    /match/sport?name=&status=            → Matches by sport
GET    /match/tournament/{id}                → Tournament matches
GET    /match/summary/{mid}                  → MatchSummaryDto
GET    /match/scoreCard/{mid}/{tid}          → ScorecardResponse
GET    /match/balls/{mid}/{tid}              → List<Ball>
PUT    /match/start/{id}                     → Start match
PUT    /match/abandon/{id}                   → Abandon match
```

#### Teams
```
POST   /team/{id}/{playerId}                 → Create team
GET    /team/tournament/{id}                 → Tournament teams
GET    /team/{teamId}/players                → Team players
GET    /team/tournament/account/{tid}/{aid}  → Team by tournament & account
```

#### Players
```
GET    /player/{playerId}/stats              → PlayerStatsDto
PUT    /player/{id}                          → Update player
GET    /account/players/{tid}                → Team players
```

#### Requests
```
POST   /playerRequest                        → Create request
GET    /playerRequest/player/{id}            → Player requests
PUT    /playerRequest/approve/{id}           → Approve
PUT    /playerRequest/reject/{id}            → Reject

POST   /teamRequest                          → Create request
GET    /teamRequest                          → All requests
PUT    /teamRequest/approve/{id}             → Approve
PUT    /teamRequest/reject/{id}              → Reject
```

#### Media
```
POST   /media                                → Upload (Multipart)
GET    /media/season/{id}/{page}/{size}      → Season media
GET    /media/tournament/{id}/{page}/{size}  → Tournament media
GET    /media/sport/{id}/{page}/{size}       → Sport media
```

#### Statistics
```
GET    /ptsTable/tournament/{id}             → Points table
GET    /tournament/{id}/stats                → Tournament stats
```

#### Seasons
```
POST   /season                               → Create season
GET    /season/names                         → Season names
GET    /season/{id}                          → Season by ID
POST   /add-sports                           → Add sports to season
GET    /season/tournaments/{id}/{sid}        → Season tournaments
```

#### Engagement
```
POST   /vote/{matchId}/{accountId}/{playerId} → Submit vote
```

---

## 📁 File Structure Quick Map

```
app/src/main/java/com/example/fypproject/

Activity/
├── LoginActivity.kt              ← Login screen
├── HomeActivity.kt               ← Main dashboard
├── CreateTournamentActivity.kt   ← Tournament creation
├── CreateFixtureActivity.kt      ← Match scheduling
├── MatchSummaryActivity.kt       ← Match results
├── HeavyStatsActivity.kt         ← Player statistics
├── SeasonMediaActivity.kt        ← Media gallery
├── Scoring/
│   ├── CricketScoringActivity.kt
│   ├── FutsalScoringActivity.kt
│   ├── BadmintionScoringActivity.kt
│   ├── VolleyBallScoringActivity.kt
│   ├── TableTennisScoringActivity.kt
│   ├── LudoScoringActivity.kt
│   ├── ChessScoringActivity.kt
│   └── TugOfWarScoringActivity.kt
└── ... (24+ total)

DTO/                             ← Data models
├── LoginRequest.kt
├── TournamentRequest.kt
├── MatchResponse.kt
├── PlayerStatsDto.kt
├── MediaDto.kt
└── ... (30+ total)

Network/
├── ApiService.kt                ← 50+ endpoints
├── RetrofitInstance.kt          ← Singleton
└── ApiClient.kt                 ← Configuration

Fragment/
├── CricketFragment.kt
├── FutsalFragment.kt
├── BadmintonFragment.kt
└── ... (8+ sports)

Adapter/
├── TournamentAdapter.kt
├── PlayerAdapter.kt
├── MatchAdapter.kt
└── ...

Scoring/
├── CricketScoring.kt
├── FutsalScoring.kt
└── ... (Sport logic)

Utils/
├── Constants.kt
├── Extensions.kt
└── Helpers.kt
```

---

## 🎯 Key Activities Reference

### Login Flow
```
MainActivity
  ↓
LoginActivity
  ↓
HomeActivity
```

### Tournament Flow
```
HomeActivity
  ↓
SeasonsActivity
  ↓
SportsActivity
  ↓
TournamentDetailActivity
```

### Match Scoring Flow
```
TournamentDetailActivity
  ↓
MatchesDetailActivity
  ↓
StartScoringActivity
  ↓
[Sport-Specific Scoring Activity]
  ↓
MatchSummaryActivity
```

### Player Stats Flow
```
HomeActivity
  ↓
HeavyStatsActivity
  ↓
[Display player stats with loading]
```

### Media Flow
```
HomeActivity
  ↓
SeasonMediaActivity / SportsMediaActivity
  ↓
[Display media with pagination]
```

---

## 🛠️ Common Tasks

### Add New Endpoint

1. **Define DTO in `DTO/` folder**
```kotlin
data class NewFeatureRequest(
    val id: Long,
    val name: String,
    val status: String
)
```

2. **Add to ApiService.kt**
```kotlin
@POST("endpoint")
suspend fun createNewFeature(
    @Body request: NewFeatureRequest
): Response<NewFeatureResponse>
```

3. **Use in Activity/ViewModel**
```kotlin
val response = ApiClient.apiService.createNewFeature(request)
```

### Add New Activity

1. **Create Activity file**
```kotlin
// NewActivity.kt
class NewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNewBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
```

2. **Create layout XML**
```xml
<!-- res/layout/activity_new.xml -->
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout...>
    <!-- Your layout -->
</androidx.constraintlayout.widget.ConstraintLayout>
```

3. **Register in AndroidManifest.xml**
```xml
<activity
    android:name=".Activity.NewActivity"
    android:exported="false" />
```

### Handle API Error

```kotlin
try {
    val response = apiService.getDataCall()
    if (response.isSuccessful) {
        val data = response.body()
        // Process data
    } else {
        showError("Server Error: ${response.code()}")
    }
} catch (e: HttpException) {
    showError("Network Error: ${e.message}")
} catch (e: Exception) {
    showError("Unexpected Error: ${e.message}")
}
```

### Show Progress Bar

```kotlin
binding.progressBar.visibility = View.VISIBLE
binding.progressBar.setIndicatorColor(
    ContextCompat.getColor(this, R.color.brand_red)
)

// After completion
binding.progressBar.visibility = View.GONE
```

### Load Image with Glide

```kotlin
Glide.with(this)
    .load(imageUrl)
    .placeholder(R.drawable.placeholder)
    .error(R.drawable.error)
    .into(binding.imageView)
```

---

## 📊 Data Models Quick Reference

### Authentication
```
LoginRequest:      email, password
LoginResponse:     token, userId, role, email
```

### Tournament
```
TournamentRequest:      name, description, sportId, seasonId
TournamentResponse:     id, name, status, teams, matches
```

### Match
```
FixturesRequest:        tournament, team1, team2, date, time
FixturesResponse:       id, status, score, winner
MatchResponse:          id, match, status, teams, scorecard
```

### Player
```
PlayerDto:              id, name, email, sport, stats
PlayerStatsDto:         runs, wickets, goals, points, avg
```

### Team
```
TeamDTO:                id, name, players, tournament
TeamPlayerDto:          playerId, name, stats
```

### Media
```
MediaDto:               id, url, type, tournament, season, date
```

---

## 🎮 Sports Scoring Reference

| Sport | Key Fields | Calculation |
|-------|-----------|-------------|
| **Cricket** | Runs, Balls, Wickets | Runs/Balls, Strike Rate |
| **Futsal** | Goals, Fouls, Time | Goal Count |
| **Badminton** | Points, Games, Serves | Points/Games |
| **Volleyball** | Points, Sets, Rotation | Points/Sets |
| **Table Tennis** | Points, Games | Points/Games |
| **Ludo** | Positions, Ranks | Position Order |
| **Chess** | Win/Loss, Draws | Points (W=1, D=0.5) |
| **Tug of War** | Team Score | Total Score |

---

## 🚀 Build Commands Quick List

```bash
# Build
./gradlew build                 # Full build
./gradlew clean build          # Clean + build
./gradlew assembleDebug        # Debug APK

# Install
./gradlew installDebug         # Install debug APK
./gradlew installRelease       # Install release APK

# Run
./gradlew :app:run            # Run app

# Dependencies
./gradlew dependencies         # Show dependencies
./gradlew dependency-tree      # Dependency tree

# Gradle Info
./gradlew --version           # Gradle version
./gradlew tasks               # All tasks
```

---

## 🔧 Troubleshooting

### Build Issues

#### ❌ "BASE_URL missing in local.properties"
**Solution:** Add to local.properties
```properties
BASE_URL=https://your-api-server.com/api/
```

#### ❌ "SDK path not found"
**Solution:** Check local.properties
```properties
sdk.dir=C:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
```

#### ❌ "Gradle sync failed"
**Solution:**
```bash
./gradlew clean
./gradlew build
```

### Runtime Issues

#### ❌ App crashes on API call
**Solution:** Check:
- BASE_URL in local.properties
- Network connectivity
- API endpoint validity
- DTO mapping correctness

#### ❌ Progress bar not showing
**Solution:**
```kotlin
binding.progressBar.visibility = View.VISIBLE
// Make sure layout includes ProgressBar
```

#### ❌ Image not loading
**Solution:**
```kotlin
// Add permissions to AndroidManifest.xml
<uses-permission android:name="android.permission.INTERNET" />
```

#### ❌ ViewBinding not working
**Solution:** Ensure in build.gradle.kts:
```kotlin
buildFeatures {
    viewBinding = true
}
```

---

## 📱 Testing Quick Guide

### Run on Emulator
```bash
# List emulators
emulator -list-avds

# Start emulator
emulator -avd [emulator_name]

# Run app
./gradlew installDebug
```

### Run on Device
```bash
# Enable USB debugging
# Connect device
adb devices          # Check connection
./gradlew installDebug
```

### Test Specific Activity
```bash
# In Android Studio
# Select activity → Run (Shift + F10)
```

---

## 📞 Support & Resources

### Documentation
- [README.md](README.md) - Full project documentation
- [API Endpoints](#api-quick-reference) - All endpoints
- [AndroidManifest.xml](app/src/main/AndroidManifest.xml) - All activities

### External Resources
- [Android Dev Docs](https://developer.android.com)
- [Retrofit Documentation](https://square.github.io/retrofit)
- [Kotlin Docs](https://kotlinlang.org/docs)
- [Material Design 3](https://m3.material.io)

---

## 🎯 Next Steps

1. ✅ Set up local.properties
2. ✅ Build project successfully
3. ✅ Run on emulator/device
4. ✅ Test login flow
5. ✅ Explore tournament creation

---

## 📝 Version Info

| Component | Version |
|-----------|---------|
| **Gradle** | 8.0+ |
| **Kotlin** | 2.2.10 |
| **Android Studio** | Latest |
| **Retrofit** | 3.0.0 |
| **Material** | 1.13.0 |
| **Coroutines** | 1.10.2 |

---

**Last Updated:** April 22, 2026  
**Status:** ✅ Production Ready  
**Keep handy for quick reference!**

---

*For detailed information, refer to README.md*

