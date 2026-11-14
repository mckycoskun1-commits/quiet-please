# Mod sayfası taslağı (Modrinth / CurseForge)

> Modun adı **Quiet Please**, mod kimliği `quietplease`. Önerilen Modrinth adresi: `modrinth.com/mod/quiet-please`.

## Sayfa ayarları

| Alan | Değer |
|---|---|
| Proje türü | Mod |
| Yükleyici | Fabric, Forge, NeoForge |
| Minecraft sürümü | 1.18.2, 1.19.2, 1.20.1 (Fabric, Forge); 1.21.1, 26.1.2, 26.2 ve 26.3 (Fabric, Forge, NeoForge); Fabric sürümleri Quilt ile de çalışır |
| Ortam (environment) | Client: **required** · Server: **unsupported** (sadece istemci) |
| Kategoriler | **Utility**, **Management** |
| Etiket önerileri | music, sound, volume, audio, modpack, client-side |
| İkon | `art/jukebox-icon/quietplease-icon-1024.png` (1024 px) |
| Galeri | `docs/screenshots/panel-en.png`, `docs/screenshots/options-button.png`, `docs/screenshots/pause-menu.png`; açıklamadaki görseller gh-pages img/ klasöründen gelir (panel-en, playlist-suggestions, pause-menu-now-playing, song-popup, popup-settings, music-behavior, settings, options-button) |
| Lisans | **MIT** (`LICENSE` dosyası) |

## Kısa açıklama (summary, ~100 karakter)

> Mute or turn down any single music track in your modpack. Per-track volume, per-mod toggle, preview.

## Uzun açıklama (İngilizce – sayfanın ana metni)

