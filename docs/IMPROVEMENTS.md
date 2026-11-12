# Quiet Please – iyileştirmeler (tasarım)

Bu belge `improvements` dalındaki yeni özelliklerin tasarımıdır. Kod `main` dalına birleştirilmedi;
denemek için:

```
git switch improvements
.\gradlew.bat runClient
```

Tüm yeni ayarlar yine `config/quietplease.json` dosyasında durur. Eski dosyalar (sadece `"tracks"` bölümü olan)
aynen yüklenir; eksik bölümler varsayılan değerle başlar.

```json
{
  "tracks":   { "minecraft:music/game/calm1": { "volume": 40, "enabled": true } },
  "options":  { "nowPlayingText": true, "pauseMenuButton": true, "optionsButton": true,
                "showAmbient": false, "playlistMode": false,
                "gap": "vanilla", "gapMinSeconds": 30, "gapMaxSeconds": 60 },
  "playlist": { "tracks": ["minecraft:music/game/calm1", "..."], "position": 0, "repeat": "repeat_list" }
}
```

---

## A. Ayarlar ekranı (`QuietPleaseSettingsScreen`)

Panelin başlığında solda **Ayarlar** düğmesi var. Mods listesindeki **Config** düğmesi eskisi gibi paneli açar.

| Ayar | Değerler | Varsayılan | Not |
|---|---|---|---|
| Çalan şarkı yazısı | Açık / Kapalı | Açık | ESC ve Ayarlar menüsündeki düğmenin altındaki küçük yeşil yazı |
| ESC menüsünde düğme | Açık / Kapalı | Açık | Kapalıysa panel yine Ayarlar, Mods → Config ya da `/quietplease gui` ile açılır |
| Ayarlar menüsünde düğme | Açık / Kapalı | Açık | Ayarlar ve Müzik ve Sesler ekranları |
| Ortam seslerini göster | Açık / Kapalı | Kapalı | Panel açılınca "Ortam" filtresinin başlangıç değeri |
| Çalma listesi modu | Açık / Kapalı | Kapalı | Bkz. B |
| Şarkı arası | Vanilla / Kısa / Yok / Özel | Vanilla | Bkz. aşağısı |
| En kısa / en uzun ara | 0–1200 sn | 30 / 60 | Sadece "Özel" seçiliyken |

### Şarkı arası (müzik boşluğu)

Minecraft iki arka plan şarkısı arasında uzun bir sessizlik bırakır (oyunda 10–20 dk, menüde 1–30 sn).
`MusicManager.tick()` her tick'te o anki durum müziğini (`Music`) seçer ve bekleme süresini bu nesnenin
`getMinDelay()` / `getMaxDelay()` değerlerinden alır. Mixin, `tick()` içinde seçilen `Music`'i
**sadece arka plan müziğiyse** aynı ses olayına sahip ama farklı bekleme süreli bir kopyayla değiştirir:

