# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

The LibreSpeed Android template is an Android application that allows users to perform speed tests using existing LibreSpeed servers. The app supports download, upload, ping, jitter measurements, IP address detection, ISP information, distance calculation, telemetry, and results sharing. The app includes advanced features like server caching with IP prefix checking and Android TV compatibility.

The application has been enhanced with local network speed testing capabilities:
- Auto-detection of device's local IP address
- IP segment and port input fields for specifying local servers
- Direct construction of server objects from IP/port inputs
- HTTP protocol support for local network testing

## Architecture

The application follows a modular structure with the following main components:

- **Core components** (`com.fdossena.speedtest.core`):
  - Base connection utilities
  - Download functionality (`Downloader`, `DownloadStream`)
  - Upload functionality (`Uploader`, `UploadStream`)
  - Ping functionality (`Pinger`, `PingStream`)
  - IP detection (`GetIP`)
  - Server selection (`ServerSelector`, `TestPoint`)
  - Telemetry reporting (`Telemetry`)
  - Main speedtest engine (`Speedtest`)
  - Worker threads (`SpeedtestWorker`)

- **UI components** (`com.fdossena.speedtest.ui`):
  - Main activity (`MainActivity`)
  - Custom gauge view (`GaugeView`)

## Configuration

The application is configured through JSON files in `app/src/main/assets/`:
- `ServerList.json`: Defines the available test servers
- `SpeedtestConfig.json`: Speed test configuration parameters
- `TelemetryConfig.json`: Telemetry configuration
- `privacy_en.html`: Privacy policy

## Building the Project

The project uses Gradle with Android Studio. Key build properties:
- Compile SDK Version: 34
- Min SDK Version: 21 (Updated for TV compatibility)
- Target SDK Version: 34
- Package name: `dev.local.speedtest` (needs to be changed for distribution)

To build:
```bash
./gradlew build
```

To assemble debug APK:
```bash
./gradlew assembleDebug
```

To assemble release APK:
```bash
./gradlew assembleRelease
```

To clean build:
```bash
./gradlew clean
```

To install debug APK to connected device:
```bash
./gradlew installDebug
```

## Server Caching with IP Prefix Checking

The application includes intelligent server caching functionality:
- The app detects the local IP address prefix (first three octets) on startup
- If the IP prefix matches the previously cached one, it automatically loads the last successfully used server
- Server URLs are cached in SharedPreferences with keys: `server_url` and `last_successful_server_url`
- IP prefix caching ensures the appropriate server is selected when the user's network doesn't change
- This reduces the need for manual server selection in stable network environments

The relevant methods for this functionality are in MainActivity:
- `getLocalIPv4Prefix()`: Extracts the IP prefix from the device's network interfaces
- `getCachedIpprefix()` / `cacheIpprefix()`: Manage IP prefix storage
- `getLastSuccessfulServerURL()` / `cacheLastSuccessfulServerURL()`: Manage successful server URL storage
- `getCachedServerURL()` / `cacheServerURL()`: Manage general server URL storage

## Customization

To customize the application:
1. Change the package name in build.gradle and AndroidManifest.xml
2. Update the server list in `ServerList.json` with your LibreSpeed server details
3. Configure speedtest and telemetry parameters in their respective JSON files
4. Update the app icon and launcher graphics in `app/src/main/res/drawable/`
5. Modify strings in `app/src/main/res/values/strings.xml`

## Android TV Compatibility

The application has been updated for Android TV compatibility with the following changes:

### Manifest Updates
- Added leanback feature requirement for TV
- Added `LEANKBACK_LAUNCHER` intent category
- Added `android.hardware.touchscreen` as non-required feature
- Added banner attribute for TV home screen
- Set landscape orientation as preferred for TV

### Dependencies
- Added AndroidX Leanback library dependencies for TV support
- Updated minSdkVersion to 21 for proper TV support

