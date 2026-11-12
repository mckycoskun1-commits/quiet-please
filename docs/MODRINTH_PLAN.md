# Modrinth planı (dürüst yol)

> Bu belge sadece bir plan. Modrinth'e hiçbir şey gönderilmedi; **"quiet-please" taslağına dokunulmadı**
> (hâlâ gönderilmemiş taslak). Yapay zekâ (YZ) beyanı hiçbir aşamada gizlenmeyecek.

## Kısa özet: yapay zekâ sorununa çözüm (12.11.2026 güncellemesi)

Gizlemek bir seçenek değil: CurseForge sayfasında "AI disclosure" bölümü zaten herkese açık ve git geçmişinde
"Co-Authored-By: Claude" satırları var. Fark edilirse sadece proje değil hesap da kapanabilir. Dürüst yollar:

| Yol | Emek | Ne kazandırır | Öneri |
|---|---|---|---|
| **A. Şimdilik sadece CurseForge (+ istersen GitHub Releases)** | Yok | CurseForge YZ'yi beyanla kabul ediyor; oyuncuların çoğu zaten oradan indiriyor | **Hemen** – zaten yapıldı |
| **B. Görselleri kendin yap, sonra Modrinth'e dürüstçe sor** | 1 akşam + mesaj | Kesin cevap. "Evet" derlerse Modrinth'e de çıkarsın | **Sıradaki adım** |
| **C. Java/NeoForge öğrenip çekirdeği yeniden yaz** | Birkaç hafta | Mod gerçekten "senin" olur; Modrinth'in "ağırlıklı YZ" şartı ortadan kalkar | B'de "hayır" derlerse |

### Bu güncellemede yapılanlar (B yolunun yarısı)
* Paneldeki YZ logosu **kaldırıldı**: `textures/gui/logo.png` silindi; panel ve çalma listesi başlığında artık
  oyunun kendi **Jukebox** eşyası çiziliyor.
* `docs/screenshots/*.png` oyun içinden **yeniden çekildi** (otomatik GUI testi). Artık hiçbirinde YZ görseli yok.
  Yeni olarak `playlist.png` ve `settings.png` da eklendi.

### Senin yapman gereken (YZ ile yapılamaz, çünkü kural tam olarak bunu istiyor)
1. **İkon (iki dosya):** `art/quietplease-icon.png` (512 ya da 1024 px) ve `src/main/resources/quietplease.png` (256 px).
   En kolay yol: oyunda bir Jukebox'ın ekran görüntüsünü al (`F1` ile arayüzü gizle, `F2` ile çek), Paint ile kare kırp.
   Aşağıdaki 1. bölümde ayrıntısı var. Dosyaları aynı adla değiştirmen yeterli; gerisini ben hallederim
   (derleme, CurseForge ikonunu güncelleme önerisi).
2. **Modrinth'e mesaj:** 3. bölümdeki taslağı kendi Modrinth kullanıcı adınla gönder. Mesajı senin göndermen gerekiyor;
   ben senin adına hiçbir yere mesaj göndermem.
3. Cevap **evet** ise: Modrinth taslağını "Contains AI-generated content" işareti açık olarak gönderiyoruz.
   Cevap **hayır** ise: 2. bölümdeki öğrenme yolu (C), bu arada CurseForge'da yayında kalırsın.

---

## Kurallar (12.11.2026'da yeniden okundu)

Kaynak: <https://modrinth.com/legal/rules>, **§6 "Usage of Generative AI"**, sayfanın son değişikliği: 13 Ağustos 2026.

* **§6.1 Beyan:** Kodun büyük kısmı YZ çıktısıysa, varlıklar (asset) çoğunlukla YZ ile yapıldıysa ya da sayfanın bir
  kısmı YZ'ye dayanıyorsa projede **"Contains AI-generated content"** işareti açılmalı. → Quiet Please için **zorunlu**,
  her durumda açık kalacak.
* **§6.2 Görseller:** Proje sayfasındaki ikon, galeri, açıklama dahil **hiçbir görsel** YZ ile üretilmiş ya da YZ
  çıktısından türetilmiş olamaz. Moderatörler bunları kaldırabilir.
* **§6.2 Bütün proje:** İçeriği **ağırlıklı olarak ya da tamamen YZ çıktısı** olan bir proje herkese açık yayınlanamaz.

