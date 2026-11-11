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
