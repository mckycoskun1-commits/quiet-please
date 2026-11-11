# Mod sayfası taslağı (Modrinth / CurseForge)

> Modun adı **Quiet Please**, mod kimliği `quietplease`. Önerilen Modrinth adresi: `modrinth.com/mod/quiet-please`.

## Sayfa ayarları

| Alan | Değer |
|---|---|
| Proje türü | Mod |
| Yükleyici | NeoForge |
| Minecraft sürümü | 1.21.1 |
| Ortam (environment) | Client: **required** · Server: **unsupported** (sadece istemci) |
| Kategoriler | **Utility**, **Management** |
| Etiket önerileri | music, sound, volume, audio, modpack, client-side |
| İkon | `art/quietplease-icon.png` (1024 px) |
| Galeri | `docs/screenshots/panel-en.png`, `docs/screenshots/options-button.png`, `docs/screenshots/pause-menu.png` |
| Lisans | Şu an **All Rights Reserved** (`gradle.properties` → `mod_license`) |

## Kısa açıklama (summary, ~100 karakter)

> Mute or turn down any single music track in your modpack. Per-track volume, per-mod toggle, preview.

## Uzun açıklama (İngilizce – sayfanın ana metni)

```markdown
# Quiet Please

Big modpacks bring a lot of music – and not all of it fits together.
**Quiet Please** lets you decide, track by track, what you actually want to hear.

![Panel](panel-en.png)

## Features
- 🎵 **Every music track in one list**, grouped by the mod it comes from
- 🔇 **Turn off** any track – the game simply picks another one instead
- 🔉 **Per-track volume** from 0 to 100 %
- 📦 **Disable all music of a mod** with one click
- ▶ **Preview** any track inside the panel (background music pauses while you listen)
- 🟢 **See what is playing right now** – in the panel and right under the button in the ESC menu, so you can find a song even if you don't know its name
- 🔍 Search, mod filter, and an optional "ambient" view for long ambience loops
- 💿 Works for vanilla music, music discs and modded boss/biome music
- 🌍 English and Turkish

## How to open it
**Esc → ♪ Quiet Please** (top-right corner), **Options → ♪ Quiet Please**, or **Options → Music & Sounds → ♪ Quiet Please**,
or **Mods → Quiet Please → Config**. Power users can also use `/quietplease` in chat.

## Good to know
- **Client-side only.** Install it on your own game; servers don't need it, and you can join any server.
- Settings are stored in `config/quietplease.json`, so modpack makers can ship a pre-tuned file.
- Picks up modded music too – for example the boss themes of L_Ender's Cataclysm and Mowzie's Mobs.
```

## Uzun açıklama (Türkçe – istersen ikinci dil olarak)

```markdown
# Quiet Please

Büyük mod paketleri bir sürü müzik getirir ve hepsi birbirine uymaz.
**Quiet Please** ile neyi duymak istediğine parça parça sen karar verirsin.

## Özellikler
- 🎵 **Tüm müzikler tek listede**, geldikleri moda göre gruplanmış
- 🔇 İstediğin parçayı **kapat** – oyun onun yerine başka bir parça seçer
- 🔉 Her parça için **%0–100 ses ayarı**
- 📦 Bir modun **tüm müziklerini tek tıkla kapat**
- ▶ Panelde **dinleyerek önizle** (dinlerken arka plan müziği susar)
- 🔍 Arama, mod filtresi ve uzun ortam sesleri için ayrı görünüm
- 💿 Vanilla müzikler, plaklar ve modların boss/biyom müzikleriyle çalışır
- 🌍 İngilizce ve Türkçe

## Nasıl açılır
**Ayarlar → ♪ Quiet Please** (sağ üst köşe), **Ayarlar → Müzik ve Sesler → ♪ Quiet Please**
ya da **Modlar → Quiet Please → Yapılandırma**. Sohbette `/quietplease` komutu da var.

## Bilmen gerekenler
- **Sadece istemci tarafı.** Sunucuya kurulması gerekmez, her sunucuya girebilirsin.
- Ayarlar `config/quietplease.json` dosyasında; mod paketi yapanlar hazır ayarlı bir dosya ekleyebilir.
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
