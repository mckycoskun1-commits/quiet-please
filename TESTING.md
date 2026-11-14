# Quiet Please – elle test listesi

Otomatik testlerin göremediği şeyler. Her maddeyi deneyip yanına ✅ / ❌ koyman yeterli;
❌ olanları neyin olduğunu bir cümleyle yaz.

## A. Geliştirme oyununda (hızlı)

Proje klasöründe:

```
.\gradlew.bat runClient
```

1. [ ] Dünyadayken **ESC** → sağ üstte **♪ Quiet Please** düğmesi, altında çalan şarkının adı var.
1. [ ] Ana menü → **Options** → sağ üstte **♪ Quiet Please** düğmesi var ve paneli açıyor.
2. [ ] **Options → Music & Sounds** ekranında da aynı düğme var.
3. [ ] **Mods → Quiet Please → Config** paneli açıyor.
4. [ ] Panelde **▶**'a basınca parça çalıyor, **■** ile duruyor; çalan satırda ekolayzır zıplıyor.
    - Oyunun Müzik sesi %0 iken de önizleme duyuluyor.
    - Panel açıkken (tek oyunculu) oyun duruyor.
    - Üstteki **Now playing** çubuğu çalan müziği gösteriyor; tıklayınca o parça listede çıkıyor.
    - Oyunda çalan parçanın satırında yeşil **playing** etiketi var.
5. [ ] **Ses kaydırıcısını fareyle sürüklemek** çalışıyor ve önizleme çalarken ses anında değişiyor.
6. [ ] Bir parçayı **Kapalı** yap → adı üstü çizili, ikonu kararık görünüyor.
7. [ ] Bir mod başlığında **Hepsini kapat / Hepsini aç** çalışıyor.
8. [ ] **Arama kutusu** ve **Mod** filtresi listeyi daraltıyor; Mod düğmesinde sağ tık bir önceki moda dönüyor; **Ortam: Göster** Nether ortam seslerini ekliyor.
9. [ ] Paneli kapatıp oyunu yeniden açınca ayarlar duruyor.
10. [ ] Bir dünyaya gir, sohbete `/quietplease` yaz → mod adına tıklayınca liste geliyor; **[−] [+] [Kapat] [Aç]** düğmeleri çalışıyor.
11. [ ] Creative'de bir **Jukebox** koy, *Music Disc - cat* tak:
    - `/quietplease volume minecraft:records/cat 20` → ses hemen kısılıyor.
    - `/quietplease off minecraft:records/cat` → plak hemen susuyor.
12. [ ] **Options → Language → Türkçe** seç: panel ve komutlar Türkçe görünüyor; İngilizceye dönünce İngilizce.

## B. Gerçek oyunda (asıl test)

Bitmiş mod dosyası: `dist/quietplease-1.0.0.jar`