Bugünkü durum: kodun neredeyse tamamı YZ (Claude) ile yazıldı; ikon (`art/quietplease-icon.png`), mod listesi ikonu
(`src/main/resources/quietplease.png`) YZ görsel aracıyla yapıldı (panel logosu da öyleydi; 12.11.2026'da kaldırıldı).
Yani **şu haliyle Modrinth'te herkese açık yayınlanamaz.** İki şey değişmeli: görseller ve
kodun "ağırlıklı olarak YZ" olması.

---

## 1. İkon ve görseller: YZ'siz hale getir

Değişmesi gereken iki dosya kaldı (ikisi de aynı YZ görselinden; panel logosu artık oyunun Jukebox eşyası):

| Dosya | Nerede görünüyor | Boyut |
|---|---|---|
| `art/quietplease-icon.png` | Modrinth/CurseForge proje ikonu | 1024 px (512 px de yeter) |
| `src/main/resources/quietplease.png` | Oyundaki Mods listesi | 256 px |

İki seçenek (ikisi de tamamen senin elinden çıkar):

**A) Oyun içi ekran görüntüsü (en kolay).**
1. Creative dünyada bir Jukebox koy, yanına bir plak bırak (ya da eşya çerçevesine as), güzel bir ışık/arka plan seç.
2. `F1` ile arayüzü gizle, `F2` ile ekran görüntüsü al (`.minecraft/screenshots`).
3. Paint / Paint.NET / GIMP ile kare kırp, 512×512 ve 256×256 kaydet.
   Minecraft'ın kendi dokuları oyun içi ekran görüntüsünde sorun değil; YZ yok.

**B) Kendi piksel sanatın.** <https://www.piskelapp.com> (ücretsiz, tarayıcıda): 32×32 tuvalde bir jukebox + nota ya da
"sus" işareti (🤫) çiz, PNG olarak 16× büyütüp dışa aktar (512 px). 256 px'lik küçüğü mod listesi ikonu olur.

Sonra:
* İki dosyayı yenileriyle değiştir (aynı adlar).
* ~~Ekran görüntülerini yeniden al~~ **yapıldı (12.11.2026)**: `docs/screenshots/*.png` logosuz panelle yeniden çekildi.
  Tekrar gerekirse: `QUIETPLEASE_AUTODUMP=gui` ile `runClient` yeni görüntüleri `run/screenshots/` içine kendiliğinden çeker.
* `art/quietplease-folder.ico` sadece senin bilgisayarındaki klasör ikonu; yayına girmiyor ama istersen onu da değiştir.
* CurseForge'daki ikonu ve galeriyi de yenileriyle değiştirmek iyi olur (orada yasak değil ama tutarlılık için).

## 2. Kod: Java/NeoForge'u öğrenip önemli kısımları kendin yeniden yaz

Amaç numara yapmak değil, **gerçekten** senin yazdığın bir mod: önce öğren, sonra her dosyayı orijinale bakmadan
(orijinali sadece "ne yapmalı" listesi olarak kullanarak) yeniden yaz ve oyunda dene. Git geçmişi dürüst kalsın:
her dosyayı kendi commit'inle değiştir. YZ beyanı yine açık kalır (yardım aldığın kısımlar oldu).

### Önce temel (2–4 hafta, günde ~1 saat)
* Java: <https://java-programming.mooc.fi/> (Helsinki Üniversitesi, ücretsiz) – Part 1–7 yeter.
  Alternatif: <https://dev.java/learn/>.
* NeoForge belgeleri: <https://docs.neoforged.net/> – "Getting Started", "Concepts → Events", "Resources → Client → I18n".
* Mixin: <https://github.com/SpongePowered/Mixin/wiki> ve <https://github.com/LlamaLad7/MixinExtras/wiki>.
* Bu projenin kendi `.\gradlew.bat runClient` döngüsü: bir şeyi değiştir → oyunda gör.

### Sonra bu moddaki dosyalar, kolaydan zora

