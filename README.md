Welcome to the first Android client for **Purple Music**! This modern, optimized application allows you to stream your music library directly from your personal Purple Music server instance.

## ✨ Key Features

### 🎵 Full Audio Experience

* **Seamless Streaming:** Powered by `Media3 ExoPlayer` for ultra-stable background network playback.
* **Advanced Controls:** Native support for Shuffle and Repeat modes (*Repeat All*, *Repeat One*).
* **Dynamic Queue:** View and manage your upcoming track queue at any time.
* **Synced Lyrics:** Auto-fetched, line-by-line synced lyrics with live highlighting as the track plays.
* **Audio Equalizer & Visualizer:** Fine-tune bass/treble to your taste and enjoy real-time animations reacting to the beat.
* **Sleep Timer:** Schedule playback to stop automatically so you don't drain your battery overnight.
* **Offline Downloads:** Download tracks for playback without an internet connection.
* **Chromecast & Android Auto:** Cast playback to a Chromecast device, or connect the app to your car's head unit.

### 🎨 Modern Interface (Jetpack Compose)

* **Sleek, Themeable UI:** Choose from several color presets, or let **Material You** pull colors straight from your wallpaper — all applied consistently across the entire app.
* **Home Screen:** Curated sections (recent adds, most played, your mixes) instead of one flat list.
* **Mini-Player & Full-Player:** Transition smoothly between a persistent bottom mini-player and an animated full-screen player view.
* **Playlist Detail View:** Tapping into a playlist (*Mix*) shows its tracks first — nothing starts playing until you pick a song or hit "Play all".
* **Multi-language:** Full interface translation — French, English, Spanish, and German — picked up automatically from your device language.
* **Guided Setup:** A short first-launch walkthrough (with its own background music and sound effects) gets new accounts oriented in seconds.

### 🏠 System Integration & Home Widget

* **Background Service:** Music playback continues smoothly even when the app is minimized or the screen is turned off.
* **Interactive Home Widget (Android Glance):** Control your playback (Play/Pause, Next, Previous, Shuffle, Repeat) and track your listening progress directly from your phone's home screen — resizable down to a compact size.

### 🛠️ Administration & Preferences

* **Genre Filtering:** Hide specific music genres you don't want to see in your main feed.
* **On-the-go Track Editing:** Modify metadata (title, artist, genre), update cover art, or delete tracks (available for admins and original uploaders).
* **Remote Uploading:** Publish new MP3 files and their artwork to your server directly from the Android app.

---

## 🔧 Initial Setup

On first launch, the app will prompt you to configure your personal Purple Music server URL:

1. Enter your API endpoint address (e.g., `https://your-server.com/music/`).
2. Register a new account or log in with your existing credentials.
3. Your preferences (theme, app volume, default sorting, hidden genres) are saved locally and securely.

## 📝 License

This project is licensed under the **MIT License**. Feel free to use, modify, and distribute it.
