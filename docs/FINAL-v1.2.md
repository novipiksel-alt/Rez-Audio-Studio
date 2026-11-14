# Završni status Rez Audio Studio v1.2

Status 14.11.2026: produkcijsko izdanje javno objavljeno uz završno odobrenje vlasnika. Isti postojeći GitHub Release v1.2 je ažuriran, a svih šest javnih priloga ponovo je anonimno preuzeto i verificirano. Produkcijski APK odgovara lokalno odobrenom i testiranom APK-u.

## Revizija ranijeg javnog APK-a

Raniji asset Rez-Audio-Studio-v1.2.apk: 107191 bajt; SHA-256 `c8a78473de0f509735cf7e25880e2502dc1eab470829a1b009a680ea416e8956`. Potpis je validan, identitet CN=Rez Audio Studio, O=Rez Audio Studio, C=BA, RSA 3072; SHA-256 certifikata `d120740d28739d29e77dcb204de8466ea9fb92ef2020d2bf500b7e980a82796d`. Nije razvojni certifikat. Svih sedam payload datoteka identično je svježem unsigned buildu provjerenog izvora. Pregled arhive i skeniranje poznatih obrazaca nisu pronašli privatne ključeve, tokene, lokalne putanje ili razvojne datoteke.

Vlasnik je naknadno potvrdio da ne zna lozinku tog ključa. Po njegovom izričitom nalogu postojeći Release prebačen je u draft. U trenutku skrivanja tag, izvori i raniji asset nisu obrisani. Prilikom odobrene završne objave samo raniji APK asset zamijenjen je novim produkcijskim APK-om. Anonimni Release API i direktni APK link nakon skrivanja vratili su 404. Stari ključ i njegov backup nisu prepisani ili obrisani; lozinka nije pogađana.

## Novi produkcijski kandidat

- APK: `Rez-Audio-Studio-v1.2.apk`; veličina 103095 bajt.
- SHA-256: `9922af6503b858da5cac39e395ddab8bdd42d14c05873aba875089299a805922`.
- Paket `rs.rez.audio`; versionName `1.2`; versionCode `3`; minSdk 29; targetSdk 35.
- Certifikat: `CN=Rez Audio Studio, O=Rez Audio Studio, C=BA`.
- SHA-256 certifikata: `8cfed1510ed9d1de649b04c8bc9d9f7347026a4db1ad5e8fe4364d370f8b7a70`.
- RSA 3072; važenje 14.11.2026–01.04.2054 (10000 dana).
- Zipalign i apksigner verifikacija PASS; APK Signature Scheme v3 PASS.
- Checksum i javni izvještaj potpisa pripremljeni. Sadržaj odgovara unsigned buildu; tri pravne datoteke u assets identične su izvoru.

Novi ključ je trajno izvan projekta u `$HOME/.local/share/rez-signing/rez-release-v1.2-maintained.p12`. Direktorij ima prava 700, keystore 600. Backup je na eksternom disku u direktoriju `Rez-Signing-Backup`, pod istim novim nazivom i s pravima 600. Keystore datoteke su identične bajt po bajt i imaju jednake SHA-256 vrijednosti. Javni certifikati izvezeni iz originala i backupa također su identični. Vlasnik je postavio lozinku direktno kroz interaktivni terminal; nova lozinka nije upisana u kod, argumente, logove, dokumentaciju ili environment datoteku.

## Izvor i tag

Čist build commit: `af8511be8d54e7acb639d64dd43a75f85c8839d3`; tada su main i origin/main bili poravnati. Tag `v1.2` ostaje na `b39adcef23a056b177f5d556f8b488a051c0ebee`. Razlika do build commita su samo README i dva screenshota; aplikacija i svi build ulazi su identični. Objavljeni tag se ne pomjera. Kasnija dokumentacijska revizija na main bilježi stvarne rezultate i ne mijenja APK.

Vlasnik je u ovoj sesiji 14.11.2026. potvrdio pravo objave pod Apache-2.0 za projektni kod, testove, skripte, XML resurse, vektorsku ikonu i screenshotove. To je izjava vlasnika; ne predstavlja nezavisno dokazivanje autorstva. Starije napomene o nepotvrđenom porijeklu u ugrađenim NOTICE/THIRD_PARTY_NOTICES predstavljaju historijsko stanje prije ove potvrde. Nije izmišljen novi nosilac autorskih prava niti mijenjana licenca. Seal nije uključen.

## Izvršeni testovi

