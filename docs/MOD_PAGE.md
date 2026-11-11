# Mod sayfası taslağı (Modrinth / CurseForge)

> Yayınlamadan önce: **ad değişikliği gerekiyor**. Bkz. en alttaki "Ad sorunu" bölümü.

## Sayfa ayarları

| Alan | Değer |
|---|---|
| Proje türü | Mod |
| Yükleyici | NeoForge |
| Minecraft sürümü | 1.21.1 |
| Ortam (environment) | Client: **required** · Server: **unsupported** (sadece istemci) |
| Kategoriler | **Utility**, **Management** |
| Etiket önerileri | music, sound, volume, audio, modpack, client-side |
| İkon | `art/musicbox-icon-original.png` (1024 px) |
| Galeri | `docs/screenshots/panel-en.png`, `options-button.png`, `sound-options-button.png`, `panel-tr.png` |
| Lisans | Şu an **All Rights Reserved** (`gradle.properties` → `mod_license`) |

## Kısa açıklama (summary, ~100 karakter)

> Mute or turn down any single music track in your modpack. Per-track volume, per-mod toggle, preview.

## Uzun açıklama (İngilizce – sayfanın ana metni)

```markdown
# Music Box

Big modpacks bring a lot of music – and not all of it fits together.
**Music Box** lets you decide, track by track, what you actually want to hear.

![Panel](panel-en.png)

## Features
- 🎵 **Every music track in one list**, grouped by the mod it comes from
- 🔇 **Turn off** any track – the game simply picks another one instead
- 🔉 **Per-track volume** from 0 to 100 %
- 📦 **Disable all music of a mod** with one click
- ▶ **Preview** any track inside the panel (background music pauses while you listen)
- 🔍 Search, mod filter, and an optional "ambient" view for long ambience loops
- 💿 Works for vanilla music, music discs and modded boss/biome music
- 🌍 English and Turkish

## How to open it
**Options → ♪ Music Box** (top-right corner), or **Options → Music & Sounds → ♪ Music Box**,
or **Mods → Music Box → Config**. Power users can also use `/musicbox` in chat.

## Good to know
- **Client-side only.** Install it on your own game; servers don't need it, and you can join any server.
- Settings are stored in `config/musicbox.json`, so modpack makers can ship a pre-tuned file.
- Picks up modded music too – for example the boss themes of L_Ender's Cataclysm and Mowzie's Mobs.
```

## Uzun açıklama (Türkçe – istersen ikinci dil olarak)

```markdown
# Music Box

Büyük mod paketleri bir sürü müzik getirir ve hepsi birbirine uymaz.
**Music Box** ile neyi duymak istediğine parça parça sen karar verirsin.

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
**Ayarlar → ♪ Music Box** (sağ üst köşe), **Ayarlar → Müzik ve Sesler → ♪ Music Box**
ya da **Modlar → Music Box → Yapılandırma**. Sohbette `/musicbox` komutu da var.

## Bilmen gerekenler
- **Sadece istemci tarafı.** Sunucuya kurulması gerekmez, her sunucuya girebilirsin.
- Ayarlar `config/musicbox.json` dosyasında; mod paketi yapanlar hazır ayarlı bir dosya ekleyebilir.
```

## İlk sürüm notu (changelog)

```markdown
## 1.0.0 – First release
- Per-track volume and on/off for every music track, grouped by mod
- In-game panel with search, mod filter and preview
- Buttons in Options and Music & Sounds, plus /musicbox chat commands
- English and Turkish translations
```

## Ad sorunu – yayından önce karar verilmeli

Modrinth'te **iki ayrı "Music Box" modu** zaten var:

| Mod | Adres | İndirme | Yükleyici / sürüm |
|---|---|---|---|
| Music Box (YouTube müziği çalar) | modrinth.com/mod/**musicbox** | ~71 bin | Forge/Fabric, 1.20.1 |
| Music Box (nota ızgarası) | modrinth.com/mod/cc-mb | ~12 bin | **NeoForge 1.21.1 dahil** |

- `musicbox` adresi alınmış; aynı adla yayınlamak karışıklık yaratır.
- İkinci mod NeoForge 1.21.1'de de var. Onun **mod kimliği** de `musicbox` ise ikisi aynı pakette
  **birlikte yüklenemez** (oyun açılmaz). Bunu kesin bilmenin yolu onun jar'ına bakmak; güvenli yol
  bizim kimliği değiştirmek.

**Ad önerileri** (11.11.2026'da Modrinth'te kontrol edildi; yayın öncesi tekrar bak):
- **Tracklist** – kısa, işi anlatıyor · adres `tracklist` **boş**, aynı adlı mod yok
- **Soundtrack Tuner** – adres **boş**, aynı adlı mod yok
- **Modpack Music Manager** – arama sonuçlarında bulunması kolay · adres **boş**
- **Jukebox Control** – adres **boş**, ama "Jukebox Redstone Control" adlı benzer bir mod var
- ~~Mixtape~~ – **alınmış** (20 bin indirmeli bir mod)

Ad değişirse değişecekler: `gradle.properties` (`mod_id`, `mod_name`), Java paket adı (isteğe bağlı),
`assets/musicbox` klasör adı, `musicbox.mixins.json`, dil anahtarları (`musicbox.` öneki) ve
ayar dosyasının adı (`config/musicbox.json`). Bunu tek seferde yapabilirim.
