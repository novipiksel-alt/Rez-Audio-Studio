# Sigurnosna provjera GitHub paketa v1.2

Prethodna poruka `Personal path or possible secret detected` nastala je u tadašnjem `scripts/prepare-github.sh`, red 23. Originalni uslov sadržavao je doslovni obrazac korisničke home putanje i doslovni zabranjeni nadimak. Pri skeniranju source arhive taj je uslov pronašao sam sebe. Reprodukcija originalnog reda u memoriji potvrdila je oba lažno pozitivna nalaza. Nisu bili pronađeni stvarni keystore, privatni ključ ili lozinka.

Provjera je izdvojena u `scripts/verify-package-security.py`. Uzorci više ne odgovaraju vlastitom izvornom zapisu. Nalazi prikazuju samo naziv datoteke, broj reda i kategoriju; pronađene vrijednosti se ne ispisuju. Provjeravaju se korisničke home putanje, privatni identitet, poznati oblici privatnih ključeva i pristupnih tokena, kao i neželjene datoteke ključeva, credentials i build izlaza. To je provjera poznatih obrazaca, ne garancija otkrivanja svake moguće tajne.

Pokretanje bez objave:

```bash
./scripts/package-source.sh
python3 scripts/verify-package-security.py dist/Rez-Audio-Studio-1.2-source-DRAFT.zip
python3 scripts/verify-package-security.py dist/github/Rez-Audio-Studio
```

Paket i lokalni Git indeks ne uključuju produkcijski ključ. Predviđena trajna mapa ključa je izvan projekta; njeno generisanje nije izvršeno u ovoj sesiji. Nisu promijenjeni Java kod, prevodi, audio funkcije, Seal integracija niti instalacija telefona. Nema javne objave.

## Naknadno stanje produkcijskog kandidata — 14.11.2026.

Prethodni odlomci opisuju raniju pripremu. Novi produkcijski ključ sada postoji izvan projekta, s nezavisnim identičnim backupom; vlasnik je lozinku postavio interaktivno. Produkcijski APK je potpisan i testiran na Xiaomi 12. Vlasnik je potvrdio prava objave; postojeći GitHub Release prvo je privremeno povučen u draft, a zatim uz završno odobrenje ponovo objavljen s verificiranim novim APK-om. Aktuelne hash vrijednosti, provedene provjere, ograničenja i status javne objave su u FINAL-v1.2.md. Privatni ključevi i lozinke ostaju isključeni iz svih priloga.