* **Vanilla** – hiçbir şey değişmez.
* **Kısa** – 30–60 sn.
* **Yok** – 1 sn (0 yerine 1 sn: ses kapalıyken oyunun her tick'te yeni şarkı denemesini önler).
* **Özel** – kaydırıcılardaki en kısa/en uzun süre.

Ejderha ve jenerik müziği (ve bir modun `replaceCurrentMusic=true` ile verdiği her müzik) etkilenmez.

### "Arka plan müziği" tanımı

| Müzik | `replaceCurrentMusic` | Arka plan sayılır mı? |
|---|---|---|
| Oyun, yaratıcı, su altı, biyomlar (Nether dahil) | false | Evet |
| Ana menü (`Musics.MENU`), End (`Musics.END`) | true | **Evet** (vanilla'da da sadece arka plan) |
| Ejderha (`Musics.END_BOSS`), jenerik (`Musics.CREDITS`) | true | Hayır |
| Bir modun `replaceCurrentMusic=true` müziği | true | Hayır |
| Bir modun `SoundManager.play(...)` ile doğrudan çaldığı ses (çoğu boss müziği) | – | MusicManager'dan geçmez, hiç dokunulmaz |

Menü ve End vanilla'da `true` olduğu için ayrıca listeye alındı; yoksa çalma listesi menüde hiç çalışmazdı.

---

## B. Arka plan çalma listesi

### Çalma listesi paneli (`QuietPlaylistScreen`)

Açılış: ana panelin başlığında sağda **Çalma listesi** düğmesi, Ayarlar ekranındaki düğme ve `/quietplease playlist`.

```
            ♪ Çalma listesi  (5 şarkı · mod: açık)
 [Sıra (5)] [Şarkı ekle]                         [Çalma listesi modu: Açık]
 ▮ ♪ calm2   Minecraft   ███████░░░░░ 1:12 / 2:55           [Atla ⏭]
 ┌──────────────────────────────────────────────────────────────────┐
 │ 1. hal3        Minecraft                      [▶] [▲] [▼] [✕]   │
 │ 2. aria_math   Minecraft                      [▶] [▲] [▼] [✕]   │
 │ 3. ...                                                            │
 └──────────────────────────────────────────────────────────────────┘
 [Karıştır] [Tekrar: Listeyi tekrarla] [Temizle] [Bitti]
```

* En üstte **şu an çalan** çalma listesi şarkısı, ilerleme çubuğu ve süre. Süre, `.ogg` dosyasının son
  Ogg sayfasındaki granül konumundan / örnekleme hızından hesaplanır (arka planda, bir kere).
  Geçen süre oyun duraklatılınca durur.
* Altta **sıradaki şarkılar**, çalınacakları sırayla numaralı. Liste sonuna gelince başa sarar; "Sonda dur"
  ve "Sürekli karıştır" modlarında sarmadan sonraki satırlar soluk gösterilir ("bir sonraki tur").
* Satır düğmeleri: **▶** şimdi çal (sıradaki olarak ayarlayıp hemen geçer), **▲/▼** sırada taşı, **✕** sıradan çıkar.
* **Şarkı ekle** sekmesi: tüm müzikler, arama kutusu ve mod filtresi (ana paneldeki gibi); her satırda
  **+ Ekle** (zaten listedeyse **✓**). Sürükle-bırak yapılmadı (isteğe bağlıydı).
* Alt satır: **Karıştır** (çalan şarkı başta kalır, gerisi karışır), **Tekrar** modu, **Temizle** (onay sorar), **Bitti**.

Tekrar modları:

| Mod | Liste bitince |
|---|---|
| Listeyi tekrarla | Baştan, aynı sırayla |
| Sürekli karıştır | Liste karıştırılır ve baştan |
| Sonda dur | Vanilla müziğe dönülür (liste değişene, ▶'ye basılana ya da mod yeniden açılana kadar) |

Ana panelde her parça satırında küçük **☆/★** düğmesi: parçayı listeye ekler/çıkarır. Mod filtresinde
"Tümü"nden sonra **★ Çalma listesi** seçeneği var (sadece listedeki parçalar).

### Davranış

* `MusicManager.startPlaying(Music)` başına mixin: çalma listesi modu açık, liste boş değil ve `Music`
  **arka plan müziği** ise, oyunun rastgele seçimi yerine listedeki **sıradaki** parça çalınır.
  Bunun için `PlaylistSound` (MUSIC kanalı, `SimpleSoundInstance.forMusic` ile aynı özellikler) parçanın
  ses olayıyla oluşturulur ve `resolve()` içinde doğrudan o dosyaya sabitlenir (önizlemedeki yöntem).
  Böylece `WeighedSoundEvents` rastgele seçimine gerek kalmaz.
* `tick()` içindeki "farklı müzik + `replaceCurrentMusic`" kontrolü: çalan şey bir çalma listesi şarkısıysa
  ve yeni durum müziği de arka plan müziğiyse, vanilla'nın onu kesmesi engellenir (yoksa menüde her şarkı
  hemen kesilirdi). Ejderha/jenerik/modların `replace=true` müzikleri yine keser.
* Modların doğrudan çaldığı boss müzikleri (ör. Cataclysm) MusicManager'dan geçmediği için hiç etkilenmez;
  aynı anda çalabilirler – bu vanilla davranışıyla aynı.
* Kapalı parçalar sırada atlanır, parça ses seviyeleri yine uygulanır. Yüklü olmayan parçalar (mod kaldırıldıysa) atlanır.
* Liste boşsa ya da mod kapalıysa vanilla davranışı.
* Önizleme çalarken yeni arka plan şarkısı başlatılmaz (1 sn sonra tekrar denenir); böylece çalma listesi
  önizleme sırasında "ileri sarmaz".
* "Şu an çalan" göstergesi, ESC menüsündeki yazı ve önizleme aynen çalışır (çalma listesi şarkısı normal bir MUSIC sesi).

### Sonraki adım (yapılmadı): yere göre çalma listesi

Menü / Overworld / Nether / End için ayrı listeler. Tasarım fikri: `"playlists": {"menu": [...], "overworld": [...], ...}`;
`startPlaying` içinde `Music` nesnesinden (MENU, END, Nether biyom müzikleri...) ya da oyuncunun boyutundan
yer seçilir; o yerin listesi boşsa genel liste kullanılır. Panelde üstte bir "Yer" seçici olur.

---

## C. Atla

* Panelin "şu an çalan" çubuğunda ve çalma listesi panelinde **Atla** düğmesi, sohbette `/quietplease skip`.
* `MusicManager.stopPlaying()` çağrılır, sonra bekleme süresi ayarlanır:
  * Çalma listesi modu açık ve çalınacak parça varsa: hemen sıradaki parça.
  * Değilse şarkı arası ayarı geçerli: Yok → 1 sn, Kısa → 30–60 sn, Özel → kaydırıcılar,
    Vanilla → o anki müziğin vanilla arası (yani "bu şarkıyı durdur").
* Sadece MusicManager'ın çaldığı müzik atlanır; plaklar ve modların doğrudan çaldığı sesler atlanmaz.

## D. Hazır ayarlar (preset)

* `/quietplease export <ad>` → `config/quietplease-presets/<ad>.json`
* `/quietplease import <ad>` → dosyayı yükler, `config/quietplease.json` olarak kaydeder.
* Dosya biçimi ana dosyayla aynı (parça ayarları + ayarlar + çalma listesi). Ad sadece harf, rakam, `_` ve `-`.
* `import` için var olan hazır ayarlar önerilir.

## E. Küçük düzeltmeler

* Panelin "şu an çalan" çubuğunda uzun parça adları çubuğun dışına taşıyordu; artık kısaltılıyor.

## Test

* `DevSelfTest` (sadece geliştirme, `FMLEnvironment.production` ile kapalı): çalma listesi seçim mantığı
  (sıra, tekrar modları, kapalı parçayı atlama, taşı/çıkar, karıştır), arka plan müziği tanımı ve şarkı arası kopyası.
* GUI testi (`QUIETPLEASE_AUTODUMP=gui`): ayarlar ekranı ve örnek şarkılı çalma listesi panelinin ekran görüntüsü;
  deneme dünyasında çalma listesi modunun `Musics.GAME` yerine listedeki şarkıyı çaldığı ve **Atla**'nın
  sıradakine geçtiği log satırlarıyla kontrol edilir.