```markdown
# Quiet Please

## Explore the mod here
[Quiet Please website](https://mckycoskun1-commits.github.io/quiet-please/)

Big modpacks bring a lot of music – and not all of it fits together.
**Quiet Please** lets you decide, track by track, what you actually want to hear.

## Every music track in one list
![The Quiet Please panel with every music track grouped by mod](https://mckycoskun1-commits.github.io/quiet-please/img/panel-en.png)

All the music in your game, vanilla and modded, grouped by the mod it comes from. On every row:
- **On / Off** – turn a track off and the game simply picks another one instead
- **Volume** – set each track anywhere from 0 to 100 %
- **Mod volume** – one slider in each mod header turns all songs of that mod up or down at once
- **Play button** – preview the track right in the panel (background music pauses while you listen)
- **Star** – keep the song in a Favorites group at the top of the list

On top of the list:
- **Enable all / Disable all** – switch off every track of a mod with one click
- **Search box, Mod filter and Ambient** – find a track fast, or also show the long ambience loops
- **Now playing bar** – the song you hear right now, with a **Skip** button next to it

## Your own background playlist
![The playlist: queue on the left, suggested songs on the right](https://mckycoskun1-commits.github.io/quiet-please/img/playlist-suggestions.png)

Press **Playlist** and pick the songs and their order, like a music player.
- Every song in the queue has **play now, move up, move down, loop and remove** buttons
- A song leaves the queue once it has played; turn on its **loop** button to keep it in its place
- When the queue reaches the end, it starts again from the top
- **Suggested** on the right: your favorites, songs you added before and well-known Minecraft songs. One click adds a song to the end of the queue, and hovering shows why it was suggested
- **Shuffle** and **Clear** at the bottom
- Boss, dragon and credits music still interrupt the playlist

## Always know what is playing
![The current song shown under the Quiet Please button in the pause menu](https://mckycoskun1-commits.github.io/quiet-please/img/pause-menu-now-playing.png)

Press Esc and the current song is right under the **Quiet Please** button, so you can find a song even if you don't know its name. One click opens the panel.

## A popup when a new song starts
![A small card in the corner of the screen showing the new song and its mod](https://mckycoskun1-commits.github.io/quiet-please/img/song-popup.png)

When a new song starts, a small card slides in with the song name and the mod it comes from, then slides out again.

![The popup settings screen with a large framed example card](https://mckycoskun1-commits.github.io/quiet-please/img/popup-settings.png)

- **Six positions**, distance from the edge, size and how long it stays
- **Song and mod name colors**, bold or italic text, shadow and background
- The icon, the mod name and the little equalizer can each be turned off
- **Drag the example card** in the popup settings to put it exactly where you want it

## Music that fits the moment
![The Music behavior screen](https://mckycoskun1-commits.github.io/quiet-please/img/music-behavior.png)

- **Jukebox** – background music turns down or mutes while a music disc plays nearby, and comes back when the disc ends
- **In background** – turn the music down when you Alt+Tab out of the game
- **In combat** – turn the music down when you take damage or a hostile mob is close, so you can hear the fight
- **Fade in/out** – songs start and end softly instead of cutting in and out
- **No repeats** – the last 5 or 10 songs are skipped when the game picks the next one
- **Favorites** – songs with a star come up 2, 3 or 5 times as often, even without the playlist
- **Fade on skip** – Skip turns the song down smoothly; press it again to stop it at once

## Shorter silence between songs
![The Quiet Please settings screen](https://mckycoskun1-commits.github.io/quiet-please/img/settings.png)

- **Music gap** – Vanilla, Short, None or Custom with your own shortest and longest wait. It only changes background music; dragon, credits and boss music are not affected
- Turn the now-playing text, the menu buttons or the song popup off
- Turn playlist mode on or off, and choose whether ambient tracks show in the list
- **Popup settings...** and **Music behavior...** open the two screens above

## Open it from anywhere
![The Quiet Please button in the Options screen](https://mckycoskun1-commits.github.io/quiet-please/img/options-button.png)

- **Esc → ♪ Quiet Please** (top-right corner)
- **Options → ♪ Quiet Please** or **Options → Music & Sounds → ♪ Quiet Please**
- **Mods → Quiet Please → Config**
- **Key bindings** (unbound by default) to open the panel, skip a song, favorite the current song and turn playlist mode on or off: Controls → Key Binds → Quiet Please
- **Chat commands**: `/quietplease`, `/quietplease skip`, `/quietplease mod <mod> volume <0-100>`, and `/quietplease export <name>` / `import <name>` to share a setup

## Supported versions
- **Minecraft 1.18.2** – Fabric, Forge
- **Minecraft 1.19.2** – Fabric, Forge
- **Minecraft 1.20.1** – Fabric, Forge
- **Minecraft 1.21.1** – Fabric, Forge, NeoForge
- **Minecraft 26.1.2** – Fabric, Forge, NeoForge
- **Minecraft 26.2** – Fabric, Forge, NeoForge
- **Minecraft 26.3** – Fabric, Forge, NeoForge
- The Fabric versions need **Fabric API** and also run on **Quilt** (with Fabric API)

## Good to know
- **Client-side only.** Install it on your own game; servers don't need it, and you can join any server.
- Works for vanilla music, music discs and modded boss and biome music – for example the boss themes of L_Ender's Cataclysm and Mowzie's Mobs.
- Settings are stored in `config/quietplease.json`, so modpack makers can ship a pre-tuned file.
- English, Turkish, German, Spanish, Russian, Brazilian Portuguese and Simplified Chinese.

## AI disclosure
The code of this mod was written with the help of an AI assistant (Claude), and the project icon is rendered from Minecraft's own jukebox and music disc textures. All screenshots on this page are real in-game screenshots.
```

## Uzun açıklama (Türkçe – istersen ikinci dil olarak)

