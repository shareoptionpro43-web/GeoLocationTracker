# 📍 Geo Location Tracker — OpenStreetMap Edition

A full-featured Android GPS tracker using **OpenStreetMap (OSMDroid)** — completely free, no API key required.

## ✨ Features
- **Live GPS tracking** with configurable intervals (1–60s)
- **OpenStreetMap maps** via OSMDroid — offline-capable, free forever
- **Nominatim geocoding** — converts coordinates to addresses (OSM's free geocoder)
- **Route polyline** drawn on map with start/current markers
- **Geofencing** — draw zones on OSM map, get notified on enter/exit
- **Speedometer dashboard** with max/avg speed, distance, duration
- **CSV Export** — share full history as spreadsheet
- **Share location** — send OSM/Google Maps link
- **Room database** — all history stored locally
- **Background foreground service** — tracks with screen off
- **Auto-start on boot** (optional)
- **Dark themed UI**

---

## 🗺️ OpenStreetMap — No API Key Needed!
This app uses:
- **OSMDroid** for map tiles → completely free, no signup
- **Nominatim** for reverse geocoding → OSM's free geocoder (no key)

You don't need to configure anything map-related. Just build and run!

---

## 🚀 Get Your APK via GitHub Actions (Free)

### Step 1 — Push to GitHub
```bash
git init
git add .
git commit -m "Initial commit"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/GeoLocationTracker.git
git push -u origin main
```

### Step 2 — Download APK
1. Go to your repo on GitHub
2. Click **Actions** tab
3. Click the latest workflow run
4. Download **GeoLocationTracker-DEBUG** artifact

> The workflow auto-runs on every push and produces a signed debug APK.

---

## 📲 Install on Android
1. Transfer APK to phone
2. **Settings → Security → Install unknown apps → Allow**
3. Open APK → Install → Done

---

## 🛠 Build Locally (Android Studio)
1. Open Android Studio → File → Open → this folder
2. Click **▶ Run** — no API key needed!
3. APK at: `app/build/outputs/apk/debug/app-debug.apk`

---

## 📋 Permissions
| Permission | Purpose |
|---|---|
| `ACCESS_FINE_LOCATION` | Precise GPS |
| `ACCESS_BACKGROUND_LOCATION` | Track with screen off |
| `INTERNET` | OSM map tiles + Nominatim geocoding |
| `FOREGROUND_SERVICE` | Keep service alive |
| `POST_NOTIFICATIONS` | Geofence alerts |
| `RECEIVE_BOOT_COMPLETED` | Auto-start on reboot |

---

## 📁 Key Files
```
service/LocationTrackingService.kt  ← GPS + Nominatim geocoding
ui/MapActivity.kt                   ← OSMDroid map + route
ui/GeofenceActivity.kt              ← OSMDroid geofence zones
ui/DashboardActivity.kt             ← Speedometer + stats
utils/CsvExporter.kt                ← CSV export/share
utils/GeofenceHelper.kt             ← Geofence management
data/                               ← Room DB (Entity/Dao/Repo)
```
