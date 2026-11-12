# Quiet Please

Minecraft 1.21.1 (NeoForge) için istemci tarafı bir mod. Mod paketlerindeki müzikleri tek tek
yönetmeyi amaçlar: her müziğin sesini ayrı ayrı kısmak ya da hiç çalınmamasını sağlamak.

> İlk adı "Music Box" idi; Modrinth'te aynı adlı iki mod olduğu için **Quiet Please** olarak değiştirildi.
> Proje `C:\Users\mckyc\QuietPlease` klasöründe (OneDrive dışında); masaüstünde **Quiet Please** kısayolu var.
> Klasör ve kısayol ikonu `art/quietplease-folder.ico` dosyasına bağlı.

## Komutlar

Proje klasöründe (PowerShell):

| Ne yapar | Komut |
|---|---|
| Modu derler; bitmiş jar `dist/` klasörüne kopyalanır | `.\gradlew.bat build` |
| Modu yüklenmiş halde Minecraft'ı açar (test için) | `.\gradlew.bat runClient` |
| Derleme çıktılarını siler | `.\gradlew.bat clean` |

- Derleme çıktıları `build/`, test oyununun klasörü (modlar, ayarlar, loglar, ekran görüntüleri) `run/` içinde.
- Bitmiş jar ayrıca `dist/` klasörüne kopyalanır.
- Test oyununa başka mod eklemek için jar'ları `run/mods` klasörüne koy.

### OneDrive notu

Projeyi OneDrive'daki bir klasöre koyma: OneDrive eşitlediği dosyaları Gradle'ın okuyamadığı bir türe çeviriyor.
Yine de oraya konursa `build.gradle` bunu fark edip derleme çıktılarını ve test oyununu
`%USERPROFILE%\.quietplease-dev` altına alır.

## Klasör yapısı

- `src/main/java/com/quietplease/` modun Java kodu
- `src/main/resources/quietplease.png` mod listesinde görünen ikon (256 px)
- `src/main/resources/assets/quietplease/textures/gui/logo.png` panel başlığındaki küçük logo
- `src/main/resources/assets/quietplease/lang/` çeviriler (İngilizce ve Türkçe)
- `src/main/templates/META-INF/neoforge.mods.toml` mod bilgileri (ad, yazar, açıklama)
- `gradle.properties` Minecraft/NeoForge sürümleri, mod kimliği (`quietplease`), sürüm ve lisans
- `art/quietplease-icon.png` ikonun büyük hali (1024 px, Modrinth ikonu) · `art/quietplease-folder.ico` Windows klasör ikonu
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
8. [x] Ad kararı: **Quiet Please** (mod kimliği `quietplease`)
9. [ ] Yayın ([docs/MOD_PAGE.md](docs/MOD_PAGE.md))
10. [x] `improvements` dalı (1.1.0): Ayarlar ekranı, şarkı arası, çalma listesi, Atla, hazır ayarlar – elle test bekliyor

## Panel

**ESC menüsü**, **Ayarlar** (Options) ya da **Müzik ve Sesler** ekranının sağ üst köşesindeki **♪ Quiet Please** düğmesi,
Mods listesinde Quiet Please > **Config** düğmesi ya da `/quietplease gui` komutu ile açılır. Arama, mod filtresi, her parça için
▶ dinleme, ses kaydırıcısı ve Açık/Kapalı düğmesi vardır. Önizleme çalarken oyunun arka plan müziği susar.
Listenin üstündeki yeşil **Şu an çalıyor** çubuğu oyunda o an çalan müziği gösterir; tıklayınca o parça listede bulunur.
Çalan parçanın satırı da yeşil "çalıyor" etiketiyle işaretlenir. Tek oyunculu oyunda panel açıkken oyun duraklar.
Önizleme ana ses kanalından çalar, yani oyunun "Müzik" kaydırıcısı %0 olsa da duyulur.
ESC menüsünde düğmenin hemen altında, Ayarlar ekranlarında düğmenin solunda o an çalan şarkının adı küçük yazıyla görünür.
Mod filtresi düğmesinde sol tık sonraki moda, sağ tık önceki moda geçer.
Çubuğun sağındaki **Atla** çalan arka plan şarkısını durdurur. Başlıkta solda **Ayarlar**, sağda **Çalma listesi** düğmesi var;
her parça satırındaki **☆/★** parçayı çalma listesine ekler/çıkarır, mod filtresindeki **★ Çalma listesi** sadece listedekileri gösterir.

