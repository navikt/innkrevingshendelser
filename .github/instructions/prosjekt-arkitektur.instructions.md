---
applyTo: "src/**/*.kt"
---

# Prosjektregler: Ports & Adapters, TDD og navngiving

Kilden er `docs/utviklingsregler.md`. Les den før du endrer struktur eller legger
til kode. Reglene utfyller Navs generelle instruksjoner og går foran ved konflikt
om kodestruktur. Sikkerhetsreglene gjelder alltid.

## Ports & Adapters

- Lag: `adapter.<teknologi>` → `application` (porter i `port.inn` og `port.ut`) → `domain`.
- `domain` og `application` kan bare avhenge av Kotlin stdlib, `kotlinx.coroutines`
  og `arrow-core`. Aldri Ktor, Kafka, Jackson, Micrometer, database eller logging.
- Utgående porter er interfaces i domenespråk. Adaptere eier serialisering og
  tekniske feil, og oversetter unntak til `Left`.
- Porter returnerer `Either<Feil, Resultat>` med sealed-domenefeil.
- Kobling skjer i oppstartskoden med konstruktørinjeksjon. Ikke Koin uten avklaring.
- Tekniske metrikker telles i adapteren. Forretningsmetrikker telles i en dekoratør
  rundt den inngående porten. Label-verdier kommer aldri fra hendelsesdata.

## Testdrevet utvikling

- Skriv testen først, kjør den og vis at den feiler før du implementerer.
  Kan du ikke kjøre testene, ber du utvikleren kjøre dem og venter på bekreftet rød test.
- Test brukstilfeller gjennom den inngående porten med håndskrevne fakes av
  utgående porter. Ikke mock egne porter eller domeneklasser.
- Test både `Right` og relevante `Left`-varianter.
- Bruk Kotest `WordSpec`: `"subject" should { "do something" { } }`, med `When` som ytre nivå ved behov.
- Testklasser ligger i samme pakke som koden de tester. Felles testhjelpere ligger i `kotest`.
- I rød sone lager du bare tester og stubber, med mindre utvikleren ber om mer.
- Endre aldri en eksisterende test for å få den grønn uten å si fra.
- Oppgi hvilke tester du kjørte, og resultatet. Si fra hvis de ikke kunne kjøres.

## Dokumentasjon

- Ingen TODO-er, oppgavelister eller midlertidige tilstander i dokumentasjon eller skills.
  Det som gjenstår, registreres som GitHub-issue.

## Urelaterte funn

- Oppdager du noe som ikke hører til oppgaven, fiks det ikke i samme endring. Vis et utkast
  til GitHub-issue med bakgrunn og akseptansekriterier, og opprett det etter godkjenning.
- Unntak: det som trengs for at oppgaven skal virke, og sikkerhetsfeil som ikke kan vente.

## Navngiving

- Domeneklasser og egenskapene deres har norske navn (`Innkrevingshendelse`, `Krav.beløp`).
  Alt annet skrives på engelsk, også testnavn.
- Æ, ø og å er tillatt i `domain` og `application`. Ikke i `adapter`, og ikke i serialiserte
  feltnavn, topic-navn, metrikknavn, URL-er eller miljøvariabler.
