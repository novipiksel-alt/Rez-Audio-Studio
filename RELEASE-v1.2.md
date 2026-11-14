# Rez Audio Studio v1.2

Status: produkcijski kandidat je potpisan i testiran; postojeći Release je draft do završne potvrde vlasnika. Novi APK još nije ponovo javno objavljen.

Rez Audio Studio lokalno izrezuje audio zapise i sprema isječke, bez računa i oglasa. Pet jezika: engleski, bosanski, njemački, italijanski i španski. Talas, zum/pomjeranje, izbor raspona, preview i izvoz bez ponovnog kodiranja za podržane formate. Seal je zasebna aplikacija i nije uključen.

Android 10+, paket `rs.rez.audio`, verzija `1.2` (3), minSdk 29, targetSdk 35. Apache-2.0; licence i napomene su u prilozima i APK assets.

Produkcijski APK: `Rez-Audio-Studio-v1.2.apk`, 103095 bajt.
SHA-256: `9922af6503b858da5cac39e395ddab8bdd42d14c05873aba875089299a805922`.
Certifikat: `CN=Rez Audio Studio, O=Rez Audio Studio, C=BA`.
SHA-256 certifikata: `8cfed1510ed9d1de649b04c8bc9d9f7347026a4db1ad5e8fe4364d370f8b7a70`.

Raniji javni APK povučen je jer vlasnik nije znao lozinku njegovog ključa. Novi certifikat ne može direktno nadograditi raniju instalaciju s drugim potpisom. Prije deinstalacije sačuvati rad; deinstalacija briše privatne podatke i cache. Na Xiaomi 12 ovo je izvršeno uz odobrenje vlasnika. Datoteke izvora i sačuvani izvozi izvan privatnih podataka aplikacije nisu mijenjani.

Provjere: 124 Java, 54 FFmpeg, pet jezičkih kataloga i konfiguracije u APK-u; potpis, poravnanje i sadržaj paketa PASS. Tačan instalirani APK ima identičan hash. Vođeni test Xiaomi 12 / Android 15: birač datoteka, WAV, talas, zum/pomjeranje, raspon, preview, rezanje, čuvanje, otvaranje/zvuk i sistemski izbor dijeljenja PASS; original nepromijenjen, izvezeni PCM identičan traženom dijelu. Pet početnih ekrana provjereno, puni tok na bosanskom. Instrumentation, mali ekran/font matrica i Gradle/lint nisu izvršeni.

Build commit: `af8511be8d54e7acb639d64dd43a75f85c8839d3`. Tag `v1.2` ostaje na `b39adcef23a056b177f5d556f8b488a051c0ebee`: aplikacija i build ulazi su identični, razlika su samo README i screenshotovi. Dokumentacijska revizija na main bilježi završni status. Detalji: [FINAL-v1.2.md](docs/FINAL-v1.2.md).

Nakon odobrene objave preuzeti APK i .sha256 iz ovog Release-a, provjeriti `sha256sum -c Rez-Audio-Studio-v1.2.apk.sha256`. Javno preuzimanje novog APK-a mora se ponovo verificirati prije proglašenja završetka.
