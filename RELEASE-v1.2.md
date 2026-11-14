# Rez Audio Studio v1.2

Android aplikacija za lokalno izrezivanje audio zapisa i spremanje isječaka. Pet jezika: engleski, bosanski, njemački, italijanski i španski. Talas, zum/pomjeranje, izbor raspona, preview i izvoz bez ponovnog kodiranja za podržane formate. Seal je zasebna aplikacija i nije uključen u APK.

Android 10+, paket `rs.rez.audio`, versionName `1.2`, versionCode `3`, minSdk 29, targetSdk 35. Licenca: Apache-2.0; LICENSE, NOTICE i THIRD_PARTY_NOTICES.md su priloženi i ugrađeni u APK.

## Produkcijski APK

- `Rez-Audio-Studio-v1.2.apk`: **103095 bajt**.
- SHA-256: `9922af6503b858da5cac39e395ddab8bdd42d14c05873aba875089299a805922`.
- Certifikat: `CN=Rez Audio Studio, O=Rez Audio Studio, C=BA`.
- SHA-256 certifikata: `8cfed1510ed9d1de649b04c8bc9d9f7347026a4db1ad5e8fe4364d370f8b7a70`.
- RSA 3072; certifikat važi do 1. aprila 2054. Potpis (APK Signature Scheme v3), poravnanje i sadržaj paketa verificirani.

Preuzmite APK i checksum iz priloga, pa provjerite:

```bash
sha256sum -c Rez-Audio-Studio-v1.2.apk.sha256
```

## Izvršene provjere i ograničenja

**Prošli su 124 Java testne provjere (assertions), 54 nezavisne FFmpeg provjere, lokalizacijske provjere i vođeni ručni test osnovnog rada na Xiaomi 12 / Android 15. Gradle/lint i kompletni instrumentation testovi nisu izvršeni.**

Na Xiaomi 12 testiran je tačan produkcijski APK: instalirani base.apk izvučen je kroz ADB i njegov SHA-256 identičan je odobrenom lokalnom APK-u. Potvrđeni su birač datoteka, WAV uvoz, waveform, 400% zum i horizontalno pomjeranje, izbor raspona, preview sa zaustavljanjem, rezanje, čuvanje, otvaranje i zvuk izvoza te sistemski izbor za dijeljenje. Nijedna datoteka nije poslata primatelju. Izvezeni WAV sadrži identične originalne PCM uzorke za izabrani raspon; original i postojeći izvozi ostali su nepromijenjeni. Relevantni dostupni logcat zapisi nakon potvrđenih koraka nisu pokazali pad aplikacije.

Provjereni su katalozi i pet jezičkih konfiguracija u APK-u te početni ekrani na svih pet jezika. Puni audio tok na uređaju potvrđen je na bosanskom. Nisu izvršeni puni tok na ostala četiri jezika, instrumentation matrica malog ekrana/uvećanih fontova, svi formati na telefonu ili nezavisna lektura prevoda. Skeniranje poznatih obrazaca tajni nije potpuni sigurnosni audit.

## Zamjena ranijeg priloga i nadogradnja

Raniji javni APK (107191 bajt, SHA-256 `c8a78473de0f509735cf7e25880e2502dc1eab470829a1b009a680ea416e8956`) imao je validan projektni potpis, ali vlasnik nije znao lozinku njegovog ključa i nije mogao potvrditi održavanje tim ključem. Release je privremeno povučen u draft. Uz izričito odobrenje vlasnika isti Release sada dobija novi produkcijski APK potpisan novim trajnim ključem s provjerenim nezavisnim backupom. Raniji ključevi i backup nisu prepisani ili obrisani.

**Novi certifikat ne može direktno nadograditi instalaciju potpisanu starim certifikatom.** Prije deinstalacije sačuvajte rad: deinstalacija briše privatne podatke, cache i nesačuvane isječke. Izvorne datoteke i sačuvani izvozi izvan privatnih podataka aplikacije ostaju. Na Xiaomi 12 deinstalacija je izvršena tek nakon zasebnog odobrenja vlasnika.

Build commit: `af8511be8d54e7acb639d64dd43a75f85c8839d3`. Tag `v1.2` ostaje na `b39adcef23a056b177f5d556f8b488a051c0ebee`: aplikacijski izvori i build ulazi identični su; razlika do build commita je README i dva screenshota. Tag nije pomjeran. Dokumentacija na main bilježi produkcijsku provjeru i objavu: [završni status](https://github.com/novipiksel-alt/Rez-Audio-Studio/blob/main/docs/FINAL-v1.2.md).

Prilozi su isključivo produkcijski APK, njegov checksum, javni izvještaj potpisa i tri licencne datoteke. Privatni ključ, keystore, lozinka, razvojni APK i build direktoriji nisu priloženi.

## Provjera nakon javne objave

14.11.2026. u 02:56:02 UTC svih šest priloga ponovo je anonimno preuzeto bez autentifikacije i upoređeno s lokalno odobrenim datotekama: identični su. Javna Release stranica i direktni APK link vraćaju HTTP 200. Javni APK ima 103095 bajt i SHA-256 `9922af6503b858da5cac39e395ddab8bdd42d14c05873aba875089299a805922`; apksigner potvrđuje tačan produkcijski identitet i SHA-256 certifikata `8cfed1510ed9d1de649b04c8bc9d9f7347026a4db1ad5e8fe4364d370f8b7a70`. Zipalign PASS.
