# Spotify-Stat-API

Spring Boot API for Spotify listening analytics from Spotify-Stat-Tracker track history.

Spotify-Stat-API queries a PostgreSQL database of your complete Spotify playback history to generate advanced statistics including top tracks, artist distributions, listening heatmaps, session analytics, and niche/popular content discovery. Works with data from the companion Spotify-Stat-Tracker.
​
Example API: https://api.pugking4.dev/
Example /stats/time/rolling?endHours= query: https://api.pugking4.dev/stats/time/rolling?endHours=18
<img width="675" height="995" alt="Spotify-Stat-API-example0" src="https://github.com/user-attachments/assets/6a905791-c8bc-450a-979c-de5dfdc3e659" />

Example /stats/recently-played?limit= query: https://api.pugking4.dev/stats/recently-played?limit=5
