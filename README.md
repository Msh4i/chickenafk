# ChickenAFK

<img src="src/main/resources/assets/chickenafk/icon.png" width="128" alt="ChickenAFK">

> **Resmî değildir.** ChickenAFK, chickennw ile bağlantılı değildir ve chickennw tarafından onaylanmamıştır. Hayran yapımı, bağımsız bir moddur.

ChickenAFK, chickennw SMP'deki AFK hesaplarını oyunun içinden yönetmen için bir panel. Hesaplarını eklersin, tek tıkla başlatırsın; mod her hesabı arka planda sunucuya bağlar, giriş yapar, SMP'ye geçer ve AFK alanına götürür. Sen o sırada oynamaya ya da ana menüde beklemeye devam edersin.

## Özellikler

- **Kendi paneli:** Siyah-beyaz, sade bir arayüz. Oyun içinde **K** tuşuyla, ana menüde sağ alttaki kartla açılır.
- **Otomatik giriş:** Sunucu `/gir` istediğinde kayıtlı şifreni gönderir. Aynı istek tekrar gelirse şifreyi yeniden göndermez, deneme hakkın yanmaz.
- **AFK takibi:** Hangi hesabın AFK alanında olduğunu, bir sonraki ödüle kalan süreyi ve kristal sayını (sunucunun yazdığı gibi, örn. `23.9k`) gösterir.
- **Envanter:** Her hesabın envanterini gerçek eşya görselleriyle görürsün; tıklayarak eşya taşıyabilirsin.
- **Menüler:** Bot bir sandık ya da sunucu menüsü açınca panelde görünür, içine tıklayabilirsin.
- **Sohbet:** Her hesabın sohbetini okuyabilir, o hesap adına yazabilirsin.
- **Makrolar:** SMP'ye girdikten sonra çalışacak kendi komutlarını ekleyebilirsin (örn. `/home 1`).
- **Kopunca yeniden bağlanır:** Bağlantı koparsa bir süre bekleyip kendiliğinden tekrar girer.
- **İlk açılış turu:** İlk açılışta paneli tanıtan kısa bir tur var; "Bir daha gösterme" ile kapatabilirsin.
- **Hafif:** Botlar dünyayı yüklemez ve çizmez (chunk ve varlıkları yok sayar), sadece gereken paketleri işler.
- **Güncelleme bildirimi:** Yeni sürüm çıkınca panelde ve ana menüde görünür.

## Kullanım

1. Paneli aç (**K** ya da ana menüdeki kart).
2. **Hesap ekle** ile hesap adını ve (varsa) `/gir` şifresini gir.
3. **Hepsini başlat** ile bütün hesapları ya da bir hesabı seçip **Başlat** ile tek tek çalıştır.

## Şifreler

Şifreler sadece senin bilgisayarında, oyun klasöründeki `chickenafk/hesaplar.json` dosyasında saklanır. Mod şifreyi hiçbir yere göndermez; sadece chickennw sunucusu `/gir` istediğinde o sunucuya yazar. Sohbet panelinde ve `chickenafk/sohbet.log` dosyasında şifre `••••` olarak gizlenir.

## Gereksinimler

- Minecraft **1.21.11**
- Fabric Loader **0.16** veya üstü
- **Fabric API**

## Notlar

- Sunucunun kurallarına uymak senin sorumluluğundadır. Mod, chickennw'nun izin verdiği AFK alanı sistemini kullanmak için yapıldı.
- Kaynak kod: [github.com/Msh4i/chickenafk](https://github.com/Msh4i/chickenafk) (MIT lisansı)

## Yapımcı

**MsHai** · [TikTok](https://www.tiktok.com/@mshai_1) · [GitHub](https://github.com/Msh4i) · Discord: `freakyms`

## Derleme

Java 21 gerekir (Gradle kendisi indirir).

```
./gradlew build
```

Jar dosyası `build/libs/` içinde çıkar.

Geliştirme için:

- `./gradlew runClient -Pshots` arayüzü test eder ve ekran görüntüsü alır.
- `./gradlew runServer` yerel bir test sunucusu açar.
- `./gradlew prodBotTest` botu o sunucuya karşı, oyuncuların kullandığı haliyle test eder.

## Lisans

Kod MIT lisanslıdır (`LICENSE`). Geist yazı tipleri SIL Open Font License altındadır (`src/main/resources/LICENSE-Geist-OFL.txt`).
