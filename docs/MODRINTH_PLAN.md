# Modrinth planı (dürüst yol)

> Bu belge sadece bir plan. Modrinth'e hiçbir şey gönderilmedi; **"quiet-please" taslağına dokunulmadı**
> (hâlâ gönderilmemiş taslak). Yapay zekâ (YZ) beyanı hiçbir aşamada gizlenmeyecek.

## Kurallar (12.11.2026'da yeniden okundu)

Kaynak: <https://modrinth.com/legal/rules>, **§6 "Usage of Generative AI"**, sayfanın son değişikliği: 13 Ağustos 2026.

* **§6.1 Beyan:** Kodun büyük kısmı YZ çıktısıysa, varlıklar (asset) çoğunlukla YZ ile yapıldıysa ya da sayfanın bir
  kısmı YZ'ye dayanıyorsa projede **"Contains AI-generated content"** işareti açılmalı. → Quiet Please için **zorunlu**,
  her durumda açık kalacak.
* **§6.2 Görseller:** Proje sayfasındaki ikon, galeri, açıklama dahil **hiçbir görsel** YZ ile üretilmiş ya da YZ
  çıktısından türetilmiş olamaz. Moderatörler bunları kaldırabilir.
* **§6.2 Bütün proje:** İçeriği **ağırlıklı olarak ya da tamamen YZ çıktısı** olan bir proje herkese açık yayınlanamaz.

Bugünkü durum: kodun neredeyse tamamı YZ (Claude) ile yazıldı; ikon (`art/quietplease-icon.png`), mod listesi ikonu
(`src/main/resources/quietplease.png`) ve panel logosu (`src/main/resources/assets/quietplease/textures/gui/logo.png`)
YZ görsel aracıyla yapıldı. Yani **şu haliyle Modrinth'te herkese açık yayınlanamaz.** İki şey değişmeli: görseller ve
kodun "ağırlıklı olarak YZ" olması.

---

## 1. İkon ve görseller: YZ'siz hale getir

Değişmesi gereken üç dosya var (üçü de aynı YZ görselinden):

| Dosya | Nerede görünüyor | Boyut |
|---|---|---|
| `art/quietplease-icon.png` | Modrinth/CurseForge proje ikonu | 1024 px (512 px de yeter) |
| `src/main/resources/quietplease.png` | Oyundaki Mods listesi | 256 px |
| `src/main/resources/assets/quietplease/textures/gui/logo.png` | Panelin başlığı (ekran görüntülerinde görünüyor!) | küçük, kare |

İki seçenek (ikisi de tamamen senin elinden çıkar):

**A) Oyun içi ekran görüntüsü (en kolay).**
1. Creative dünyada bir Jukebox koy, yanına bir plak bırak (ya da eşya çerçevesine as), güzel bir ışık/arka plan seç.
2. `F1` ile arayüzü gizle, `F2` ile ekran görüntüsü al (`.minecraft/screenshots`).
3. Paint / Paint.NET / GIMP ile kare kırp, 512×512 ve 256×256 kaydet; küçük logo için 64×64.
   Minecraft'ın kendi dokuları oyun içi ekran görüntüsünde sorun değil; YZ yok.

**B) Kendi piksel sanatın.** <https://www.piskelapp.com> (ücretsiz, tarayıcıda): 32×32 tuvalde bir jukebox + nota ya da
"sus" işareti (🤫) çiz, PNG olarak 16× büyütüp dışa aktar (512 px). Aynı çizimin küçüğü logo olur.

Sonra:
* Üç dosyayı yenileriyle değiştir (aynı adlar).
* **Ekran görüntülerini yeniden al** (`docs/screenshots/*.png`): eskilerinde paneldeki YZ logosu görünüyor, bu da §6.2'ye
  takılır. `QUIETPLEASE_AUTODUMP=gui` ile `runClient` yeni görüntüleri `run/screenshots/` içine kendiliğinden çeker.
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

* **CurseForge:** proje oluşturuldu (id 1799446), galeri yüklendi, jar formu dolduruldu; sadece **Add File** düğmesine
  basman gerekiyor (bu proje moderasyona gider). CurseForge'un kuralı YZ görselleri için görünür bir not istiyor; açıklamada
  "AI disclosure" bölümü var. İkonu sonradan YZ'siz olanla değiştirirsen bu notu da güncelle.
* **GitHub sürümü** (henüz uzak depo yok; komutlar sadece öneri, çalıştırılmadı):

  ```
  gh repo create quiet-please --public --source . --remote origin
  git push -u origin main
  gh release create v1.0.0 dist/quietplease-1.0.0.jar --title "Quiet Please 1.0.0" --notes "First release. AI disclosure: code written with the help of an AI assistant (Claude); icon made with an AI image tool."
  ```

  `.github/workflows/build.yml` zaten her push'ta derliyor. `improvements` dalını henüz gönderme; elle testten sonra
  `main`'e birleştirip 1.1.0 olarak ayrıca yayınla.
