# Music Box

Minecraft 1.21.1 (NeoForge) için istemci tarafı bir mod. Mod paketlerindeki müzikleri tek tek
yönetmeyi amaçlar: her müziğin sesini ayrı ayrı kısmak ya da hiç çalınmamasını sağlamak.

## Komutlar

Proje klasöründe (PowerShell):

| Ne yapar | Komut |
|---|---|
| Modu derler, `.jar` dosyasını `build/libs/` içine koyar | `.\gradlew.bat build` |
| Modu yüklenmiş halde Minecraft'ı açar (test için) | `.\gradlew.bat runClient` |
| Derleme çıktılarını siler | `.\gradlew.bat clean` |

## Klasör yapısı

- `src/main/java/com/musicbox/` modun Java kodu
- `src/main/resources/musicbox.png` mod listesinde görünen ikon (256 px)
- `src/main/resources/assets/musicbox/textures/gui/logo.png` panel başlığındaki küçük logo
- `art/musicbox-icon-original.png` ikonun büyük hali (Modrinth/CurseForge sayfası için)
- `src/main/resources/assets/musicbox/lang/` çeviriler (İngilizce ve Türkçe)
- `src/main/templates/META-INF/neoforge.mods.toml` mod bilgileri (ad, açıklama, sürüm)
- `gradle.properties` Minecraft/NeoForge sürümleri ve mod kimliği (`musicbox`)

## Plan

1. [x] Proje iskeleti
2. [x] Oyundaki tüm müzikleri bulup hangi moddan geldiklerine göre listelemek
3. [x] Her müzik için ses seviyesi ve kapatma ayarı (ayarlar dosyaya kaydedilir)
4. [x] Oyun içi ekran: liste, kaydırma çubukları, aç/kapat düğmeleri, dinleyerek önizleme

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

Ayarlar `config/musicbox.json` dosyasında saklanır.

## Geliştirici notu

`MUSICBOX_AUTODUMP` ortam değişkeni ayarlıyken oyun açılırsa, ana menüde küçük bir öz-test çalışır
(parça atlama ve ses çarpanı) ve sonuç `run/logs/latest.log` dosyasına `SELFTEST` olarak yazılır.
