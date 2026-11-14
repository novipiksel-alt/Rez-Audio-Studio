# Produkcijsko izdanje 1.2

Ova procedura zamjenjuje ranije upute o lokalnom nacrtu i automatskom generisanju lozinke. Stvarni status i otisci su u [FINAL-v1.2.md](FINAL-v1.2.md). Repozitorij, tag i postojeći GitHub Release već postoje; ne praviti duplikat.

## Trajni ključ

Aktivni novi produkcijski ključ: `$HOME/.local/share/rez-signing/rez-release-v1.2-maintained.p12`, alias `rez-release`. Stari `rez-release.p12` ima drugi certifikat i nepoznatu lozinku; ne prepisivati ga, ne brisati i ne pokušavati pogoditi lozinku.

Direktorij ključa mora imati prava 700, keystore 600. Original i nezavisni backup novog keystorea na eksternom disku već su provjereni bajt po bajt, SHA-256 vrijednostima i identičnim izvezenim certifikatima. Backup postoji u namjenskom direktoriju `Rez-Signing-Backup`, pod nazivom `rez-release-v1.2-maintained.p12`. Ne čuvati jedinu kopiju lozinke uz backup ključa; vlasnik je čuva odvojeno.

Lozinku postavlja i unosi vlasnik direktno kroz interaktivni keytool/apksigner upit. Nikada je ne slati u chat, kod, argumente, log, shell historiju, dokumentaciju ili environment datoteku. Ne koristiti raniji `create-production-key.sh`: njegova automatska lozinka/datoteka lozinke ne odgovara ovoj proceduri. Novi ključ je već kreiran; ne pokretati novu generaciju radi ponovnog builda.

Ako ikada bude potreban drugi novi ključ, koristiti novi naziv koji ne postoji, RSA 3072, najmanje 25 godina i tačan DN `CN=Rez Audio Studio, O=Rez Audio Studio, C=BA`. Izvoz javnog certifikata iz originala i backupa mora dati identične DER datoteke. Promjena certifikata prekida običnu nadogradnju prethodnih instalacija i zahtijeva posebno razmatranje migracije.

## Build i potpisivanje postojećim novim ključem

Početi iz čistog provjerenog commita, uporediti main s origin/main i provjeriti tag. Pokrenuti `./scripts/test.sh`, lokalizacijsku i sigurnosnu provjeru izvora/source paketa. Podržani build je ručni, SDK 36 / Build Tools 36.0.0 / JDK 17. Gradle/lint nisu validirani.

```bash
export REZ_RELEASE_KEYSTORE="$HOME/.local/share/rez-signing/rez-release-v1.2-maintained.p12"
export REZ_RELEASE_ALIAS=rez-release
export REZ_RELEASE_CERT_SHA256=8cfed1510ed9d1de649b04c8bc9d9f7347026a4db1ad5e8fe4364d370f8b7a70
unset REZ_RELEASE_PASSWORD_FILE
./scripts/sign-release.sh
```

Skripta pravi svjež unsigned build, izvršava zipalign prije potpisivanja, traži lozinku interaktivno i provjerava certifikat, verziju i jezičke konfiguracije. Izlaz su `dist/Rez-Audio-Studio-v1.2.apk`, `.apk.sha256` i `-verification.txt`. Provjeriti apksigner i zipalign, package `rs.rez.audio`, verziju `1.2` / `3`, minSdk 29 i targetSdk 35. Sadržaj payload datoteka mora odgovarati unsigned buildu; pregledati sve APK unose i tajne. Development APK nikada nije produkcijski prilog. Stare .idsig datoteke iz ranijih buildova nisu odobreni prilozi.

## Stvarni uređaj

Instalirati tačan verificirani APK; nakon instalacije izvući `base.apk` i uporediti SHA-256. Prvo pokušati sigurnu nadogradnju. Ako potpis nije kompatibilan, objasniti gubitak privatnih podataka i tražiti izričito odobrenje prije deinstalacije. Ne koristiti `test-device.sh` preko produkcijske instalacije: instalira razvojni APK. Instrumentacijski testni APK za produkcijski paket mora imati kompatibilan potpis; trenutno nije izvršen.

Na Xiaomi 12 vlasnik je odabrao vođeni ručni test bez promjene sigurnosnih postavki. Nakon svakog potvrđenog koraka pregledati UI, relevantni logcat, originalni zvuk i dostupne izvoze/hashove. Pokriti birač datoteka, waveform, zum/pomjeranje, izbor raspona, preview, rezanje, čuvanje, otvaranje i sistemski izbor dijeljenja. Ne slati fajl drugim osobama. Provjeriti tačan audio sadržaj izvoza i da original ostaje nepromijenjen. Provjeriti pet jezika koliko je dostupno i navesti ograničenja.

## Odobrenje i postojeći GitHub Release

Za v1.2 ova objava je završena: vlasnik je odobrio priloge, isti Release ponovo je javno objavljen i anonimno provjeren. Sljedeće upute opisuju obavezne korake postupka, a ne preostali posao ovog izdanja.

Dokumentaciju uskladiti sa stvarnim rezultatima, commitovati i pushovati na main. Ne pomjerati objavljeni tag bez jasnog izvještaja i opravdanja; za ovaj kandidat postojeći v1.2 tag ima identične izvore aplikacije i build ulaze.

Prije javne zamjene tražiti završnu potvrdu vlasnika za konkretni APK i SHA-256. Release do tada ostaje draft, sa sačuvanim starim assetom. Nakon potvrde ažurirati isti Release, zamijeniti stari APK i postaviti samo:

- `Rez-Audio-Studio-v1.2.apk`
- `Rez-Audio-Studio-v1.2.apk.sha256`
- `Rez-Audio-Studio-v1.2-verification.txt`
- `LICENSE`, `NOTICE`, `THIRD_PARTY_NOTICES.md`

Nikad keystore, lozinku, privatni ključ, razvojni/testni APK, .idsig, build direktorij ili lokalnu konfiguraciju. Provjeriti uploadovane bajtove dok je Release draft. Objaviti tek odobreni skup priloga. Ponovo anonimno preuzeti javni APK, potvrditi njegov SHA-256 i potpis naspram odobrenog lokalnog APK-a te provjeriti javne linkove. Zabilježiti rezultat i tek tada proglasiti izdanje završenim.