```markdown
# Quiet Please

Büyük mod paketleri bir sürü müzik getirir ve hepsi birbirine uymaz.
**Quiet Please** ile neyi duymak istediğine parça parça sen karar verirsin.

## Özellikler
- **Tüm müzikler tek listede**, geldikleri moda göre gruplanmış
- İstediğin parçayı **kapat** – oyun onun yerine başka bir parça seçer
- Her parça için **%0–100 ses ayarı**, her mod için de ayrı bir **mod sesi** kaydırıcısı
- Bir modun **tüm müziklerini tek tıkla kapat**
- **Şarkı bildirimi** – yeni şarkı başlayınca şarkının ve modun adını gösteren küçük bir kart; yeri, boyutu, renkleri ve yazı stili ayarlanır, örnek kart sürüklenerek yerleştirilir
- **Müzik davranışı** – yakında plak çalarken, oyun arka plandayken ya da savaşta müziği kıs; şarkıları yumuşak başlat ve bitir, son çalınanları tekrar etme, favorileri daha sık çal
- Panelde **dinleyerek önizle** (dinlerken arka plan müziği susar)
- **Arka plan çalma listesi** – şarkıları ve sırasını sen seç, bir müzik çalar gibi: ilerleme çubuklu sıra, yukarı/aşağı taşıma, karıştırma, tekrar. Boss, ejderha ve jenerik müzikleri yine araya girer
- Çalan arka plan şarkısını **atla** (düğme, `/quietplease skip` ya da seçtiğin bir tuş)
- Paneli açmak, şarkı atlamak, çalan şarkıyı favorilere eklemek ve çalma listesi modunu açıp kapatmak için **kısayol tuşları** (varsayılan olarak boş)
- **Şarkı arası** – arka plan şarkıları arasındaki uzun sessizliği kısalt ya da kaldır
- **Ayarlar ekranı** – menü düğmelerini ya da çalan şarkı yazısını gizle
- **Hazır ayarlar** – `/quietplease export <ad>` / `import <ad>` ile ayarları paylaş
- Arama, mod filtresi ve uzun ortam sesleri için ayrı görünüm
- Vanilla müzikler, plaklar ve modların boss/biyom müzikleriyle çalışır
- İngilizce, Türkçe, Almanca, İspanyolca, Rusça, Brezilya Portekizcesi ve Basitleştirilmiş Çince

## Desteklenen sürümler
- **Minecraft 1.18.2** – Fabric, Forge
- **Minecraft 1.19.2** – Fabric, Forge
- **Minecraft 1.20.1** – Fabric, Forge
- **Minecraft 1.21.1** – Fabric, Forge, NeoForge
- **Minecraft 26.1.2** – Fabric, Forge, NeoForge
- **Minecraft 26.2** – Fabric, Forge, NeoForge
- **Minecraft 26.3** – Fabric, Forge, NeoForge
- Fabric sürümleri **Fabric API** ister ve **Quilt** üzerinde de çalışır (Fabric API ile)

## Nasıl açılır
**Ayarlar → ♪ Quiet Please** (sağ üst köşe), **Ayarlar → Müzik ve Sesler → ♪ Quiet Please**
ya da **Modlar → Quiet Please → Yapılandırma**. Sohbette `/quietplease` komutu da var.

## Bilmen gerekenler
- **Sadece istemci tarafı.** Sunucuya kurulması gerekmez, her sunucuya girebilirsin.
- Ayarlar `config/quietplease.json` dosyasında; mod paketi yapanlar hazır ayarlı bir dosya ekleyebilir.
```

## 1.3.0 sürüm notu (14.11.2026'da 18 dosyayla yayınlandı)

```markdown
## 1.3.0

- Song popup: a small card shows the song and its mod when a new song starts.
- Popup settings screen: position, size, duration, colors, text style, shadow, background, icon, mod name and equalizer. Drag the example card to put the popup where you want it.
- Music behavior screen: mute or lower background music near a playing jukebox, while the game is in the background or in combat. Fade songs in and out, skip recently played songs and pick favorites more often.
- Fade on skip: Skip turns the song down smoothly, a second press skips at once.
- Volume slider for each mod in the panel, and `/quietplease mod <mod> volume`.
- New keys: favorite the current song and turn playlist mode on or off.
- New languages: German, Spanish, Russian, Brazilian Portuguese and Simplified Chinese.
```

## 1.1.0 sürüm notu (taslak, `improvements` dalı – henüz yayınlanmadı)

```markdown
## 1.1.0 – Playlist and settings
- Background playlist with its own queue panel (play now, move, remove, shuffle, repeat modes, progress bar)
- Skip button in the panel and /quietplease skip
- Optional key bindings: open the panel, skip the current song (Controls → Key Binds → Quiet Please)
- Settings are saved safely: an unreadable config is kept as quietplease.json.broken instead of being overwritten
- The panel header now shows the vanilla jukebox item instead of a separate logo image
- Settings screen: menu buttons on/off, now-playing text on/off, ambient default, music gap (Vanilla / Short / None / Custom)
- Presets: /quietplease export <name> and /quietplease import <name>
- Long track names no longer run past the now-playing bar
```

## İlk sürüm notu (changelog)

```markdown
## 1.0.0 – First release
- Per-track volume and on/off for every music track, grouped by mod
- In-game panel with search, mod filter and preview
- Buttons in Options and Music & Sounds, plus /quietplease chat commands
- English and Turkish translations
```

## Ad geçmişi

İlk adı **Music Box** idi. Modrinth'te iki ayrı "Music Box" modu olduğu için (biri `musicbox` adresini
kullanıyor, diğeri NeoForge 1.21.1'de de var ve mod kimliği çakışabilirdi) ad **Quiet Please**,
mod kimliği `quietplease` olarak değiştirildi.

11.11.2026 itibarıyla Modrinth'te `quiet-please` / `quietplease` adresleri ve "Quiet Please" adlı bir mod yoktu;
yayından hemen önce tekrar kontrol et.