1. [ ] CurseForge / Prism / Modrinth uygulamasında **NeoForge 1.21.1** profili oluştur, jar'ı `mods` klasörüne at, oyun açılıyor.
2. [ ] Gerçek bir mod paketine ekle (ör. All the Mods 10 ya da kendi paketin) → oyun açılıyor, panel açılıyor.
3. [ ] Panelde paketteki modların müzikleri kendi adlarıyla gruplanmış görünüyor.
4. [ ] Sevmediğin bir modun müziklerini **Hepsini kapat** ile kapat → bir süre oyna, o müzikler hiç çalmıyor.
5. [ ] **The Aether** ile dene (geliştirme ortamında Aether'ın içindeki Accessories kütüphanesi çöküyor;
       bu Quiet Please'tan değil, ama gerçek oyunda da sorun olmadığını görmek lazım).
6. [ ] Boss müziği olan bir modla dene (Cataclysm, Mowzie's Mobs): boss müziği kapatılınca çalmıyor / kısılınca kısılıyor.
7. [ ] Bir sunucuya bağlan (sunucuda Quiet Please yok) → sorunsuz bağlanıyor.

## C. `improvements` dalı (1.1.0): Ayarlar, çalma listesi, Atla, hazır ayarlar

Önce dalı aç ve test oyununu başlat:

```
git switch improvements
.\gradlew.bat runClient
```

Bu dalın jar'ı `dist/quietplease-1.1.0.jar` olur (1.0.0 jar'ına dokunmaz).

### Ayarlar
1. [ ] Panelde sol üstte **Settings / Ayarlar** düğmesi Ayarlar ekranını açıyor; **Done** panele geri dönüyor.
2. [ ] **Now playing text: OFF** → ESC menüsünde ve Options'ta düğmenin yanındaki şarkı adı kayboluyor.
3. [ ] **Button in ESC menu: OFF** → ESC menüsünde ♪ Quiet Please yok; **Button in Options: OFF** → Options ve Music & Sounds'ta yok.
       Panel yine **Mods → Quiet Please → Config** ve `/quietplease gui` ile açılıyor.
4. [ ] **Ambient in list: ON** → panel her açıldığında "Ambient: Show" ile başlıyor.
5. [ ] **Music gap: None** → bir dünyada bir şarkı bitince sıradaki ~1 sn içinde başlıyor. **Short** → 30–60 sn.
       **Custom** → kaydırıcılar aktifleşiyor; en kısa > en uzun yapılamıyor (diğeri kendini düzeltiyor).
6. [ ] Oyunu kapatıp açınca ayarlar duruyor (`run/config/quietplease.json` içinde `"options"`).

### Çalma listesi
7. [ ] Panelde bir parçanın **☆** düğmesine bas → **★** oluyor; Mod filtresinde **★ Playlist** sadece onları gösteriyor.
8. [ ] Sağ üstte **Playlist / Çalma listesi** → panel açılıyor; **Add songs** sekmesinde arama ve mod filtresi çalışıyor, **+ Add** / **✓ Added**.
9. [ ] **Queue** sekmesinde **▲/▼** sırayı değiştiriyor, **✕** çıkarıyor, **Clear** onay soruyor.
10. [ ] **Playlist mode: ON**, **▶** bir şarkıda → o şarkı hemen çalıyor, üstte ilerleme çubuğu ve süre (ör. 0:12 / 3:21) ilerliyor;
        ESC'de (tek oyunculu) oyun durunca süre de duruyor.
11. [ ] **Skip ⏭** → sıradaki şarkı hemen başlıyor. **Shuffle** → çalan şarkı çalmaya devam ediyor, liste karışıyor.
        Çalan şarkı listede yerinde duruyor ve başında yeşil çizgi var; fareyle üstüne gelinen şarkıda turuncu çizgi çıkıyor.
        Ortadaki bir şarkıda **▶** → şarkı yerinden oynamıyor, bitince ardındaki çalıyor.
12. [ ] **Repeat: Stop at end** → son şarkı bitince vanilla müzik dönüyor ve üst çubukta "playlist ended" yazıyor.
13. [ ] Ana menüde çalma listesi şarkısı çalarken menü müziği onu kesmiyor; bir dünyaya girince liste devam ediyor.
14. [ ] **Boss müziği (asıl test):** `run/mods` içinde **L_Ender's Cataclysm** varken çalma listesi modu açık, Settings → Music gap: None.
        Creative'de bir boss çağır (ör. `/summon cataclysm:ignis` ya da `cataclysm:netherite_monstrosity`):
        boss müziği çalıyor (listeden bağımsız). Boss ölünce/uzaklaşınca liste kaldığı yerden devam ediyor.
        Not: modun doğrudan çaldığı boss müziği MusicManager'dan geçmediği için, liste şarkısı o sırada da çalabilir (vanilla'da da böyle).
15. [ ] **Ejderha:** End'de ejderha savaşı müziği liste şarkısını kesiyor (`/execute in minecraft:the_end run tp @s 0 80 0`).
16. [ ] Bir parçayı **Off** yap → listede üstü çizili ve sırası gelince atlanıyor.
17. [ ] Oyunu kapatıp açınca sıra, tekrar modu ve en son çalan yer korunuyor.

