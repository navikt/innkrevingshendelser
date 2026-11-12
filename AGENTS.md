# innkrevingshendelser

Kotlin/Ktor-prosjekt for fremtidig publisering av innkrevingshendelser til Kafka,
eid av team utenlandsadresser. Foreløpig kjører bare health- og metrics-endepunkter.
Kafka-produsenten er en læringsoppgave med TODO-stubber, ikke en ferdig integrasjon.
Ingen API-polling, publiserings-API, database eller Kafka-konsument inngår i applikasjonen.
Kun syntetiske testdata uten personopplysninger er tillatt nå.

## Bygg og kjør lokalt

Java-versjonen styres av `mise.toml`. Gradle-versjonen styres av wrapperen.
Les `mise.toml` før du kjører `mise trust`. Docker-motoren installeres separat
og må kjøre når Kafka-integrasjonstestene senere aktiveres.

```bash
mise trust
mise install
mise run check
mise run build
mise run run
```

`mise run test` kjører testene. `mise run build` kjører også `installDist`.
Rapporten ligger i `build/reports/tests/test/index.html`.
Tre Ktor-tester er aktive. Fire Kafka-testskjeletter vises som hoppet over.
Et grønt bygg bekrefter derfor ikke Kafka-publisering.

Lokal verifikasjon mens applikasjonen kjører:

```bash
curl --fail http://localhost:8080/isalive
curl --fail http://localhost:8080/isready
curl --fail http://localhost:8080/metrics
```

Readiness bekrefter bare at Ktor har startet. Ingen Kafka-produsent opprettes ved oppstart.

## Container

Dockerfile bruker Navs Chainguard JRE 21, låst til en digest hentet av utvikleren.
Registryet krever autentisering. Ikke del tokens eller lokale credential-filer.
Containeren kjører som non-root og kopierer bare biblioteker fra Gradle-distribusjonen.
Java startes direkte, siden runtime-imaget ikke trenger et shell.

```bash
mise run build
docker build -t innkrevingshendelser:local .
docker run --rm --name innkrevingshendelser-local -p 127.0.0.1:8080:8080 innkrevingshendelser:local
```

Bruk curl-kommandoene over for å sjekke containeren. Stopp med Ctrl-C,
eller `docker stop innkrevingshendelser-local` fra en annen terminal.
Base-imagets digest må oppdateres når en ny runtime-versjon tas i bruk.

## Prosjektstruktur

```text
mise.toml                         Java og utviklerkommandoer
gradle/libs.versions.toml          Kotlin- og bibliotekversjoner
src/main/kotlin/                  Ktor og Kafka-stubber
src/main/resources/logback.xml    JSON-logging til stdout
src/test/kotlin/                  Kotest og Kafka-testskjeletter
.nais/                           Manifestutkast for dev-gcp
.github/workflows/build.yaml     Bygg og tester, uten deploy
Dockerfile                       Runtime for ferdigbygd distribusjon
```

## Kafka-oppgaven

Rød sone: Utvikleren implementerer `KafkaPublisher` og fullfører testskjelettene.
Direkte `kafka-clients` er valgt fordi applikasjonen bare skal produsere meldinger.
Hendelseskontrakt, partisjonsnøkkel og leveringskrav for ekte hendelser er ikke bestemt.

1. Implementer produsentkonfigurasjon og eierskap. Nais leverer blant annet
   `KAFKA_BROKERS`, `KAFKA_KEYSTORE_PATH`, `KAFKA_TRUSTSTORE_PATH` og
   `KAFKA_CREDSTORE_PASSWORD`. Behold TLS-verifisering. Testbrokerens lokale
   tilkobling er ikke en mal for sikkerhetskonfigurasjonen i Nais.
2. Implementer sending med `acks=all`, produsent-idempotens og begrenset ventetid.
   En ack betyr at broker har bekreftet meldingen, ikke at en konsument har behandlet den.
   Timeout kan bety ukjent utfall. Produsent-idempotens gir ikke ende-til-ende
   deduplisering ved gjentatte kall eller restart.
3. Implementer lukking med begrenset ventetid. Ikke logg credentials, key eller payload,
   heller ikke gjennom exceptions eller Kafka-konfigurasjon.
4. Fullfør og aktiver roundtrip-testen med Testcontainers. Konsumenten finnes bare
   i testen og skal verifisere topic, key og value. Fullfør også feil-, timeout-
   og lukketestene. Kjør `mise run check` med Docker-motoren startet.

Fortell deretter hva koden gjør, hvorfor løsningen er valgt, og hvilke leveringsgarantier den gir.

## Drift og utrulling

Nais-manifestene er utkast for `dev-gcp`, namespace `utenlandsadresser`.
Topicets fulle navn blir `utenlandsadresser.innkrevingshendelser-v1`.
Bare applikasjonen har skrivetilgang. Ingen konsumenter har lesetilgang.
Én partisjon og 24 timers retention er startverdier for syntetiske dev-data.

CI kjører bygg, tester og `installDist` på PR-er og `main`. Den bygger eller
publiserer ikke container-images og deployer ikke ressurser.

Før første deploy må Kafka-oppgaven være fullført og containeren verifisert.
Vurder readiness på nytt når produsenten kobles inn. Ekte hendelser krever nye
avklaringer om personvern, kontrakt, tilgang og leveringskrav.

JVM- og HTTP-metrikker eksponeres på `/metrics`. Livssykluslogger går til stdout
som JSON. Kafka-tellere for bekreftede sendinger, feil og sendetid gjenstår.
Bruk aldri payload eller personopplysninger som metrikklabler.

Etter en senere dev-deploy: Sjekk health-endepunkter, Prometheus-scraping og
oppstartslogger. Verifiser publisering separat med syntetiske data og avklart
testtilgang. Ikke åpne lesetilgang automatisk for å gjøre testen.
Rollback ved senere oppdateringer er å gjeninnføre forrige fungerende image.
Ikke slett topicet som del av rollback. Ingen ressurser er opprettet av dette oppsettet.

## Code Style

### Minimal Editing

When fixing a bug or implementing a feature, change only what is necessary.
Do not rename variables, restructure working code, or refactor beyond the task at hand.
Keep diffs small and focused so they are easy to review.

## Git Workflow

Bruk feature-branch og PR. CI bygger og tester endringer, men deployer ikke.

## Boundaries

### ✅ Always

- Run tests after changes
- Follow existing code patterns in the project
- Preserve existing code structure — do not reorganize or refactor beyond the task
- Validate all external input

### ⚠️ Ask First

- Changing authentication mechanisms
- Adding new dependencies
- Modifying database schema

### 🚫 Never

- Commit secrets or credentials
- Skip input validation on external boundaries