## Ayarlar

Paneldeki **Ayarlar** düğmesi ya da `/quietplease settings`: ESC menüsündeki ve Ayarlar ekranlarındaki düğme (ayrı ayrı),
çalan şarkı yazısı, listede ortam sesleri, çalma listesi modu ve **şarkı arası** (iki arka plan şarkısı arasındaki sessizlik:
Vanilla / Kısa 30–60 sn / Yok / Özel kaydırıcılar). Şarkı arası sadece arka plan müziğini değiştirir; ejderha, jenerik ve
boss müzikleri etkilenmez. Tasarım ayrıntıları: [docs/IMPROVEMENTS.md](docs/IMPROVEMENTS.md).

## Çalma listesi

Paneldeki **Çalma listesi** düğmesi, Ayarlar ekranı ya da `/quietplease playlist`. Bir müzik çalar gibi: üstte çalan şarkı ve
ilerleme çubuğu, altında sıradaki şarkılar numaralı; **▶** şimdi çal, **▲/▼** sırada taşı, **✕** çıkar, **Şarkı ekle** sekmesinde
arama ve mod filtresiyle ekleme, **Atla**, **Karıştır**, **Tekrar** (listeyi tekrarla / sürekli karıştır / sonda dur), **Temizle**.
**Çalma listesi modu** açıkken oyun her arka plan şarkısı başlatacağında (menü, oyun, biyomlar, su altı, Nether, End) rastgele
parça yerine listedeki sıradakini çalar. Ejderha/jenerik müziği ve modların doğrudan çaldığı boss müzikleri yine araya girer.
Liste boşsa ya da mod kapalıysa her şey vanilla gibidir. Sıra `config/quietplease.json` dosyasında saklanır.

## Dil desteği

Tüm yazılar `src/main/resources/assets/quietplease/lang/` içindeki dosyalardadır: `en_us.json` (İngilizce, varsayılan)
ve `tr_tr.json` (Türkçe). Oyun hangi dildeyse o dosya kullanılır; çevirisi olmayan dillerde İngilizce görünür.
Yeni bir dil eklemek için `en_us.json` dosyasını kopyalayıp dil koduyla adlandırmak (örneğin `de_de.json`) ve
metinleri çevirmek yeterli.

## Sohbet komutları

- `/quietplease` modlara göre müzik sayıları (mod adına tıklayınca liste açılır)
- `/quietplease list <mod>` o modun parçaları, tıklanabilir [−] [+] [Kapat]/[Aç] düğmeleriyle
- `/quietplease volume <parça> <0-100>` parçanın ses seviyesi
- `/quietplease off <parça>` / `/quietplease on <parça>` parçayı kapat / aç
- `/quietplease mod <mod> off` / `on` bir modun tüm parçalarını kapat / aç
- `/quietplease reset <parça>` / `/quietplease resetall` ayarları sıfırla
- `/quietplease dump` tam listeyi (ayarlarla birlikte) oyun klasöründeki `quietplease-tracks.txt` dosyasına yazar
- `/quietplease gui` paneli açar
- `/quietplease settings` / `/quietplease playlist` Ayarlar ekranını / çalma listesi panelini açar
- `/quietplease skip` çalan arka plan şarkısını atlar
- `/quietplease export <ad>` / `/quietplease import <ad>` tüm ayarları (çalma listesi dahil)
  `config/quietplease-presets/<ad>.json` olarak kaydeder / yükler

Ayarlar `config/quietplease.json` dosyasında saklanır (parça ayarları, `options` ve `playlist` bölümleri; eski dosyalar aynen yüklenir).

## Geliştirici notu

Sadece geliştirme ortamında (`runClient`) ve `QUIETPLEASE_AUTODUMP` ortam değişkeni ayarlıyken: ana menüde küçük bir
öz-test çalışır (parça atlama, ses çarpanı, çalma listesi seçimi, şarkı arası, hazır ayar), müzik listesi
`quietplease-tracks.txt` dosyasına yazılır ve `QUIETPLEASE_AUTODUMP=gui` ise panel, Ayarlar ekranı ve çalma listesi paneli
açılıp ekran görüntüsü alınır; bir deneme dünyasında çalma listesinin oyun müziğini devraldığı ve Atla'nın sıradakine geçtiği kontrol edilir. Sonuçlar test oyununun `logs/latest.log`
dosyasında `SELFTEST` / `GUITEST` olarak görünür. Gerçek oyunda (launcher'dan) bu kod hiç çalışmaz.
