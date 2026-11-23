# innkrevingshendelser

Kotlin/Ktor-prosjekt for publisering av innkrevingshendelser til Kafka,
eid av team utenlandsadresser. Applikasjonen kjører health- og metrics-endepunkter.
`KafkaPublisher` sender meldinger til Kafka, men er ikke koblet inn i applikasjonen.
Ingen API-polling, publiserings-API, database eller Kafka-konsument inngår i applikasjonen.
Kun syntetiske testdata uten personopplysninger er tillatt.

## Bygg og kjør lokalt

Java-versjonen styres av `mise.toml`. Gradle-versjonen styres av wrapperen.
Les `mise.toml` før du kjører `mise trust`. Docker må kjøre for `mise run check`,
fordi Kafka-integrasjonstesten bruker Testcontainers.

```bash
mise trust
mise install
mise run check
mise run build
mise run run
```

`mise run test` kjører testene. `mise run build` kjører også `installDist`.
Rapporten ligger i `build/reports/tests/test/index.html`.
Kafka-integrasjonstesten sender en melding til en lokal broker og leser den tilbake.

Lokal verifikasjon mens applikasjonen kjører:

```bash
curl --fail http://localhost:8080/isalive
curl --fail http://localhost:8080/isready
curl --fail http://localhost:8080/metrics
```

Readiness bekrefter bare at Ktor har startet. Ingen Kafka-produsent opprettes ved oppstart.

## Container

Dockerfile bruker Navs Chainguard JRE 25, låst til en digest hentet av utvikleren.
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
src/main/kotlin/                  Ktor og Kafka-adapter
src/main/resources/logback.xml    JSON-logging til stdout
src/test/kotlin/                  Kotest-tester, testpakker speiler src/main
.nais/                           Manifestutkast for dev-gcp
.github/workflows/build.yaml     Bygg og tester, uten deploy
Dockerfile                       Runtime for ferdigbygd distribusjon
```

## Kafka-produsenten

Rød sone: Kafka-konfigurasjon, leveringsgarantier og lukking.
Direkte `kafka-clients` er valgt fordi applikasjonen bare produserer meldinger.

`KafkaPublisher` i `adapter.kafka` sender én melding med `publish`, venter på
brokerens bekreftelse og kansellerer sendingen hvis coroutinen kanselleres.
`close` venter høyst `closeTimeout` (standard 5 sekunder) på meldinger som ikke er sendt.
Produsentkonfigurasjonen sendes inn utenfra.

Regler for produsenten:

- Konfigurasjon i Nais leses fra `KAFKA_BROKERS`, `KAFKA_KEYSTORE_PATH`,
  `KAFKA_TRUSTSTORE_PATH` og `KAFKA_CREDSTORE_PASSWORD`. TLS-verifisering beholdes.
  Testbrokerens lokale tilkobling er ikke en mal for sikkerhetskonfigurasjonen.
- En ack betyr at brokeren har bekreftet meldingen, ikke at en konsument har behandlet den.
  Timeout kan bety ukjent utfall. Produsent-idempotens gir ikke ende-til-ende-deduplisering
  ved gjentatte kall eller restart.
- Logg aldri credentials, key eller payload, heller ikke gjennom exceptions eller
  Kafka-konfigurasjon.
- Konsumenter finnes bare i tester.

## Drift og utrulling

Nais-manifestene er utkast for `dev-gcp`, namespace `utenlandsadresser`.
Topicets fulle navn blir `utenlandsadresser.innkrevingshendelser-v1`.
Bare applikasjonen har skrivetilgang. Ingen konsumenter har lesetilgang.
Én partisjon og 24 timers retention er startverdier for syntetiske dev-data.

CI kjører bygg, tester og `installDist` på PR-er og `main`. Den bygger eller
publiserer ikke container-images og deployer ikke ressurser.

Ekte hendelser krever avklaringer om personvern, kontrakt, tilgang og leveringskrav.
Readiness må vurderes når produsenten kobles inn i applikasjonen.

JVM- og HTTP-metrikker eksponeres på `/metrics`. Livssykluslogger går til stdout
som JSON. Bruk aldri payload eller personopplysninger som metrikklabler.

Etter en dev-deploy: Sjekk health-endepunkter, Prometheus-scraping og
oppstartslogger. Verifiser publisering separat med syntetiske data og avklart
testtilgang. Ikke åpne lesetilgang automatisk for å gjøre testen.
Rollback er å gjeninnføre forrige fungerende image.
Ikke slett topicet som del av rollback.

## Utviklingsregler

Les `docs/utviklingsregler.md` før du endrer struktur eller legger til kode.
Reglene der utfyller Navs generelle instruksjoner og går foran ved konflikt om kodestruktur.

- Ports & Adapters: `adapter` → `application` → `domain`. Indre lag har ingen rammeverksimporter,
  unntatt `kotlinx.coroutines` og `arrow-core`. Metrikker telles i adaptere.
- Testdrevet utvikling: feilende test først, så minst mulig kode, så refaktorering.
  Kan agenten ikke kjøre testene, ber den utvikleren bekrefte rød test før implementasjon.
  Brukstilfeller testes gjennom porten med håndskrevne fakes, ikke mock-bibliotek.
  Tester skrives med Kotest `WordSpec`.
- Navngiving: domeneklasser og egenskapene deres har norske navn. Alt annet, også testnavn,
  skrives på engelsk. Æ, ø og å er tillatt i `domain` og `application`, ikke i `adapter` eller
  i noe som forlater applikasjonen.
- Dokumentasjon beskriver hvordan ting er. Ingen TODO-er, gjenstående oppgaver eller
  midlertidige tilstander i dokumentasjon eller skills. Det som gjenstår, hører hjemme i issues.
- Urelaterte funn under arbeid blir et GitHub-issue, ikke en del av PR-en. Unntak er det som
  trengs for at oppgaven skal virke, og sikkerhetsfeil som ikke kan vente. Agenten viser utkast
  til issuet og oppretter det etter godkjenning.

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
