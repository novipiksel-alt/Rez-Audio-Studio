# Rez Audio Studio 1.2 — Android

Rez je jednostavna Android aplikacija za izrezivanje audio snimaka i izradu zvona. Podržava Android 10+ (API 29+). Audio se obrađuje lokalno na telefonu, bez naloga i bez reklama.

## Mogućnosti

- Uvoz audio datoteka iz Android birača ili dijeljenjem iz druge aplikacije.
- Prikaz talasnog oblika, zumiranje dugmadima i s dva prsta, horizontalno pomjeranje.
- Izbor početka i kraja, preslušavanje označenog dijela.
- Izrezivanje bez ponovnog kodiranja za podržane formate. Rez kod komprimovanih formata zavisi od granica frejmova.
- Spremanje isječaka u `Music/Rez` ili `Ringtones/Rez` i opcionalno postavljanje zvona.
- YouTube → audio prečica otvara **zasebno instaliranu** aplikaciju [Seal](https://github.com/JunkFood02/Seal). Seal nije uključen u Rez i preuzimanje zavisi od pravila servisa i prava na sadržaj.

## Izgradnja (CachyOS / Linux)

Potrebni su JDK 17, Android SDK platforme 36 i Build Tools 36.0.0.

```bash
ANDROID_SDK_ROOT=/opt/android-sdk ./scripts/build.sh
```

Skripta kreira lokalni razvojni ključ u `build/manual/debug.keystore` ako ne postoji i izlazni APK `dist/Rez-1.2.apk`. **Razvojni ključ nikada ne slati na GitHub.** Za javnu distribuciju pripremiti poseban produkcijski ključ, sačuvati ga van repozitorija i izgraditi/potpisati izdanje odgovarajućim postupkom.

## Testiranje

```bash
./scripts/test.sh
```

Dana 14.11.2026. korisnički test na Xiaomi 12 potvrdio je 7/7 osnovnih funkcija (učitavanje, prikaz, zumiranje, pomjeranje, izbor raspona, izvoz). To nije automatska potvrda kompatibilnosti sa svim Android uređajima.

## Distribucija i licenciranje

Projekt koristi **Apache License 2.0**; puni tekst je u [LICENSE](LICENSE). Napomene o alatima i odvojenim aplikacijama su u [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Provjera autorskog porijekla i potvrda završnog izdanja još su otvorene.
APK za javnu distribuciju treba potpisati zasebnim produkcijskim ključem. Dosad korišteni razvojni APK nije javno izdanje.

## Priprema javnog izdanja

Repozitorij za objavu: **Rez-Audio-Studio**. Produkcijski postupak i obavezne provjere su u [docs/RELEASE-PROCESS.md](docs/RELEASE-PROCESS.md), a rezultati pregleda i otvorene stavke u [docs/REVIEW-1.1.md](docs/REVIEW-1.1.md).

`./scripts/build.sh --unsigned` gradi bez kreiranja razvojnog ključa. `./scripts/sign-release.sh` koristi zaseban vanjski ključ i provjerava njegov unaprijed poznati certifikat. `./scripts/package-source.sh` priprema nacrt arhive izvora bez ključeva, APK-ova i binarnih testnih datoteka. Testni zvukovi se generišu lokalno; za testove su potrebni FFmpeg/ffprobe, Python 3 i JDK 17.

Javna objava čeka potvrdu autorskog porijekla, provjeru produkcijskog APK-a na uređaju i izričitu potvrdu završnog izdanja.

## Jezici (1.2)

Interfejs podržava engleski (podrazumijevani), bosanski, njemački, italijanski i španski. Android automatski bira resurse prema sistemskim jezicima; za nepodržane jezike koristi se engleski. Nema zasebnog birača jezika u aplikaciji.

Prevodi su u `app/src/main/res/values/strings.xml`, `values-bs`, `values-de`, `values-it` i `values-es`. Prevedeni su dijalozi, statusi, dugmad, greške i opisi za čitače ekrana. Vremenski unos i zapis zadržavaju isti format `mm:ss.SSS` (prihvaća se i decimalni zarez). Audio engine i pravila Seal integracije ostaju isti.

Provjere kataloga: `python3 tests/verify_localization.py`; provjere ugrađenih prevoda: `python3 tests/verify_localization.py dist/Rez-1.2.apk`. Testovi uređaja koriste ID-jeve umjesto prevedenih oznaka i uključuju pet jezika, regionalne varijante, engleski fallback te kontrole na 320 dp uz font 100%, 150% i 200%.

Razvojni APK `dist/Rez-1.2.apk` nije javno izdanje. Rezultati i obavezna vizuelna provjera: [docs/REVIEW-1.2.md](docs/REVIEW-1.2.md).

## Konačni kandidat za GitHub v1.2

Naziv produkcijskog priloga je `Rez-Audio-Studio-v1.2.apk`. Njegov certifikat mora imati identitet `CN=Rez Audio Studio, O=Rez Audio Studio, C=BA`; razvojni APK nije zamjena za ovaj prilog. Produkcijski ključ i lozinka nisu dio izvora niti release paketa.

[Postupak izdanja](docs/RELEASE-PROCESS.md) opisuje sigurnu lokalnu pripremu ključa, rezervnu kopiju i provjere. [Završni status](docs/FINAL-v1.2.md) razlikuje izvršene provjere od preostalih. Priprema i testiranje ne instaliraju ništa na Xiaomi 12. Repo i Release ostaju neobjavljeni do završne provjere.