| Sıra | Dosya | Ne yapıyor | Öğreneceğin şey |
|---|---|---|---|
| 1 | `music/MusicTrack.java` | Bir müzik parçası: dosya kimliği, onu çalan ses olayları, türü (müzik/plak/ortam) | `record`, `enum`, metotlar |
| 2 | `Lang.java` + `lang/en_us.json`, `tr_tr.json` | Çeviri anahtarları | Minecraft `Component`, çeviri dosyaları |
| 3 | `music/MusicLibrary.java` | Oyundaki tüm sesleri tarayıp müzikleri moda göre gruplar | Listeler, `Map`, Minecraft ses kayıtları |
| 4 | `music/MusicSettings.java`, `music/ModOptions.java` | Ayarları `config/quietplease.json` dosyasına yazar/okur | Gson ile JSON, dosya okuma/yazma |
| 5 | `QuietPleaseCommands.java` | `/quietplease ...` komutları | Brigadier komut ağacı, tıklanabilir sohbet yazıları |
| 6 | `mixin/WeighedSoundEventsMixin.java` | Kapalı parçaları rastgele seçimden çıkarır | İlk mixin: `@Inject` ile bir metodu değiştirmek |
| 7 | `mixin/SoundEngineMixin.java` | Ses seviyesini parça ayarıyla çarpar | `@ModifyExpressionValue` |
| 8 | `music/MusicPreview.java`, `music/NowPlaying.java` | Önizleme ve "şu an çalan" | Kendi `SoundInstance` sınıfın |
| 9 | `gui/QuietPleaseSettingsScreen.java` | En basit ekran: düğmeler ve kaydırıcılar | `Screen`, `Button`, `CycleButton` |
| 10 | `gui/MenuButton.java` | ESC/Ayarlar menüsüne düğme ekler | NeoForge ekran olayları |
| 11 | `gui/QuietPleaseScreen.java` | Ana panel (liste, filtre, kaydırıcılar) | Kaydırılabilir listeler, çizim |
| 12 | `music/Playlist.java`, `mixin/MusicManagerMixin.java`, `gui/QuietPlaylistScreen.java` | Çalma listesi (en zor kısım) | Oyunun müzik yöneticisine bağlanmak |

**Önerilen en az hedef:** 1–7 ve 9–11 senin elinden çıkmış olsun (modun "çekirdeği" ve arayüzün çoğu). Ondan sonra
proje "ağırlıklı olarak YZ çıktısı" olmaktan çıkar ve aşağıdaki mesajla moderatörlere sorabilirsin. Kendi yazdıklarını
README'de "Credits" başlığında açıkça yaz (hangi kısımlar senin, hangileri YZ yardımıyla).

## 3. Moderatörlere sorulacak mesaj (GÖNDERİLMEDİ)

Nereye: Modrinth destek sayfası (<https://support.modrinth.com>) ya da Modrinth Discord'undaki destek kanalı.
İstersen ikon ve kodu değiştirdikten sonra gönder; değiştirmeden önce sormak da olur (o zaman 2. paragrafı uyarla).

```text
Hi Modrinth team,

I'm the author of a small client-side NeoForge 1.21.1 mod called "Quiet Please" (per-track music volume,
mute, a background playlist). My draft project "quiet-please" is not submitted yet.

I want to be upfront: the first version of the code was written with heavy help from an AI assistant (Claude).
Since then I have been learning Java and NeoForge and have rewritten [list the files/parts] myself.
The icon and all images on the page are now made by me (an in-game screenshot / my own pixel art),
with no generative AI involved, and the gallery shows real in-game screenshots.

I will keep the "Contains AI-generated content" disclosure enabled. Could you tell me whether a project like this,
AI-assisted code but human-made images, can be published publicly under rule 6, or what I would need to change first?

Thank you!
[your Modrinth username]
```

## 4. Bu arada: CurseForge + GitHub sürümü

* **CurseForge:** proje oluşturuldu (id 1799446), galeri yüklendi, 1.0.0 jar'ı 12.11.2026'da yüklendi ve proje
  moderasyonda (onay e-postası bekleniyor). CurseForge'un kuralı YZ görselleri için görünür bir not istiyor; açıklamada
  "AI disclosure" bölümü var. İkonu sonradan YZ'siz olanla değiştirirsen bu notu da güncelle.
* **GitHub sürümü** (henüz uzak depo yok; komutlar sadece öneri, çalıştırılmadı):

  ```
  gh repo create quiet-please --public --source . --remote origin
  git push -u origin main
  gh release create v1.0.0 dist/quietplease-1.0.0.jar --title "Quiet Please 1.0.0" --notes "First release. AI disclosure: code written with the help of an AI assistant (Claude); icon made with an AI image tool."
  ```

  `.github/workflows/build.yml` zaten her push'ta derliyor. `improvements` dalını henüz gönderme; elle testten sonra
  `main`'e birleştirip 1.1.0 olarak ayrıca yayınla.
