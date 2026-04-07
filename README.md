# FYP Project - Sports Management System

A comprehensive sports management Android application built with Kotlin and modern Android development practices.

## 🏆 Features

### Core Functionality
- **User Authentication**: Secure login system with role-based access
- **Tournament Management**: Create, manage, and organize tournaments
- **Fixture Management**: Schedule and manage sports fixtures
- **Player Statistics**: Detailed player stats and performance tracking
- **Media Management**: Upload and view sports media content
- **Request System**: Player and team request approvals

### Sports Supported
- Cricket
- Futsal  
- Badminton
- Volleyball
- Table Tennis
- Ludo
- Chess
- Tug of War

## 🎨 UI/UX Features
- **Modern Material Design**: Clean and intuitive interface
- **Progress Indicators**: E31212 themed progress bars for all async operations
- **Responsive Layouts**: Optimized for various screen sizes
- **Error Handling**: User-friendly toast messages and error states

## 📱 Activities

### Main Activities
- `LoginActivity` - User authentication with progress indicators
- `HomeActivity` - Main dashboard and navigation hub
- `HeavyStatsActivity` - Detailed player statistics with loading states

### Management Activities
- `CreateTournamentActivity` - Tournament creation with form validation
- `CreateFixtureActivity` - Fixture scheduling and management
- `EditTournamentActivity` - Tournament editing capabilities
- `UpdateFixtureActivity` - Fixture modification interface

### Media Activities
- `SeasonMediaActivity` - Season-wise media browsing with pagination
- `SportsMediaActivity` - Sport-specific media galleries
- `MediaViewerActivity` - Full-screen media viewing

### User Management
- `ManageAccountActivity` - Account management interface
- `UpdateAccountActivity` - Profile editing
- `AddAccountActivity` - New user registration
- `RequestsActivity` - Request approval system

### Sports & Seasons
- `SportsActivity` - Sports overview and selection
- `SeasonsActivity` - Season management
- `TournamentDetailActivity` - Tournament information
- `MatchesDetailActivity` - Match details and scoring

## 🔧 Technical Implementation

### Architecture
- **MVVM Pattern**: Clean separation of concerns
- **Repository Pattern**: Data layer abstraction
- **Coroutines**: Asynchronous programming
- **Retrofit**: REST API integration
- **ViewBinding**: Type-safe view binding

### Key Libraries
```kotlin
// Networking
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'

// UI Components
implementation 'com.google.android.material:material:1.8.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

// Architecture
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.6.2'

// Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
```

### Color Scheme
- **Primary Color**: `#E31212` (Red accent)
- **Progress Bars**: Consistent E31212 theming
- **Background**: `#F5F5F5` (Light gray)
- **Text**: Standard black/white contrast

## 🚀 Recent Improvements

### Completed Match Summary Routing
- ✅ Completed and abandoned match cards now open the summary screen directly
- ✅ Live matches continue to open the scoring screen
- ✅ Status checks are normalized to handle case/spacing differences from the backend

### Progress Bar Implementation
- ✅ Added progress bars to all async operations
- ✅ Consistent E31212 color scheme across all progress indicators
- ✅ Proper loading states in `HeavyStatsActivity`, `SeasonMediaActivity`, `SportsMediaActivity`
- ✅ XML layout updates for progress bar integration

### Error Handling Enhancement
- ✅ Improved Toast message implementations
- ✅ User-friendly error messages instead of raw exceptions
- ✅ Consistent error handling patterns across activities

### Code Quality
- ✅ Clean async/await patterns with coroutines
- ✅ Proper resource management with try-catch-finally blocks
- ✅ Type-safe view binding implementation

## 📁 Project Structure

```
app/
├── src/main/
│   ├── java/com/example/fypproject/
│   │   ├── Activity/          # All Activity classes
│   │   ├── Adapter/           # RecyclerView adapters
│   │   ├── DTO/               # Data Transfer Objects
│   │   ├── Fragment/          # Fragment classes
│   │   ├── Network/           # API interfaces and instances
│   │   └── Utils/             # Utility classes and extensions
│   ├── res/
│   │   ├── layout/           # XML layout files
│   │   ├── values/           # Colors, strings, themes
│   │   └── drawable/         # Vector assets and images
│   └── AndroidManifest.xml
├── build.gradle.kts          # Module-level build configuration
└── gradle.properties         # Project properties
```

## 🔑 API Integration

### Endpoints
- Authentication: `/api/auth/*`
- Tournaments: `/api/tournaments/*`
- Fixtures: `/api/fixtures/*`
- Players: `/api/players/*`
- Media: `/api/media/*`

### Data Models
- `LoginRequest/Response`
- `TournamentRequest`
- `FixturesRequest`
- `PlayerStatsDto`
- `MediaDto`

## 🎯 Development Guidelines

### Code Standards
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Implement proper error handling
- Add progress indicators for all async operations

### UI/UX Principles
- Consistent Material Design implementation
- E31212 color scheme for primary actions and progress
- User-friendly error messages
- Responsive layouts for different screen sizes

## 🚧 Future Enhancements

### Planned Features
- [ ] Offline data caching with Room database
- [ ] Push notifications for match updates
- [ ] Advanced statistics and analytics
- [ ] Social features (sharing, comments)
- [ ] Live scoring integration
- [ ] Video streaming for matches

### Technical Improvements
- [ ] Dependency Injection with Hilt
- [ ] Unit and UI testing implementation
- [ ] CI/CD pipeline setup
- [ ] Code coverage metrics
- [ ] Performance optimization

## 📱 Build & Run

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 21+ (Android 5.0)
- Kotlin 1.8.0+

### Build Steps
1. Clone the repository
2. Open in Android Studio
3. Update `local.properties` with your paths
4. Build and run the project

### Configuration
```properties
# local.properties
sdk.dir=/path/to/your/android/sdk
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Implement your changes
4. Add proper error handling and progress indicators
5. Test thoroughly
6. Submit a pull request

## 📄 License

This project is part of Final Year Project (FYP) for Computer Science degree.

---

**Note**: This README will be continuously updated as the project evolves. Last updated: January 2026
