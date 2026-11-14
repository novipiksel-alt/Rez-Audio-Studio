# Produkcijsko izdanje 1.2

Status: nacrt, javna objava nije odobrena. Repozitorij: Rez-Audio-Studio.

## Ključ i potpis

Produkcijski ključ se ne generiše bez izričite potvrde vlasnika. Nakon potvrde ključ kreira vlasnik na pouzdanom računaru, izvan projekta i cloud-sinhroniziranih mapa. Primjer (keytool interaktivno traži lozinku):

```bash
umask 077
mkdir -p "$HOME/rez-signing"
keytool -genkeypair -keystore "$HOME/rez-signing/rez-release.jks" -alias rez-release -keyalg RSA -keysize 3072 -validity 10000
keytool -list -v -keystore "$HOME/rez-signing/rez-release.jks" -alias rez-release
```

Zabilježiti SHA-256 certifikata nezavisno od APK-a. Sačuvati šifriranu offline rezervnu kopiju ključa i lozinke odvojeno. Isti ključ treba za buduće nadogradnje. Ključ, lozinke i lokalne postavke nikad ne dodavati u Git ni release priloge.

```bash
export REZ_RELEASE_KEYSTORE="$HOME/rez-signing/rez-release.jks"
export REZ_RELEASE_ALIAS=rez-release
export REZ_RELEASE_CERT_SHA256='<zabilježeni SHA-256 certifikata>'
./scripts/test.sh
./scripts/sign-release.sh
```

Skripta gradi svjež nepotpisan APK, interaktivno traži lozinke, provjerava poravnanje, potpis i očekivani certifikat te odbija poznati razvojni identitet. Izlaz: `dist/Rez-Audio-Studio-v1.2.apk`, `.sha256` i izvještaj certifikata. Nikad ne koristiti `scripts/build.sh` razvojni APK kao javni prilog. Gradle release potpis nije konfigurisan; podržani postupak je navedena skripta s Build Tools 36.0.0 i SDK 36, JDK 17, zip i ripgrep.

## Provjere i potvrda

1. Apache-2.0 je odabrana i LICENSE je dodan. Vlasnik još potvrđuje nosioca prava i porijeklo koda/ikone. Binarne fixture datoteke se ne objavljuju. Provjeriti LICENSE, NOTICE i THIRD_PARTY_NOTICES.md u izvorima, APK assets i release prilozima.
2. Završiti petojezičnu provjeru prema docs/REVIEW-1.2.md. Ponoviti `scripts/test.sh` i `scripts/test-device.sh` na dostupnom testnom Android 10+ uređaju. Device skripta instalira razvojni APK i testni APK; ne koristiti je preko produkcijske instalacije s drugim certifikatom.
3. Produkcijski APK zasebno instalirati na čist testni profil/uređaj. Provjeriti uvoz biračem i dijeljenjem, talas, zum/pomjeranje, izbor, preslušavanje, izvoz, dijeljenje, preimenovanje i opcionalno zvono. Provjeriti nadogradnju s prethodnog izdanja potpisanog istim ključem ako postoji. Razvojna instalacija s drugim potpisom ne može se direktno nadograditi.
4. Pregledati sadržaj source arhive i priloga, skenirati tajne i eventualnu Git historiju. `.gitignore` ne uklanja već praćene tajne. Ovdje historija nije dostupna.
5. Priložiti SHA-256 APK-a i otisak certifikata završnom izvještaju. Tražiti izričitu potvrdu vlasnika za konačni APK, oznaku v1.2 i javni repozitorij Rez-Audio-Studio.
6. Tek nakon potvrde napraviti javni repozitorij/push/tag/Release. U release idu isključivo produkcijski APK, checksum i javni certifikat/izvještaj; nikad keystore, build, testni ili razvojni APK.

Gubitak ključa onemogućava uobičajenu nadogradnju direktno distribuiranih APK-ova. Bez ključa i stvarnog uređaja postupak je pripremljen, ali produkcijsko izdanje nije validirano.

## Odobrena priprema produkcijskog identiteta

Vlasnik je odobrio generisanje novog ključa s identitetom `CN=Rez Audio Studio, O=Rez Audio Studio, C=BA`. Koristiti dvije namjenske prazne trajne mape izvan projekta; backup treba biti na drugom mediju. Sandbox ove sesije dopušta pisanje izvan projekta samo u privremenu mapu, pa generisanje tu nije izvršeno.

Pokrenuti lokalno, s vlastitim trajnim lokacijama:

```bash
export REZ_KEY_DIRECTORY="$HOME/.local/share/rez-signing"
# Postaviti stvarnu namjensku mapu na zasebnom trajnom backup mediju.
: "${REZ_BACKUP_DIRECTORY:?Postavi mapu rezervne kopije na zasebnom mediju}"
./scripts/create-production-key.sh "$REZ_KEY_DIRECTORY" "$REZ_BACKUP_DIRECTORY"
export REZ_RELEASE_KEYSTORE="$REZ_KEY_DIRECTORY/rez-release.p12"
export REZ_RELEASE_ALIAS=rez-release
export REZ_RELEASE_PASSWORD_FILE="$REZ_KEY_DIRECTORY/store-password.txt"
export REZ_RELEASE_CERT_SHA256="$(cat "$REZ_KEY_DIRECTORY/certificate-sha256.txt")"
./scripts/sign-release.sh
```

Skripta generiše 48 nasumičnih bajtova lozinke, čuva ih u datoteci s pravima 600 u mapi 700 i predaje lozinku alatima preko datoteke, ne vrijednosti argumenta. Ne koristiti shell tracing. Lozinka se ne ispisuje. Kopija PKCS12 keystorea provjerava se bajt po bajt i otvaranjem pomoću keytool-a. Lozinku dodatno sačuvati u menadžeru lozinki ili na zasebnom sigurnom mediju; rezervna kopija keystorea sama nije dovoljna za oporavak. Skripta ne kopira lozinku uz backup ključa.

Potpisivanje provjerava identitet certifikata, unaprijed zabilježeni SHA-256 certifikata, verziju 1.2 (3), poravnanje i svih pet lokalizacija. Izlaz su produkcijski APK, njegov checksum i javni izvještaj potpisa. Za produkcijsko testiranje koristiti poseban testni uređaj/profil. **Ne pokretati test-device.sh, adb install, uninstall niti bilo kakvu promjenu na Xiaomi 12 u okviru ovog izdanja.**

Pripremljeni lokalni repozitorij je u ignorisanom izlaznom direktoriju `dist/github/Rez-Audio-Studio`. Nema remote-a ni objave. `dist/github/release-v1.2.json` je lokalni nacrt metapodataka; nije GitHub Release. Poslije provjere prava, potpisa, backupa i prikaza na testnom uređaju vlasnik odobrava konkretan APK i javnu objavu. Tek tada povezati remote i kreirati Release; ne pokretati upload tokom pripreme.
