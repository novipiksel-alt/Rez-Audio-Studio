# Pregled pripreme 1.1 — 14.11.2026.

## Opseg i nalazi

Pregledani su svi Java izvori, manifest, resursi, build/test skripte i deklaracije zavisnosti. Nove funkcije aplikacije nisu dodane.

- Nema INTERNET dozvole, mrežnog klijenta, analitike, ugrađenog Seala ili biblioteka trećih strana u izvorima. Seal se otvara eksplicitnim intentom; korisnički link ograničen je na HTTPS YouTube domene.
- Izvoz koristi MediaStore, IS_PENDING i uklanja neuspjele upise. Uvoz koristi content URI i internu privremenu datoteku, s granicom 2 GB i provjerom prostora. Ime dobavljača ne koristi se kao lokalna putanja. Backup je isključen. WRITE_SETTINGS se koristi za opcionalno zvono.
- Pretraga dostupnih tekstualnih izvora nije pronašla privatne ključeve ni poznate obrasce tokena. Lozinka `android` postoji samo kao javno poznata razvojna konfiguracija u skriptama. Izgrađeni razvojni ključ ostaje lokalno isključen iz source paketa.
- Ispravljena zastarjela putanja Rez-1.0.apk u testu uređaja; build uklanja stare klase/dex; testni build se osvježava. Dodani unsigned build, produkcijska skripta i izuzeća za lokalne tajne.
- Preostali rizik dostupnosti: limit uvoza 2 GB ne ograničava broj Frame objekata u MP3/ADTS indeksu. Vrlo duga/zlonamjerna datoteka može iscrpiti memoriju. Parsiranje/codec fuzzing i potpuni sigurnosni audit nisu provedeni. Platformski dekoderi zavise od sigurnosnih zakrpa uređaja.

## Izvršena validacija

- JDK 17.0.20.1: 124 Java assertions PASS.
- FFmpeg n9.0.2: 54 nezavisne provjere formata, dekodiranja i identičnosti paketa/PCM-a PASS.
- `scripts/build.sh --unsigned`: PASS, SDK 36 / Build Tools 36.0.0.
- `bash -n scripts/*.sh`: PASS.
- ADB: nije dostupan zbog sandbox socket/USB ograničenja; Android instrumentation nije izvršen. Ranije navedeni Xiaomi 12 rezultat je prethodni korisnički izvještaj, nije ponovljen u ovoj sesiji.
- Produkcijski ključ nije kreiran niti dostavljen. Stvarno produkcijsko potpisivanje nije izvršeno. Gradle build/lint nije izvršen (nema wrappera); validiran je dokumentovani ručni build.

## Licence i porijeklo

Apache License 2.0 odabrana je izričitom odlukom vlasnika. Dodan je puni LICENSE, identičan lokalnoj SPDX kopiji po sadržaju bez razmaka i provjeren prema službenom Apache tekstu: https://www.apache.org/licenses/LICENSE-2.0.txt . Dodani su NOTICE, THIRD_PARTY_NOTICES.md i SPDX oznake u izvorima, testovima, skriptama i konfiguraciji. Pravne datoteke ulaze u source arhivu i APK assets; za release ih treba priložiti i uz APK.

Pregled svih pet Java izvora aplikacije, dva Java testa, Python testa, sedam shell skripti, XML resursa i Gradle konfiguracije nije otkrio vendorizirani kod ni zavisnosti izvan Java/Android API-ja. Nema runtime Gradle zavisnosti. APK sadrži samo projektne klase i generisane klase u rs.rez.audio. To nije dokaz da neki fragment nikada nije preuzet iz drugog projekta.

**Nije moguće bezuslovno potvrditi pravo na objavu svakog izvornog fajla:** nedostaju Git historija, identitet nosioca prava i potvrda porijekla postojećeg Java koda, testova, skripti i ikone. Pitanje je upućeno vlasniku. Nije izmišljen nosilac copyrighta. Ako vlasnik navede preuzeti kod, potrebno je pojedinačno provjeriti izvor i obaveze prije objave. Dodavanje SPDX oznake ne uklanja tuđe uslove.

Binarni fixture fajlovi nemaju potvrđeno porijeklo; cijeli tests/fixtures direktorij isključen je iz Gita i source arhive. Svježe generisanje sintetičkih testnih zvukova treba potvrditi testom iz čiste source arhive.

Detaljan inventar platforme, build alata i odvojene GPLv3 aplikacije Seal je u THIRD_PARTY_NOTICES.md. Nema uključene kopije Seala. Gradle tranzitivni alati nisu razriješeni/auditirani; puni Gradle audit ostaje otvoren. Produkcijski ključ se ne kreira bez nove izričite potvrde vlasnika.

## Granica pregleda

Mapa .git je prazna/read-only; ovo nije funkcionalan Git checkout. Nije bilo moguće provjeriti remote, grane, praćene datoteke ni historiju. Ništa nije objavljeno. Source arhiva je nacrt dok vlasnik ne potvrdi prava na izvore i završno izdanje.

## Završna lokalna provjera nakon izbora Apache-2.0

- Nacrt source arhive raspakovan u novu privremenu mapu bez fixture datoteka i build izlaza. Svježe generisanje fixture zvukova uspjelo.
- Iz te arhive ponovljeni testovi: 124 Java assertions + 54 FFmpeg provjere PASS.
- Iz te arhive `build.sh --unsigned`: PASS. LICENSE, NOTICE i THIRD_PARTY_NOTICES.md u APK assets identični su izvornim datotekama.
- `dexdump`: svih 64 klase u lokalnom APK-u pripada rs.rez.audio (uključujući generisane klase).
- `bash -n scripts/*.sh`: PASS. Source arhiva uključuje pravne datoteke i ne sadrži fixture binarne datoteke, APK, keystore ili poznate obrasce privatnih ključeva/tokena. Ovo je ograničena provjera obrazaca, ne garancija odsustva svake moguće tajne.
- Gradle konfiguracija za uključivanje pravnih assets dodana je, ali Gradle putanja nije izvršena: Gradle/wrapper nisu dostupni. Validiran je ručni build.
- Android instrumentation, test produkcijskog potpisa i test na stvarnom uređaju ostaju otvoreni. Nije kreiran produkcijski ključ, izvršeno produkcijsko potpisivanje niti obavljena javna objava.
