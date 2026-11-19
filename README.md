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
| 18 jar'ın hepsini derler ve `dist/` klasörüne kopyalar | `.\gradlew.bat collectJars` |
| Tek bir sürümü derler | `.\gradlew.bat :1.21.1-neoforge:build` |
| O sürümle Minecraft'ı açar (test için) | `.\gradlew.bat :26.1.2-fabric:runClient` |
| `src/` klasörünü başka bir sürümün haline getirir (IDE için) | `.\gradlew.bat "Set active project to 1.21.1-fabric"` |
| `src/` klasörünü 26.1.2 haline geri getirir (commit öncesi) | `.\gradlew.bat "Reset active project"` |
| Derleme çıktılarını siler | `.\gradlew.bat clean` |

### Çok sürüm (Stonecutter)

Bütün Minecraft sürümleri tek dalda, [Stonecutter](https://stonecutter.kikugie.dev/) ile derlenir. Her sürüm ve
yükleyici için bir alt proje var: `versions/<sürüm>-<yükleyici>` (1.18.2, 1.19.2, 1.20.1: Fabric, Forge;
1.21.1, 26.1.2, 26.2, 26.3: Fabric, Forge, NeoForge).

- Kod `src/main/java` içinde bir kez yazılır, 26.1.2 adlarıyla. Sürümler arasındaki gerçek farklar
  `//? if >=1.21 {` ... `//?} else {` ... `//?}` yorumlarıyla ayrılır; o sürümde geçerli olmayan kısım yorum satırına dönüşür.
- Basit ad değişiklikleri (`Identifier` / `ResourceLocation`, `GuiGraphicsExtractor` / `GuiGraphics`,
  `g.text(` / `g.drawString(`, 26.2'deki `minecraft.gui.setScreen(` ...) elle yazılmaz: `stonecutter.gradle.kts`
  içindeki `replacements` kuralları her sürüm için değiştirir.
- Yükleyiciye özel kod `com/quietplease/fabric`, `forge`, `neoforge` paketlerinde; diğer yükleyicilerde derlenmez.
  1.19.2 ve 1.18.2'nin kendi GUI yardımcıları (`Btn`, `HintBox`, `Tooltip`, `Slider`, `GuiGraphics`, `Txt`) ve
  1.20.1 öncesinin `PauseScreenAccessor` sınıfı sadece o sürümlerde derlenir (`build.gradle`).
- Sürüm numaraları (Minecraft, Fabric API, Forge, NeoForge, Java...) `stonecutter.properties.toml` içinde,
  mod sürümü ve ortak ayarlar `gradle.properties` içinde.
- Gradle'ın Java 25 ile çalışması gerekir (Stonecutter 0.10 ve 26.x bunu ister); eski sürümler için Java 17/21
  derleyicisini Gradle kendisi indirir.

- Derleme çıktıları `versions/<sürüm>-<yükleyici>/build/`, test oyununun klasörü
  `versions/<sürüm>-<yükleyici>/run/` içinde.
- Test oyununa başka mod eklemek için jar'ları o sürümün `run/mods` klasörüne koy.

### OneDrive notu

Projeyi OneDrive'daki bir klasöre koyma: OneDrive eşitlediği dosyaları Gradle'ın okuyamadığı bir türe çeviriyor.
Yine de oraya konursa `build.gradle` bunu fark edip derleme çıktılarını ve test oyununu
`%USERPROFILE%\.quietplease-dev` altına alır.

## Klasör yapısı

- `src/main/java/com/quietplease/` modun Java kodu
- `src/main/resources/quietplease.png` mod listesinde görünen ikon (256 px)
- Panel başlığındaki logo ayrı bir resim değil: oyunun kendi Jukebox eşyası çiziliyor
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
solda üç düğme (**☆/★** favori, **▶** şimdi çal, **+** sıraya al), ses kaydırıcısı ve Açık/Kapalı düğmesi vardır.
Şarkının adına ya da ikonuna tıklamak ön izlemeyi başlatır, bir daha tıklamak durdurur. Önizleme çalarken oyunun arka plan
müziği susar. **▶** şarkıyı arka plan müziği olarak hemen başlatır. **+** şarkıyı çalan şarkıdan hemen sonraya koyar; arka
arkaya eklenenler eklenme sırasıyla dizilir (son eklenen en alta), liste yoksa oluşur ve çalma listesi modu açılır. Sıradaki
şarkıların **+** yerinde sıra numarası görünür; numaraya tıklamak şarkıyı sıradan çıkarır.
Listenin üstündeki yeşil **Şu an çalıyor** çubuğu oyunda o an çalan müziği gösterir; tıklayınca o parça listede bulunur.
Çalan parçanın satırı da yeşil "çalıyor" etiketiyle işaretlenir. Tek oyunculu oyunda panel açıkken oyun duraklar.
Önizleme ana ses kanalından çalar, yani oyunun "Müzik" kaydırıcısı %0 olsa da duyulur.
ESC menüsünde düğmenin hemen altında, Ayarlar ekranlarında düğmenin solunda o an çalan şarkının adı küçük yazıyla görünür.
Mod filtresi düğmesinde sol tık sonraki moda, sağ tık önceki moda geçer.
Çubuğun sağındaki **Atla** çalan arka plan şarkısını durdurur. Başlıkta solda **Ayarlar**, sağda **Çalma listesi** düğmesi var;
her parça satırındaki **☆/★** parçayı favorilere ve çalma listesine ekler/çıkarır, mod filtresindeki **★ Çalma listesi** sadece listedekileri gösterir.
Her mod başlığında **Mod: %100** kaydırıcısı o modun tüm şarkılarının sesini birlikte ayarlar; her şarkının kendi ses ayarı
bunun üstüne çarpılır (örneğin mod %50, şarkı %40 ise şarkı %20 çalar).

## Şarkı bildirimi

Oyunda yeni bir müzik başlayınca ekranın kenarından küçük bir kart kayarak çıkar: plak ya da nota bloğu ikonu, şarkının adı,
geldiği mod ve oynayan küçük ekolayzer çubukları. Birkaç saniye sonra kart aynı kenara geri girer. F1 ile arayüz gizlenince
kart da gizlenir.

Ayarlar ekranındaki **Bildirim ayarları...** düğmesi ayrı bir ekran açar. Burada kartın yeri (sağ ya da sol; üst, orta ya da
alt), kenardan uzaklığı ve yukarı/aşağı kayması, boyutu (%50–%200), ekranda kalma süresi (2–15 sn), şarkı adının ve mod adının
rengi, yazı stili (normal, kalın, italik), yazı gölgesi, arka planı (yok, hafif, koyu, çerçeveli) ve ikonun, mod adının ve
ekolayzerin gösterilip gösterilmeyeceği seçilir. Ekranda örnek bir kart her değişikliği anında gösterir. Kartı fareyle tutup
sürükleyerek de yerini değiştirebilirsin: bırakınca en yakın köşe ya da kenar seçilir, kenar ve yukarı/aşağı kaydırıcıları
kartın bırakıldığı yere göre ayarlanır. **Sıfırla** hepsini varsayılana döndürür.

## Ayarlar

Paneldeki **Ayarlar** düğmesi ya da `/quietplease settings` açar. Ayarlar üç sekmede, her sekmenin altında ne işe yaradığını
anlatan bir satır var:

- **Müzik**: çalma listesi modu, **Çalma listesi** ekranı, kendi müziğin (arka planda çalsın mı, klasörü aç), **Geçmiş ve
  istatistik**, **şarkılar arası** (iki arka plan şarkısı arasındaki sessizlik: Vanilla / Kısa 30–60 sn / Yok / Özel
  kaydırıcılar). Şarkı arası sadece arka plan müziğini değiştirir; ejderha, jenerik ve boss müzikleri etkilenmez.
- **Davranış**: müziğin ne zaman kısılacağı, geçişler ve Alt+Tab (aşağıda).
- **Ekran**: ESC menüsündeki ve Ayarlar ekranlarındaki düğme (ayrı ayrı), çalan şarkı yazısı, listede ortam sesleri, **Şarkı
  bildirimi** ve **Bildirim ayarları...**.

Tasarım ayrıntıları: [docs/IMPROVEMENTS.md](docs/IMPROVEMENTS.md).

### Davranış sekmesi

- **Plak çalınca** (Kapalı / Kıs %25 / Sustur, varsayılan Sustur): 64 blok içinde bir jukebox'ta plak çalarken arka plan
  müziği kısılır ya da susar, plak bitince yavaşça geri gelir.
- **Alt+Tab'da müzik** (Kapalı / Kıs %30 / Sustur): Alt+Tab ile oyundan çıkınca arka plan müziği kısılır ya da susar.
- **Alt+Tab'da sessiz** (Açık/Kapalı, varsayılan Kapalı): Alt+Tab ile oyundan çıkınca oyunun bütün sesleri susar (müzik,
  efektler, yaratıklar, ortam). Sesler durmaz, -80 dB'e iner; oyuna dönünce kaldıkları yerden duyulur (`FocusMute`).
- **Savaşta** (Kapalı / Biraz %60 / Çok %30): hasar alınca (6 saniye boyunca) ya da 12 blok içinde düşman bir yaratık varken
  müzik kısılır.
- **Yumuşak geçiş** (Kapalı / 2 sn / 5 sn, varsayılan 2 sn): arka plan şarkıları sessizce açılır ve son saniyelerde sessizce
  kapanır.
- **Tekrar etme** (Kapalı / Son 5 / Son 10, varsayılan Son 5): oyun sıradaki şarkıyı seçerken son çalınanları atlar; o
  durumda çalabilecek başka şarkı yoksa yine de çalar.
- **Favoriler** (Kapalı / 2 / 3 / 5 kat, varsayılan 2 kat): ★ ile işaretlenen şarkılar, çalma listesi modu kapalıyken de daha
  sık seçilir.
- **Yumuşak atlama**: açıkken Atla'ya basınca şarkı aniden kesilmez, 1,5 saniyede sesi azalarak biter; bu sırada Atla'ya bir
  daha basılırsa hemen geçer.

Kısma ve yumuşak geçiş sadece arka plan müziğine uygulanır; plaklar, önizleme ve diğer sesler etkilenmez. Birden fazla kısma
aynı anda geçerliyse çarpılır (örneğin savaşta %60 ve arka planda %30 ise müzik %18 çalar).

## Çalma listesi

Paneldeki **Çalma listesi** düğmesi, Ayarlar ekranı ya da `/quietplease playlist`. Bir müzik çalar gibi: üstte çalan şarkı ve
ilerleme çubuğu, altında listenin tamamı numaralı; **▶** şimdi çal, **▲/▼** sırada taşı, **✕** çıkar, **Şarkı ekle** sekmesinde
arama ve mod filtresiyle ekleme, **Atla**, **Karıştır**, **Temizle**. Çalan şarkı listede yerinde durur, başında yeşil çizgi olur;
fareyle üstüne gelinen şarkıda turuncu çizgi çıkar. Çalan şarkı bitince sıradan çıkar ve ardındaki şarkı çalar, liste bitince başa
döner; **∞** düğmesi açık olan şarkı silinmez, yerinde kalır. Geniş ekranda sıranın yanında **Önerilenler** paneli favorileri, daha önce eklenenleri ve
sevilen Minecraft şarkılarını karışık gösterir. Ana paneldeki **☆** şarkıyı favorilere ve sıraya ekler; favoriler panelin en
üstünde **Favoriler** grubunda durur (hiç favori yoksa grup görünmez).
**Çalma listesi modu** açıkken oyun her arka plan şarkısı başlatacağında (menü, oyun, biyomlar, su altı, Nether, End) rastgele
parça yerine listedeki sıradakini çalar. Ejderha/jenerik müziği ve modların doğrudan çaldığı boss müzikleri yine araya girer.
Liste boşsa ya da mod kapalıysa her şey vanilla gibidir. Sıra `config/quietplease.json` dosyasında saklanır.

## Nota blokları ve plaklar

Plaklar (vanilla ve modların plakları) panelde zaten listelenir. Nota bloğu enstrümanları da (`minecraft:note/harp`,
`note/bass`...) **Nota bloğu** türüyle listede görünür: ses seviyesi ayarlanabilir ya da kapatılabilir. Çalma listesine
eklenmezler, ses dengelemesine ve "şu an çalan" yazısına girmezler.

## Geçmiş ve istatistik

Ayarlar ekranındaki **Geçmiş ve istatistik** düğmesi, `/quietplease history` ya da `/quietplease stats`. Üç sekme:
**Son çalanlar** (son 20 arka plan şarkısı, ne zaman çaldığıyla), **En çok çalanlar** (kaç kez çaldığı) ve **Hiç çalmayanlar**.
Her satırda ▶ (şimdi çal), ☆/★ (favori) ve Açık/Kapalı düğmeleri var. **Geçmişi temizle** onay sorar.
Kayıtlar `config/quietplease-stats.json` dosyasında; hazır ayarlara girmez.

**Önceki şarkı** tuşu (ve `/quietplease previous`) geçmişteki bir önceki şarkıyı tekrar çalar; üst üste basınca geçmişte geri
gitmeye devam eder. Tekrar çalınan şarkı geçmişteki yerinde kalır. **Çalan şarkıyı bir daha çalma** tuşu çalan arka plan şarkısını
kapatır ve sıradakine geçer (panelden tekrar açılır).

## Kendi müziğin

`config/quietplease/music/` klasörüne konan `.mp3` ve `.ogg` dosyaları (alt klasörler dahil, en fazla 500) kaynak paketi gerekmeden
panelde en üstte **Kendi müziğin** grubunda görünür: dinlenebilir, ses ayarı yapılabilir, çalma listesine eklenebilir.
Ayarlar ekranındaki **Kendi müzik klasörün** düğmesi klasörü açar; yeni dosyalar paneli bir sonraki açışta görünür.
Ayarlar'ın Müzik sekmesindeki **Kendi müziğin** ayarı açıkken (varsayılan) bu şarkılar oyunun arka plan müziğine de karışır:
her biri yaklaşık bir oyun şarkısı kadar sık çalar. Dosya adları kimliğe çevrilir (`Benim Şarkım.ogg` → `quietplease_music:benim_ark_m`
gibi: küçük harf, harf/rakam dışındakiler `_`); ekranda ise dosyanın gerçek adı görünür (`Benim Şarkım`).
Dosyalar oyunun kaynak yöneticisinden okunur (`ReloadableResourceManagerMixin`).

Minecraft yalnızca OGG çözebildiği için MP3'ler moda gömülü [JavaMP3](https://github.com/delthas/JavaMP3) çözücüsüyle
(MIT, `com.quietplease.lib.javamp3`; CRC, kare arama ve dosya sonu düzeltmeleriyle) çalarken parça parça 16 bit PCM'e
çevrilir (`Mp3`, `SoundBufferLibraryMixin.getStream`). MPEG-1 Layer I/II/III desteklenir (44,1 / 48 / 32 kHz, yani
neredeyse bütün müzik MP3'leri); düşük örnekleme hızlı MPEG-2 dosyaları çalmaz. Süre kare başlıklarından, ses dengeleme
ölçümü dosya çözülerek hesaplanır.

Panelde ve bildirimlerde şarkı adlarındaki alt çizgiler boşluk olarak gösterilir (`boss_theme` → `boss theme`).

## Mod paketi varsayılanı

`config/quietplease-presets/default.json` varsa ve oyuncunun kendi `config/quietplease.json` dosyası yoksa (ilk açılış), oyun
bu hazır ayarla başlar ve hemen oyuncunun dosyasına yazar. Paket yapımcısı ayarlarını `/quietplease export default` ile
hazırlayıp paketine koyar; paket güncellenince oyuncunun değişiklikleri ezilmez. `/quietplease import default` ile paketin
ayarlarına geri dönülebilir.

## Dil desteği

Tüm yazılar `common/src/main/resources/assets/quietplease/lang/` içindeki dosyalardadır: `en_us.json` (İngilizce, varsayılan),
`tr_tr.json` (Türkçe), `de_de.json` (Almanca), `es_es.json` (İspanyolca), `ru_ru.json` (Rusça), `pt_br.json`
(Portekizce, Brezilya) ve `zh_cn.json` (Çince, basitleştirilmiş). Oyun hangi dildeyse o dosya kullanılır; çevirisi olmayan
dillerde İngilizce görünür. Yeni bir dil eklemek için `en_us.json` dosyasını kopyalayıp dil koduyla adlandırmak (örneğin
`fr_fr.json`) ve metinleri çevirmek yeterli. Yeni bir yazı eklenince yedi dosyanın hepsine eklenmeli.

## Sohbet komutları

- `/quietplease` modlara göre müzik sayıları (mod adına tıklayınca liste açılır)
- `/quietplease list <mod>` o modun parçaları, tıklanabilir [−] [+] [Kapat]/[Aç] düğmeleriyle
- `/quietplease volume <parça> <0-100>` parçanın ses seviyesi
- `/quietplease off <parça>` / `/quietplease on <parça>` parçayı kapat / aç
- `/quietplease mod <mod> off` / `on` bir modun tüm parçalarını kapat / aç
- `/quietplease mod <mod> volume <0-100>` bir modun tüm parçalarının ses seviyesi
- `/quietplease reset <parça>` / `/quietplease resetall` ayarları sıfırla
- `/quietplease dump` tam listeyi (ayarlarla birlikte) oyun klasöründeki `quietplease-tracks.txt` dosyasına yazar
- `/quietplease gui` paneli açar
- `/quietplease settings` / `/quietplease playlist` Ayarlar ekranını / çalma listesi panelini açar
- `/quietplease skip` çalan arka plan şarkısını atlar, `/quietplease previous` geçmişteki önceki şarkıyı çalar
- `/quietplease history` / `/quietplease stats` geçmiş ve istatistik ekranını açar
- Kısayol tuşları: **Options → Controls → Key Binds → Quiet Please** altında "Müzik panelini aç", "Çalan şarkıyı atla",
  "Önceki şarkı", "Müziği duraklat / devam", "Çalan şarkıyı favorilere ekle" (bir daha basınca çıkarır),
  "Çalan şarkıyı bir daha çalma" ve "Çalma listesi modunu aç/kapat"
  (varsayılan olarak boş; istediğin tuşu ata). Sonuç hotbar'ın üstünde kısa bir yazıyla görünür.
- `/quietplease export <ad>` / `/quietplease import <ad>` tüm ayarları (çalma listesi dahil)
  `config/quietplease-presets/<ad>.json` olarak kaydeder / yükler

Ayarlar `config/quietplease.json` dosyasında saklanır (parça ayarları, `mods`, `options` ve `playlist` bölümleri; eski dosyalar aynen yüklenir).

## Geliştirici notu

Sadece geliştirme ortamında (`runClient`) ve `QUIETPLEASE_AUTODUMP` ortam değişkeni ayarlıyken: ana menüde küçük bir
öz-test çalışır (parça atlama, ses çarpanı, mod ses çarpanı, çalma listesi seçimi, şarkı arası, hazır ayar), müzik listesi
`quietplease-tracks.txt` dosyasına yazılır ve `QUIETPLEASE_AUTODUMP=gui` ise panel, Ayarlar ekranı ve çalma listesi paneli
açılıp ekran görüntüsü alınır; bir deneme dünyasında çalma listesinin oyun müziğini devraldığı, Atla'nın şarkıyı yumuşakça
kısıp sıradakine geçtiği kontrol edilir ve şarkı bildiriminin sağda ve solda ekran görüntüsü alınır. Sonuçlar test oyununun `logs/latest.log`
dosyasında `SELFTEST` / `GUITEST` olarak görünür. Gerçek oyunda (launcher'dan) bu kod hiç çalışmaz.