### UI Enhancements
- Added D-pad navigation support in MainActivity (keyDown handler for TV remotes)
- Enhanced key event handling for TV remote controls
- Increased text sizes and element dimensions for better visibility on TV
- Improved color contrast for TV viewing distances
- Added focus management for TV navigation

### Resource Optimizations
- Created TV-optimized dimension resources (`values-sw600dp/dimens.xml`)
- Enhanced colors with better contrast ratios for TV viewing
- Updated themes to support TV focus states
- Added TV-specific layout resources

## Server Requirements

Requires one or more servers with LibreSpeed (https://github.com/librespeed/speedtest) installed. Server endpoints include:
- Download: garbage.php
- Upload: empty.php
- Ping: empty.php
- Get IP: getIP.php

ServerList.json can contain:
- Static server list (JSON array format)
- URL to fetch server list from (string format)

## Key Application Flow

1. Splash screen (1.5s)
2. Initialize configuration and load assets
3. Check IP prefix to determine if cached server should be used
4. Auto-detect local IP and populate IP segment fields with default values (A.B.C.10:8989)
5. Manual server selection via IP segment and port inputs
6. Execute speed test (download, upload, ping/jitter)
7. Display results and optionally share

## Local Network Testing Features

The application now supports local network speed testing:
- Automatic detection of the device's local IPv4 address on startup
- Four separate input fields for IP address segments (A.B.C.D format)
- Dedicated port input field with default value 8989
- Default auto-fill of A.B.C.10 where A.B.C are taken from the device's local IP
- Direct server object construction using IP and port inputs instead of URL
- HTTP protocol support for local network communication

## UI Elements

The server selection page now includes:
- Four EditText fields for IP address segments
- One EditText field for port number
- Dot separators between IP segments
- Colon separator between IP and port
- Validation for proper input format

## Speed Test Process

The speed test performs the following sequence:
1. Ping/jitter measurement (parallel)
2. Download test
3. Upload test
4. IP info retrieval
5. Optional telemetry submission
6. Result sharing capability

## Development Considerations

- Use `st.abort()` to cleanly stop ongoing tests
- Implement proper error handling in SpeedtestHandler callbacks
- Support for multiple test points with automatic server selection
- Configurable test sequence (D=Download, U=Upload, P=Ping, I=IP info)
- Automatic scaling of background images for performance
- Server caching with IP prefix verification for enhanced user experience
- Network security configuration supports cleartext traffic for localhost/testing
- Local IP detection and auto-population of IP segment fields

## Common Issues & Troubleshooting

- Network Security Configuration handles cleartext traffic appropriately for local network testing
- Memory management for large background images (max 16MB limit enforced)
- Proper cleanup in onDestroy() to prevent resource leaks
- Threading considerations for UI updates (use runOnUiThread)
- SharedPreference-based caching system for server selection and IP prefixes
- D-pad navigation support for TV compatibility
- Input validation for IP segments and port numbers on server selection page

## Recent Changes

- Updated to latest Gradle and Android Studio versions
- Fixed bug causing HTTP instead of HTTPS connections on dual-support servers
- Server list can be loaded from URL
- Improved auto test duration formula
- Android 11+ compatibility updates
- Enhanced security configurations
- Added server caching functionality with IP prefix checking
- Updated package name and namespace to support AGP 8+

## Network Security Configuration

The app includes permissive network security configuration to support local network testing:
- Cleartext traffic (HTTP) is permitted for all domains
- Support for HTTP protocol on local network servers
- Trust anchors include system and user certificates
- Compatible with both HTTP and HTTPS connections to local servers

## Testing

To run unit tests:
```bash
./gradlew test
```

To run instrumentation tests:
```bash
./gradlew connectedAndroidTest
```

## Package Structure Changes

With Android Gradle Plugin 8+, the app now uses:
- `namespace 'dev.local.speedtest'` in build.gradle
- Updated manifest placeholders for Android 11+ compatibility
- Packaging options exclusions for META-INF files