- 124 Java assertions PASS; 54 nezavisne FFmpeg provjere PASS, ponovljene u ovoj sesiji.
- Lokalizacija: svi katalozi, ključevi, parametri, mapiranje dijagnostike i pet konfiguracija u tačnom produkcijskom APK-u PASS.
- Sigurnosna provjera praćenog izvora i source paketa; pregled APK sadržaja i poznatih obrazaca tajni. Ograničenje: skeniranje obrazaca nije potpuni sigurnosni audit.
- Xiaomi 12 / 2201123G, Android 15: instalirani APK izvučen kroz ADB, hash identičan odobrenom lokalnom kandidatu.
- Sigurna nadogradnja prvo pokušavana; Android je odbio različit potpis. Deinstalacija izvršena tek nakon izričitog odobrenja vlasnika. Xiaomi je USB instalaciju odbio, pa je vlasnik instalirao identičan preneseni APK kroz telefon.
- Vođeni ručni test: pokretanje, birač datoteka, WAV uvoz, waveform, 400% zum, horizontalno pomjeranje, raspon 00:01.235–00:03.789, preview sa samostalnim zaustavljanjem, rezanje, čuvanje, otvaranje i zvuk izvoza, sistemski izbor za dijeljenje PASS. Dijeljenje nije poslalo datoteku.
- Izvoz WAV: 450572 bajt, 44,1 kHz, stereo, 16-bit PCM, 112632 frejma, 2,554013605 s. SHA-256 `d6426d3d4c289cc753c9c8c257bac328897f6c97014bb6885f930968e7fdff5c`. PCM uzorci su identični odgovarajućem rasponu originala.
- Originalni testni WAV: SHA-256 `485eca261a43b309847394278a6e68b652af0a32ebd9db2eae6aba6054c0a74c`; ostao nepromijenjen nakon svakog ručnog koraka. Raniji izvozi također nepromijenjeni.
- Nakon svake korisničke potvrde provjereni su UI/stanje aplikacije, dostupni hashovi i relevantni dostupni logcat zapisi; u pregledanim zapisima nema pada aplikacije. To nije neprekidan zapis svih sistemskih događaja.
- Svih pet početnih ekrana provjereno kroz Android app locale konfiguraciju; override je vraćen na praznu listu (sistemski jezik). Puni audio tok testiran na bosanskom; ostala četiri jezika nisu zasebno prošla puni ručni audio tok.

Nisu izvršeni: Android instrumentation suite, matrica 320 dp i fontova 100/150/200%, Gradle/lint, provjera svih formata na telefonu ili nezavisna lektura prevoda. Testovi uređaja nisu zamijenjeni razvojnim APK-om. Nije mijenjan INJECT_EVENTS niti sigurnosne postavke telefona. Funkcije, dizajn i podržani formati aplikacije nisu mijenjani.

## Završna objava i anonimna provjera

Vlasnik je izričito odobrio zamjenu starog APK-a i ponovnu javnu objavu. Prvo je u istom draftu zamijenjen APK i dodan checksum, javni izvještaj potpisa, LICENSE, NOTICE i THIRD_PARTY_NOTICES.md. Svih šest uploadovanih priloga preuzeto je i upoređeno s lokalnim bajtovima dok je Release još bio draft. Zatim je objavljen isti Release, bez duplikata: ID `RE_kwDOVtwRv84ahNsl`.

Objavljeno 14.11.2026. u 02:55:34 UTC. Završna anonimna provjera završena u 02:56:02 UTC, bez GitHub tokena, autentifikacije ili kolačića:

- Javna Release stranica: HTTP 200.
- Direktni APK download: HTTP 200, 103095 bajt.
- SHA-256 javno preuzetog APK-a: `9922af6503b858da5cac39e395ddab8bdd42d14c05873aba875089299a805922`; identičan lokalnom odobrenom APK-u i instaliranom APK-u s Xiaomi 12.
- Apksigner: validan potpis; tačan DN `CN=Rez Audio Studio, O=Rez Audio Studio, C=BA`; SHA-256 certifikata `8cfed1510ed9d1de649b04c8bc9d9f7347026a4db1ad5e8fe4364d370f8b7a70`.
- Zipalign javno preuzetog APK-a PASS. Svih šest javno preuzetih priloga identično je lokalnim odobrenim datotekama.
- Release opis izričito navodi prolazak 124 Java, 54 FFmpeg i lokalizacijskih provjera te ručnog osnovnog testa na Xiaomi 12, uz neizvršene Gradle/lint i kompletne instrumentation testove.
- Nisu objavljeni keystore, lozinka, privatni ključ, razvojni/testni APK, .idsig ili build direktoriji.

Dokumentacija nakon provjere commitovana je i pushovana na main; objavljeni tag ostao je nepromijenjen. Potpuni SHA dokumentacijskog commita nalazi se u Git historiji i Release opisu.

Release: https://github.com/novipiksel-alt/Rez-Audio-Studio/releases/tag/v1.2
Direktni APK: https://github.com/novipiksel-alt/Rez-Audio-Studio/releases/download/v1.2/Rez-Audio-Studio-v1.2.apk
