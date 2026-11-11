# Music Box

Minecraft 1.21.1 (NeoForge) için istemci tarafı bir mod. Mod paketlerindeki müzikleri tek tek
yönetmeyi amaçlar: her müziğin sesini ayrı ayrı kısmak ya da hiç çalınmamasını sağlamak.

> Not: "Music Box" adı Modrinth'te başka modlarca kullanılıyor; yayından önce ad değişmeli.
> Ayrıntılar ve ad önerileri: [docs/MOD_PAGE.md](docs/MOD_PAGE.md).

## Komutlar

Proje klasöründe (PowerShell):

| Ne yapar | Komut |
|---|---|
| Modu derler; bitmiş jar `dist/` klasörüne kopyalanır | `.\gradlew.bat build` |
| Modu yüklenmiş halde Minecraft'ı açar (test için) | `.\gradlew.bat runClient` |
| Derleme çıktılarını siler | `.\gradlew.bat clean` |

### OneDrive notu

Proje OneDrive'daki Masaüstünde. OneDrive eşitlediği dosyaları Gradle'ın okuyamadığı bir türe çevirdiği için,
proje bir OneDrive klasöründeyken (bkz. `build.gradle` başı):

- derleme çıktıları → `%USERPROFILE%\.musicbox-dev\build`
- test oyununun klasörü (modlar, ayarlar, loglar, ekran görüntüleri) → `%USERPROFILE%\.musicbox-dev\run`
- bitmiş jar ayrıca → proje içindeki `dist/`

Test oyununa başka mod eklemek için jar'ları `%USERPROFILE%\.musicbox-dev\run\mods` klasörüne koy.

## Klasör yapısı

- `src/main/java/com/musicbox/` modun Java kodu
- `src/main/resources/musicbox.png` mod listesinde görünen ikon (256 px)
- `src/main/resources/assets/musicbox/textures/gui/logo.png` panel başlığındaki küçük logo
- `src/main/resources/assets/musicbox/lang/` çeviriler (İngilizce ve Türkçe)
- `src/main/templates/META-INF/neoforge.mods.toml` mod bilgileri (ad, yazar, açıklama)
- `gradle.properties` Minecraft/NeoForge sürümleri, mod kimliği (`musicbox`), sürüm ve lisans
- `art/` ikonun büyük hali ve Windows klasör ikonu
- `docs/` mod sayfası taslağı ve ekran görüntüleri
- `TESTING.md` elle test listesi

## Durum

1. [x] Proje iskeleti
2. [x] Oyundaki tüm müzikleri bulup hangi moddan geldiklerine göre listelemek
3. [x] Her müzik için ses seviyesi ve kapatma ayarı (ayarlar dosyaya kaydedilir)
4. [x] Oyun içi panel: liste, kaydırıcılar, aç/kapat, dinleyerek önizleme
5. [x] İngilizce/Türkçe dil desteği, ikon
6. [x] Gerçek müzik modlarıyla otomatik test (Cataclysm, Mowzie's Mobs)
7. [ ] Elle test ([TESTING.md](TESTING.md)) – özellikle gerçek launcher ve The Aether
8. [ ] Ad kararı ve yayın ([docs/MOD_PAGE.md](docs/MOD_PAGE.md))

## Panel

**Ayarlar** (Options) ya da **Müzik ve Sesler** ekranının sağ üst köşesindeki **♪ Music Box** düğmesi,
Mods listesinde Music Box > **Config** düğmesi ya da `/musicbox gui` komutu ile açılır. Arama, mod filtresi, her parça için
▶ dinleme, ses kaydırıcısı ve Açık/Kapalı düğmesi vardır. Önizleme çalarken oyunun arka plan müziği susar.

## Dil desteği

Tüm yazılar `src/main/resources/assets/musicbox/lang/` içindeki dosyalardadır: `en_us.json` (İngilizce, varsayılan)
ve `tr_tr.json` (Türkçe). Oyun hangi dildeyse o dosya kullanılır; çevirisi olmayan dillerde İngilizce görünür.
Yeni bir dil eklemek için `en_us.json` dosyasını kopyalayıp dil koduyla adlandırmak (örneğin `de_de.json`) ve
metinleri çevirmek yeterli.

## Sohbet komutları

- `/musicbox` modlara göre müzik sayıları (mod adına tıklayınca liste açılır)
- `/musicbox list <mod>` o modun parçaları, tıklanabilir [−] [+] [Kapat]/[Aç] düğmeleriyle
- `/musicbox volume <parça> <0-100>` parçanın ses seviyesi
- `/musicbox off <parça>` / `/musicbox on <parça>` parçayı kapat / aç
- `/musicbox mod <mod> off` / `on` bir modun tüm parçalarını kapat / aç
- `/musicbox reset <parça>` / `/musicbox resetall` ayarları sıfırla
- `/musicbox dump` tam listeyi (ayarlarla birlikte) oyun klasöründeki `musicbox-tracks.txt` dosyasına yazar
- `/musicbox gui` paneli açar

Ayarlar `config/musicbox.json` dosyasında saklanır.

## Geliştirici notu

Sadece geliştirme ortamında (`runClient`) ve `MUSICBOX_AUTODUMP` ortam değişkeni ayarlıyken: ana menüde küçük bir
öz-test çalışır (parça atlama ve ses çarpanı), müzik listesi `musicbox-tracks.txt` dosyasına yazılır ve
`MUSICBOX_AUTODUMP=gui` ise panel açılıp ekran görüntüsü alınır. Sonuçlar test oyununun `logs/latest.log`
dosyasında `SELFTEST` / `GUITEST` olarak görünür. Gerçek oyunda (launcher'dan) bu kod hiç çalışmaz.