### Atla ve hazır ayarlar
18. [ ] Ana panelin "Now playing" çubuğundaki **Skip** ve sohbette `/quietplease skip`:
        liste kapalıyken şarkı duruyor, sıradaki şarkı arası ayarına göre geliyor (Vanilla'da yani uzun süre gelmeyebilir).
        Plak çalarken ya da hiç müzik yokken düğme pasif / komut "çalan arka plan müziği yok" diyor.
19. [ ] `/quietplease export test1` → `run/config/quietplease-presets/test1.json` oluşuyor (sohbetteki ada tıklayınca klasör açılıyor).
        Bir şeyleri değiştir, `/quietplease import test1` → eski hal geliyor (çalma listesi dahil). `import` yazarken ad öneriliyor.
20. [ ] `/quietplease export ../x` reddediliyor.

### Kısayol tuşları ve ayar dosyası
21. [ ] **Options → Controls → Key Binds** listesinde **Quiet Please** başlığı ve iki tuş var (ikisi de boş). Birine tuş ata:
        dünyadayken basınca panel açılıyor / şarkı atlanıyor ve hotbarın üstünde kısa bir yazı çıkıyor. Sohbet açıkken tuş bir şey yapmıyor.
22. [ ] Panel ve çalma listesi başlığında logo yerine **Jukebox** eşyası görünüyor.
23. [ ] Oyunu kapat, `run/config/quietplease.json` dosyasını bozup (ör. ilk satırı sil) oyunu aç: oyun açılıyor, varsayılanlar geliyor
        ve yanında `quietplease.json.broken` kopyası duruyor.
24. [ ] Ana panelde Mod filtresi **★ Çalma listesi** iken bir şarkının ★ düğmesine bas: şarkı listeden hemen kalkıyor.

### 1.3.0: şarkı bildirimi, yumuşak atlama, mod sesi, yeni tuşlar, diller
25. [ ] Dünyada yeni bir müzik başlayınca sağ kenarın ortasından küçük bir kart kayarak çıkıyor (ikon, şarkı adı, mod adı,
        oynayan çubuklar), 4 saniye kadar sonra sağa geri giriyor. Ekranın ortasına hiç gelmiyor. F1 basılıyken görünmüyor.
26. [ ] Settings → **Popup** yerini değiştir: her değişiklikte örnek kart o kenarda görünüyor. Sol, orta / Sağ, alt / Sol, üst
        konumlarında Xaero's Minimap ya da JourneyMap ile üst üste binmiyor. **Song popup: Off** iken hiç kart çıkmıyor.
27. [ ] **Fade on skip** açıkken Atla: şarkı 1,5 saniyede kısılarak bitiyor, sonra sıradaki geliyor. Kısılırken Atla'ya tekrar
        basınca hemen geçiyor. Ayar kapalıyken eskisi gibi hemen kesiyor.
28. [ ] Panelde bir mod başlığındaki **Mod: 100%** kaydırıcısını %50 yap: o modun çalan şarkısı hemen kısılıyor, şarkının kendi
        kaydırıcısı değişmiyor. `/quietplease mod minecraft volume 30` aynısını yapıyor. Oyunu kapatıp açınca ayar duruyor.
        Favoriler başlığında bu kaydırıcı yok.
29. [ ] Key Binds'te dört tuş var. "Favorite the current song" tuşu çalan şarkıyı favorilere ekliyor (★ yazısı), bir daha basınca
        çıkarıyor; müzik yokken "çalan müzik yok" diyor. "Toggle playlist mode" tuşu modu açıp kapatıyor, liste boşsa uyarıyor.
30. [ ] Oyun dilini Deutsch, Español (España), Русский, Português (Brasil) ve 简体中文 yap: panel, Ayarlar ve çalma listesi o dilde,
        yazılar düğmelerden taşmıyor.
