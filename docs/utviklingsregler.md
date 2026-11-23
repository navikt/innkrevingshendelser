# Utviklingsregler

Regler for struktur og arbeidsmåte i innkrevingshendelser. Reglene gjelder for
mennesker og KI-agenter, og de utfyller Navs generelle instruksjoner i
`.github/instructions/`. Ved konflikt om kodestruktur gjelder dette dokumentet.
Sikkerhetsreglene gjelder alltid.

Endringer i reglene gjøres med PR, slik som kode.

## 1. Ports & Adapters

Applikasjonen deles i tre lag. Avhengigheter peker alltid innover, mot domenet.

```text
adapter  →  application  →  domain
```

### Pakkestruktur

Rotpakke: `no.nav.utenlandsadresser.innkrevingshendelser`

| Pakke | Innhold | Kan avhenge av |
|---|---|---|
| `domain` | Verdiobjekter, entiteter, domeneregler og domenefeil | Kotlin stdlib, `kotlinx.coroutines`, `arrow-core` |
| `application` | Brukstilfeller og porter (`port.inn`, `port.ut`) | `domain`, og det `domain` kan avhenge av |
| `adapter.<teknologi>` | Adaptere for Kafka, HTTP, database, klienter og metrikker | `application`, `domain`, rammeverk |
| Rotpakken | Oppstart og kobling av adaptere til porter | Alt |

### Regler

- `domain` og `application` importerer aldri Ktor, Kafka, Jackson, Micrometer,
  database- eller loggerammeverk.
- En utgående port er et Kotlin-interface i `application.port.ut`, formulert i
  domenespråk. Porten tar imot en domenehendelse, ikke en `ProducerRecord`.
- En inngående port er brukstilfellets interface eller funksjon i
  `application.port.inn`. HTTP-ruter og konsumenter kaller bare inngående porter.
- Adaptere oversetter mellom teknologi og domene. Serialisering, topic-navn,
  headere og tekniske feiltyper holdes i adapteren.
- Porter returnerer `Either<Feil, Resultat>` fra Arrow, der `Feil` er et
  sealed-hierarki av domenefeil. Adaptere fanger tekniske unntak og oversetter
  dem til `Left`. Brukstilfeller fanger ikke `KafkaException` eller tilsvarende.
- Kobling skjer manuelt i oppstartskoden med konstruktørinjeksjon. Koin eller
  andre DI-rammeverk krever avklaring først, selv om Navs Kotlin-instruksjoner
  anbefaler Koin for større apper.
- Én adapter per ytre system. Delte hjelpere mellom adaptere legges ikke i
  `domain`.
- Lagreglene håndheves gjennom review. Verktøy som Konsist eller ArchUnit
  innføres først når manuelle brudd blir et problem.

### Metrikker

Navs Kotlin-instruksjoner ber om Prometheus-metrikker for forretningsoperasjoner.
Vi legger dem utenfor de indre lagene:

- **Tekniske metrikker** telles i adapteren som eier teknologien, for eksempel
  bekreftede sendinger, sendefeil og sendetid i Kafka-adapteren.
- **Forretningsmetrikker** telles i en dekoratør i `adapter.metrikker`. Dekoratøren
  implementerer samme inngående port som brukstilfellet, kaller det og teller
  kall, utfall og tid. Utfallet hentes fra `Either`: navnet på feiltypen ved
  `Left`, `ok` ved `Right`.
- Må et brukstilfelle melde fra om noe midt i flyten som ikke synes i resultatet,
  lages en utgående port med domenenavn for akkurat det. En generell port som
  `Metrikker.increment(navn)` er ikke lov.
- Label-verdier kommer bare fra sealed-typer eller faste strenger, aldri fra
  hendelsesdata. Personopplysninger, nøkler og payload brukes aldri som label.
- Dekoratøren kobles på i oppstartskoden. En test av `/metrics` bekrefter at
  metrikken finnes.

## 2. Testdrevet utvikling

Ny oppførsel skrives test først, i korte runder:

1. **Rød.** Skriv én test som beskriver ønsket oppførsel. Kjør den og se at den
   feiler av riktig grunn: en manglende funksjon eller feil verdi, ikke en
   kompileringsfeil i testen selv.
2. **Grønn.** Skriv minst mulig produksjonskode for å få testen grønn.
3. **Refaktorer.** Rydd i kode og test mens alle tester er grønne.

Den røde fasen skjer lokalt. Feilende tester committes ikke, i tråd med Navs
testinstruksjoner.

### Testnivåer per lag

| Lag | Testtype | Avhengigheter i testen |
|---|---|---|
| `domain` | Rene enhetstester | Ingen |
| `application` | Enhetstester av brukstilfeller gjennom den inngående porten | Håndskrevne fakes av utgående porter |
| `adapter` | Integrasjonstester av adapteren | Testcontainers, MockOAuth2Server, Ktor `testApplication` |
| Hele appen | Få ende-til-ende-tester | Ekte adaptere i testoppsett |

Mesteparten av testene skal ligge i `domain` og `application`. De er raske og
trenger ikke Docker.

### Regler

- Bruk håndskrevne fakes av egne porter, ikke mock-bibliotek. En fake er en enkel
  implementasjon i minnet, for eksempel en publisher som samler hendelsene i en
  liste. Mock-bibliotek kan brukes for tredjeparts-API-er der en fake blir
  urimelig dyr, men aldri for domeneklasser eller egne porter.
- Test oppførsel gjennom porten, ikke private funksjoner eller interne kall.
- Med `Either` testes både `Right` og hver relevant `Left`-variant.
- Én oppførsel per test, med et testnavn som beskriver oppførselen.
- En bugfiks starter med en test som gjenskaper feilen.
- Tester bruker bare syntetiske data uten personopplysninger.
- Hoppede tester (`xtest`, `enabled = false`) krever en kommentar med grunn og
  avklaring i PR-en.
- Dekningskravene i Navs testinstruksjoner gjelder. Dekning er et minimum, ikke
  målet. Målet er at hver oppførsel er drevet frem av en test.
- `mise run check` skal være grønn før PR.

### KI-agenter og TDD

- Agenten skriver testen før produksjonskoden, kjører den og viser at den feiler,
  før den skriver implementasjonen.
- For rød sone (domeneregler, sikkerhet og Kafka-leveringsgarantier) lager
  agenten bare testene og stubber, med mindre utvikleren ber om implementasjon.
- Agenten endrer aldri en eksisterende test for å få den grønn uten å si fra og
  begrunne det.
- Agenten oppgir hvilke tester som ble kjørt, og resultatet. Kan testene ikke
  kjøres, sier agenten det i stedet for å anta at de er grønne.

