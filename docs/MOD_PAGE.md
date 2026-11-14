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
| Galeri | `docs/screenshots/panel-en.png`, `docs/screenshots/options-button.png`, `docs/screenshots/pause-menu.png`; açıklamadaki görseller gh-pages img/ klasöründen gelir (panel-en, playlist-suggestions, pause-menu-now-playing, settings, options-button) |
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

## Shorter silence between songs
![The Quiet Please settings screen](https://mckycoskun1-commits.github.io/quiet-please/img/settings.png)

- **Music gap** – Vanilla, Short, None or Custom with your own shortest and longest wait. It only changes background music; dragon, credits and boss music are not affected
- Turn the now-playing text or the menu buttons off
- Turn playlist mode on or off, and choose whether ambient tracks show in the list

## Open it from anywhere
![The Quiet Please button in the Options screen](https://mckycoskun1-commits.github.io/quiet-please/img/options-button.png)

- **Esc → ♪ Quiet Please** (top-right corner)
- **Options → ♪ Quiet Please** or **Options → Music & Sounds → ♪ Quiet Please**
- **Mods → Quiet Please → Config**
- **Key bindings** (unbound by default) to open the panel and skip a song: Controls → Key Binds → Quiet Please
- **Chat commands**: `/quietplease`, `/quietplease skip`, and `/quietplease export <name>` / `import <name>` to share a setup

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
- English and Turkish.

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
- Her parça için **%0–100 ses ayarı**
- Bir modun **tüm müziklerini tek tıkla kapat**
- Panelde **dinleyerek önizle** (dinlerken arka plan müziği susar)
- **Arka plan çalma listesi** – şarkıları ve sırasını sen seç, bir müzik çalar gibi: ilerleme çubuklu sıra, yukarı/aşağı taşıma, karıştırma, tekrar. Boss, ejderha ve jenerik müzikleri yine araya girer
- Çalan arka plan şarkısını **atla** (düğme, `/quietplease skip` ya da seçtiğin bir tuş)
- Paneli açmak ve şarkı atlamak için **kısayol tuşları** (varsayılan olarak boş)
- **Şarkı arası** – arka plan şarkıları arasındaki uzun sessizliği kısalt ya da kaldır
- **Ayarlar ekranı** – menü düğmelerini ya da çalan şarkı yazısını gizle
- **Hazır ayarlar** – `/quietplease export <ad>` / `import <ad>` ile ayarları paylaş
- Arama, mod filtresi ve uzun ortam sesleri için ayrı görünüm
- Vanilla müzikler, plaklar ve modların boss/biyom müzikleriyle çalışır
- İngilizce ve Türkçe

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

## Sonraki sürüm notu (taslak, henüz yayınlanmadı)

```markdown
## Playlist update
- A song leaves the queue after it plays; the new loop button sends it to the end of the queue instead (replaces the repeat modes)
- Favorites: the star keeps a song in a Favorites group at the top of the panel (the group appears once you have a favorite)
- Suggested panel next to the queue: favorites, songs you added before and popular Minecraft songs, mixed
- Track tooltips are short; hold Ctrl for the full details
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
