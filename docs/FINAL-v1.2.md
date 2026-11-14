# Završni status Rez Audio Studio v1.2

Status: pripremljen lokalni nacrt; javna objava nije odobrena.

## Sačuvano

Pet jezika (en/bs/de/it/es), audio funkcije i Seal integracija ostaju bez promjena u ovom koraku. Android paket je rs.rez.audio, versionName 1.2 i versionCode 3. Licenca je Apache-2.0; LICENSE, NOTICE i THIRD_PARTY_NOTICES.md uključuju se u izvore i APK.

## Provjere i ograničenja

- Core testovi: 124 assertions PASS; nezavisne FFmpeg provjere: 54 PASS.
- Pet jezičkih kataloga i parametri, dijagnostičke poruke te konfiguracije u izgrađenom nepotpisanom APK-u provjereni.
- Ručni unsigned build provjeren; razvojni ili nepotpisani APK se ne preimenuje u produkcijski prilog.
- Lokalni repozitorij priprema se iz eksplicitno odabranih izvora: bez build izlaza, fixture binarnih datoteka, ključeva, credential datoteka i lokalnih metapodataka. Pregled odnosi se na taj novi repozitorij; izvorna Git historija nije dostupna.
- Nosilac autorskih prava i porijeklo postojećeg izvora/ikone još nisu potvrđeni. Apache-2.0 odluka ne zamjenjuje tu potvrdu. Nisu uočene uključene biblioteke s konfliktnom licencom; build-alati i odvojeni Seal zadržavaju vlastite uslove.
- Instrumentacijski i vizuelni testovi na Android uređaju nisu izvršeni zbog ADB ograničenja. Gradle/lint nisu izvršeni; potvrđen je ručni build.
- Xiaomi 12 nije mijenjan; u ovom koraku nisu pokretane ADB komande.

## Produkcijski potpis — još nije izvršen

Odobren identitet: CN=Rez Audio Studio, O=Rez Audio Studio, C=BA. Bez ličnih imena ili drugih privatnih podataka u certifikatu.

Produkcijski ključ nije generisan jer jedino dozvoljeno odredište izvan projekta u ovoj sesiji predstavlja privremeni storage. Vlasnik je odabrao `$HOME/.local/share/rez-signing` za trajnu mapu ključa. Sandbox nema dozvolu pisanja na toj lokaciji; ključ nije generisan u privremenoj mapi. Za trajno spremanje potreban je lokalni postupak iz RELEASE-PROCESS.md, s nezavisnim backup medijem i sigurnim čuvanjem lozinke.

Dok to nije izvršeno, ne postoje potvrđen produkcijski APK, produkcijski otisak certifikata niti njegov SHA-256. Ne objavljivati bilo koji postojeći razvojni APK kao Rez-Audio-Studio-v1.2.apk.

## GitHub

Pripremljeni naziv repozitorija: Rez-Audio-Studio; oznaka Release-a: v1.2. Lokalni nacrt treba pregledati prije povezivanja s GitHub-om. Remote repozitorij i GitHub Release nisu kreirani niti objavljeni.

Prilozi nakon uspješnog produkcijskog potpisa: Rez-Audio-Studio-v1.2.apk, odgovarajući .sha256 i javni izvještaj potpisa, LICENSE, NOTICE, THIRD_PARTY_NOTICES.md. Nikad keystore, lozinka, testni APK ili build direktorij.
