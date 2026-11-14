# Lokalizacija i završna provjera 1.2 — 14.11.2026.

Status: lokalni razvojni kandidat, nije odobren za javnu objavu.

## Izmjene

Engleski je u `values/strings.xml`; bosanski, njemački, italijanski i španski u `values-bs`, `values-de`, `values-it` i `values-es`. Svaki jezik ima 141 korisnički tekst. Verzija je zaseban neprevodivi resurs: 1.2. Gradle i glavni manifest koriste versionName 1.2 / versionCode 3; build, sign i package skripte koriste nazive izlaza 1.2.

MainActivity i RangeView više ne sadrže fiksne korisničke tekstove. Dinamički tekstovi koriste pozicione parametre (naziv datoteke, vrijeme, kodek, frekvencija, kanali i mjesto spremanja). Android bira jezik prema konfiguraciji sistema; aplikacija ne postavlja Locale.setDefault niti koristi vlastiti birač. Promjena sistemskog jezika koristi standardni Android životni ciklus.

AudioEngine.java, LosslessAudio.java i TimeRange.java poređeni su s kopijama prije lokalizacije: identični su bajt po bajt. Njihove interne dijagnostičke poruke ostaju postojeći ugovor parsera. Novi LocalizedErrors prevodi te poruke na UI granici pomoću bosanskog kataloga i odgovarajućeg aktivnog resursa, uključujući poziciju neispravnog frejma i naziv nepodržanog kodeka. Nepoznate poruke platforme ili dobavljača datoteke zamjenjuju se lokalizovanom općom greškom. Interna dijagnostika se ne prikazuje direktno korisniku. Test provjerava da svaka postojeća throw poruka ima mapiranje.

Seal paket, intent akcije/MIME tipovi, URL i HTTPS provjera domena nisu mijenjani. Potpisivanje koristi isti postojeći razvojni postupak; mijenjana su samo imena artefakata na 1.2. Produkcijski ključ nije generisan.

## Duži tekst i mali ekrani

Glavni ekran i sadržaj složenih dijaloga imaju ScrollView. Dugmad koriste wrap_content visinu, višeredni tekst i minimalni dodirni cilj od 52 dp. Kontrole zuma, pomjeranja granica i radnje nad isječkom prelaze u vertikalni raspored kada je širina ispod 360 dp ili fontScale iznad 1.3. Polja vremena su jedno ispod drugog, a položaj preslušavanja ispod dugmeta. Naslovi dijaloga koriste višeredni TextView; radnje dijaloga na uskom prikazu koriste vertikalni raspored kad platforma daje LinearLayout panel. Potrebna je stvarna provjera dijaloga na ciljnom Android/OEM sistemu.

PlatformTests koristi ID-jeve polja umjesto bosanskih oznaka. Dodane su provjere formatiranja svih stringova u pet jezika, regionalnih de-AT/es-MX varijanti, francuskog fallbacka na engleski, lokalizacije grešaka i nepoznate dijagnostike. Dodana je provjera stvarnih kontrola u izmjerenom rasporedu 320 × 568 dp s fontovima 100%, 150% i 200%, za svaki od pet jezika. To su testovi pripremljeni za izvršavanje na uređaju, ne potvrda da su već prošli.

## Izvršene provjere

- Java core testovi: 124 assertions PASS.
- Nezavisne FFmpeg provjere: 54 PASS.
- verify_localization.py: svih pet kataloga, jednakost ključeva, neprazni prevodi, formatni parametri, pokriće dijagnostike, verzija i sistemski izbor jezika PASS.
- Provjera aapt2 dump resources: 142 string resursa; svih 141 prevodivih stringova ima konfiguracije default/en, bs, de, it, es PASS.
- Ručna izgradnja aplikacije i instrumentacijskog testnog APK-a PASS. SDK 36 / Build Tools 36.0.0 / JDK 17.
- Manifest iz APK-a: rs.rez.audio, versionName 1.2, versionCode 3, minSdk 29, targetSdk 35.
- Razvojni potpis APK-a verificiran apksigner-om; certifikat CN=Rez Development. To nije produkcijski potpis.
- Audio engine i TimeRange neizmijenjeni. `bash -n scripts/*.sh` PASS.

## Preostalo prije javne objave

ADB ne može otvoriti socket niti USB u ovom okruženju; Android instrumentation i vizuelni prikaz nisu izvršeni. Nema /dev/kvm za ubrzani emulator. Gradle/lint putanja nije izvršena jer nema Gradle wrappera/instalacije; potvrđen je dokumentovani ručni build.

Na testnom uređaju pokrenuti `./scripts/test-device.sh`. Zatim, za svaki sistemski jezik en/bs/de/it/es na 320–360 dp i uvećanom fontu provjeriti:

1. Početni ekran, editor, zum, unos vremena i čitač ekrana.
2. Uvoz, preslušavanje, izvoz, spremanje, dijeljenje i preimenovanje.
3. Dijalog spremanja s dugim imenom i porukom greške; odbijanje dozvole i dijalog sistemskih postavki zvona.
4. Seal dijalog s tri radnje i porukama neispravnog linka/neinstalirane aplikacije; ne mijenjati njegovu integraciju.
5. Neispravnu audio datoteku, nepodržani kodek i pogrešan raspon — bez bosanskih dijagnostičkih poruka u drugim jezicima.
6. Rotaciju, povratak u aplikaciju i promjenu sistemskog jezika bez gubitka postojeće logike vraćanja izvora.

Provjeriti da nema odsječenih dugmadi, nečitljivih naslova ili nedostupnih radnji; sačuvati screenshotove za svih pet jezika. Prevodi su implementirani i programski provjereni, ali nisu nezavisno lektorisani od izvornih govornika.

Ranija potvrda nosioca prava i porijekla izvora ostaje otvorena (docs/REVIEW-1.1.md). Produkcijski ključ/potpis zahtijeva posebno odobrenje. Javna objava i dalje zahtijeva završnu provjeru i izričitu potvrdu vlasnika za konkretan APK i njegov SHA-256.

## Provjera čiste arhive i lokalni kandidat

Source arhiva 1.2 raspakovana je u novu privremenu mapu. Iz nje su bez postojećih fixture datoteka prošli svi core/FFmpeg/lokalizacijski testovi i unsigned build. Provjerene su sve jezičke konfiguracije u tako izgrađenom APK-u i identičnost ugrađenih LICENSE/NOTICE/THIRD_PARTY_NOTICES datoteka. Source arhiva ne sadrži APK ili ključeve.

Završni lokalni razvojni APK: `dist/Rez-1.2.apk`.
SHA-256: `9607f68413d6bb1670def52cdc38fbf1007b6452c9ffa663e7d9a21c57777397`.
Poravnanje potvrđeno zipalign provjerom. Nije javno izdanje niti produkcijski potpis.
