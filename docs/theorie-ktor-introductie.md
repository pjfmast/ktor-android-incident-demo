# Ktor Theorie- en Introductiegids: Best Practices & Patronen

Deze gids biedt een gestructureerde didactische opzet om **Ktor** (versie 3.x) te introduceren en te doceren aan de hand van de **Ktor & Android Incident Demo** (`ktor-android-incident-demo`). 

De handleiding is expliciet afgestemd op de aanbevolen patronen en *best practices* uit [Ktor Fundamentals](https://nomisrev.github.io/ktor-fundamentals) en de colleges over geavanceerde Ktor-beveiliging. Elk theoretisch concept is direct gekoppeld aan de daadwerkelijke broncode, configuratie en tests van dit project, inclusief hyperlinks naar de relevante slides.

---

## Inhoudsopgave

1. [Wat is Ktor? (Filosofie, Architectuur & "Code over Configuration")](#1-wat-is-ktor-filosofie-architectuur--code-over-configuration)
2. [Applicatiestructuur & Entry Point: Het `embeddedServer` Patroon](#2-applicatiestructuur--entry-point-het-embeddedserver-patroon)
3. [Het Plugin Systeem & De Request Pipeline](#3-het-plugin-systeem--de-request-pipeline)
   - [A. ContentNegotiation & Type-Veilige Serialization](#a-contentnegotiation--type-veilige-serialization)
   - [B. Gestructureerde Foutafhandeling met StatusPages & ApiError](#b-gestructureerde-foutafhandeling-met-statuspages--apierror)
   - [C. Observability & Request Logging via CallLogging](#c-observability--request-logging-via-calllogging)
   - [D. HTTP Standaarden & Mobile Caching via AutoHeadResponse](#d-http-standaarden--mobile-caching-via-autoheadresponse)
   - [E. Endpoint Bescherming via Rate Limiting](#e-endpoint-bescherming-via-rate-limiting)
4. [Routing DSL & Scheiding van Routing Boom en Handlers](#4-routing-dsl--scheiding-van-routing-boom-en-handlers)
5. [Beveiliging: Ktor 3.6+ Typed Authentication & Rolautorisatie](#5-beveiliging-ktor-36-typed-authentication--rolautorisatie)
   - [A. Direct Principal Model (`jwt<User>`)](#a-direct-principal-model-jwtuser)
   - [B. Declaratieve Rolautorisatie (`withRoles` & `authenticateWith`)](#b-declaratieve-rolautorisatie-withroles--authenticatewith)
   - [C. Set Semantiek (`containsAll`) & Hiërarchische Rollen (`Role.implied`)](#c-set-semantiek-containsall--hiërarchische-rollen-roleimplied)
   - [D. RBAC vs. Resource Ownership (ABAC in de Handler)](#d-rbac-vs-resource-ownership-abac-in-de-handler)
   - [E. Verse Database Lookups vs. Verouderde Token Claims](#e-verse-database-lookups-vs-verouderde-token-claims)
6. [Bestandsuploads: Streaming Multipart & Veilige Opslag](#6-bestandsuploads-streaming-multipart--veilige-opslag)
7. [Architectuur: Expliciete Dependency Injection Zonder Frameworks](#7-architectuur-expliciete-dependency-injection-zonder-frameworks)
8. [Testen in Ktor: `testApplication` & Test Best Practices](#8-testen-in-ktor-testapplication--test-best-practices)
9. [Interactieve Demo's & Oefeningen](#9-interactieve-demos--oefeningen)
10. [Overzicht: Best Practice Checklist](#10-overzicht-best-practice-checklist)

---

## 1. Wat is Ktor? (Filosofie, Architectuur & "Code over Configuration")

### Kernconcepten van Ktor
- **Ontwikkeld door JetBrains**: Ktor is een native Kotlin framework ontworpen om optimaal gebruik te maken van Kotlin taaleigenschappen (DSL's, coroutines, extension functions).
- **Asynchroon & Niet-blokkerend**: Gebouwd op Kotlin Coroutines. Iedere request draait in een coroutine op een niet-blokkerende I/O engine (zoals Netty).
- **Lichtgewicht & "Unopinionated"**:
  - In tegenstelling tot zware "batteries-included" frameworks (zoals Spring Boot) dicteert Ktor geen vaste structuur, dwingt het geen zware reflection of dependency-injection containers af, en kent het geen "magische" annotaties.
  - Je installeert en configureert uitsluitend wat je daadwerkelijk nodig hebt via *plugins*.
- **"Code over Configuration" **:
  - Expliciete, type-veilige Kotlin code duidelijker dan reflectie, XML of diepe HOCON-configuratie. Expliciete code is compile-time geverifieerd, direct navigeerbaar en eenvoudig te debuggen.
- **Server én Client in Multiplatform**:
  - Ktor biedt zowel een server-framework (`io.ktor.server.*`) als een multiplatform HTTP-client (`io.ktor.client.*`). In dit project zie je beide terug: de server draait de backend API, de unittests gebruiken de client met `testApplication`, en de toekomstige Android module (`android`) benut Ktor Client voor het consumeren van de API.

### Relevante Slides:
- [Ktor Fundamentals: What is Ktor? (Slides 1–25)](https://nomisrev.github.io/ktor-fundamentals/#/1)

### Verwijzingen in dit project:
- [`project.yaml`](../project.yaml): Definieert de multi-module architectuur (`server`, `shared`, `android`) via de Kotlin Toolchain (Amper).
- [`server/module.yaml`](../server/module.yaml): Minimale server build-definitie met Ktor 3.6 (`settings.ktor: enabled: true, version: 3.6.0`).
- [`libs.versions.toml`](../libs.versions.toml): Centrale versiecatalogus voor Kotlin, Ktor en Exposed.

---

## 2. Applicatiestructuur & Entry Point: Het `embeddedServer` Patroon

### `embeddedServer` vs. `EngineMain`
In Ktor bestaan traditioneel twee manieren om een server op te starten:
1. **`EngineMain`**: Start via reflectie op basis van HOCON (`application.conf`). Ktor zoekt via classloader-strings naar modulefuncties (bijv. `incident.server.ApplicationKt.module`).
2. **`embeddedServer`**: Start programmatisch en deterministisch direct vanuit Kotlin-code: `embeddedServer(Netty, port = ...) { ... }.start(wait = true)`.

### Waarom voorkeur voor `embeddedServer`:
(met name in combinatie met expliciete dependency passing):
- **Elimineert Reflectie & Magie**: Geen runtime classlookups of onduidelijke module-discovery fouten.
- **Deterministische Startup Volgorde**: Operationele opstarttaken (zoals database-migraties via `DatabaseFactory.init()` en data-seeding via `seedDemoData()`) worden sequentieel en synchroon uitgevoerd in `main()`, *vóórdat* de HTTP-engine bindt aan de netwerkpoort. Hierdoor kan er nooit een inkomend verzoek binnenkomen op een half-geïnitialiseerde database.
- **Volledig Type-Safe & Eenvoudig Debuggen**: Je kunt direct breakpoints zetten in `main()`, door de opbouw van afhankelijkheden stappen en instellingen inspecteren.

### Type-Safe Configuration (`AppConfig`)
In plaats van ad-hoc string-keys (`config.property("jwt.secret").getString()`) verspreid over de codebase, wordt configuratie eenmalig programmatisch ingelezen in een getypeerde data class:

```kotlin
@Serializable
data class AppConfig(
    val port: Int = 8080,
    val host: String = "0.0.0.0",
    val jwt: JwtConfig
)
```

In `server/resources/application.conf` staan uitsluitend de deployment parameters en JWT instellingen; de legacy `ktor.application.modules` sectie is volledig verwijderd.

### Relevante Slides:
- [Ktor Fundamentals: Server Setup & Configuration (Slides 26–48)](https://nomisrev.github.io/ktor-fundamentals/#/26)

### Verwijzingen in dit project:
- [`server/src/Application.kt` (main)](../server/src/Application.kt): Toont de programmatische bootstrap via `embeddedServer(Netty, port = appConfig.port, host = appConfig.host)` en de sequentiële aanroep van configuratie, database en dependencies.
- [`server/resources/application.conf`](../server/resources/application.conf): Schone HOCON configuratie zonder reflectieve module-verwijzingen.

---

## 3. Het Plugin Systeem & De Request Pipeline

### Wat is een Ktor Plugin?
Ktor is modulair opgebouwd rondom een **interceptor pipeline**. Wanneer een HTTP-verzoek binnenkomt, doorloopt het specifieke fasen (`Setup`, `Monitoring`, `Plugins`, `Routing`). Plugins haken in op deze fasen om functionaliteit toe te voegen en worden geïnstalleerd via `install(...)`.

### Relevante Slides:
- [Ktor Fundamentals: Application Pipeline & Plugins (Slides 67–85)](https://nomisrev.github.io/ktor-fundamentals/#/67)

---

### A. ContentNegotiation & Type-Veilige Serialization
- Zorgt voor automatische bidirectionele omzetting tussen HTTP-payloads en getypeerde Kotlin-objecten via `kotlinx.serialization`.
- In dit project delen client en server exact dezelfde DTO-definities (zoals `LoginRequest`, `TokenResponse`, `IncidentResponse`) via de KMP module `shared`.

```kotlin
install(ContentNegotiation) {
    json()
}
```

- **Relevante Slides**: [Ktor Fundamentals: Content Negotiation & Serialization (Slides 68–82)](https://nomisrev.github.io/ktor-fundamentals/#/68)
- **Verwijzing**: [`server/src/Application.kt`](../server/src/Application.kt).

---

### B. Gestructureerde Foutafhandeling met StatusPages & ApiError
Een veelgemaakte fout in Ktor-applicaties is het vangen van generieke exceptions met ad-hoc maps (`mapOf("error" to ...)`), of het terugsturen van plain text strings bij fouten.

#### Best Practice:
1. **Uniform Foutmodel**: Definieer een gedeelde `@Serializable data class ApiError(val message: String)` (geplaatst in `shared`, zodat zowel backend als Android app exact hetzelfde contract hanteren).
2. **Specifieke Exception Handlers**: Registreer getypeerde handlers (`exception<BadRequestException>`, `exception<NotFoundException>`) in plaats van één grote `when(cause)`.
3. **Beveiligde Fallback**: Vang onverwachte fouten (`exception<Throwable>`) af door ze intern te loggen (`call.application.log.error(...)`) en de client een veilige `500 Internal Server Error` te sturen zonder interne stacktraces te lekken.
4. **Status Code Handlers**: Vang automatische 401-, 403- en 404-antwoorden van Ktor op om ook daar altijd de uniforme `ApiError` JSON body mee te sturen.

```kotlin
install(StatusPages) {
    exception<BadRequestException> { call, cause ->
        call.respond(HttpStatusCode.BadRequest, ApiError(cause.message ?: "Invalid request."))
    }
    exception<NotFoundException> { call, cause ->
        call.respond(HttpStatusCode.NotFound, ApiError(cause.message ?: "Resource not found."))
    }
    exception<Throwable> { call, cause ->
        call.application.log.error("Unhandled error processing request", cause)
        call.respond(HttpStatusCode.InternalServerError, ApiError("An unexpected error occurred."))
    }
    status(HttpStatusCode.Unauthorized) { call, status ->
        call.respond(status, ApiError("Authentication is required to access this resource."))
    }
    status(HttpStatusCode.Forbidden) { call, status ->
        call.respond(status, ApiError("You do not have permission to access this resource."))
    }
}
```

- **Relevante Slides**: [Ktor Fundamentals: StatusPages & Error Handling (Slides 174–182)](https://nomisrev.github.io/ktor-fundamentals/#/174)
- **Verwijzingen in dit project**:
  - [`shared/src/core/ApiError.kt`](../shared/src/core/ApiError.kt): Gedeeld DTO foutmodel.
  - [`server/src/plugins/StatusPages.kt`](../server/src/plugins/StatusPages.kt): Centrale plugin configuratie.

---

### C. Observability & Request Logging via CallLogging
Zonder logging draait een backend server "blind" tijdens lokale ontwikkeling en runtime. 

```kotlin
install(CallLogging) {
    level = Level.INFO
}
```

- Geeft in het terminalvenster direct inzicht in elk inkomend verzoek: HTTP methode, URL-pad, statuscode en latentie (bijv. `200 OK: GET - /api/incidents in 12ms`).
- **Relevante Slides**: [Ktor Fundamentals: Call Logging & Monitoring (Slides 199–201)](https://nomisrev.github.io/ktor-fundamentals/#/199)
- **Verwijzing**: Geïnstalleerd in [`server/src/Application.kt`](../server/src/Application.kt).

---

### D. HTTP Standaarden & Mobile Caching via AutoHeadResponse
Conform RFC 9110 hoort elk endpoint dat `GET` ondersteunt ook `HEAD` te ondersteunen. 

```kotlin
install(AutoHeadResponse)
```

- **Voordeel voor de Android App**: Image caching libraries (zoals Coil) sturen regelmatig een `HEAD` verzoek om headers (`Content-Length`, `ETag`, `Last-Modified`) te inspecteren alvorens zware afbeeldingen over een mobiele 4G/5G-verbinding te downloaden. `AutoHeadResponse` genereert dit automatisch zonder dat je dubbele endpoints hoeft te schrijven.
- **Relevante Slides**: [Ktor Fundamentals: AutoHeadResponse Plugin (Slide 81)](https://nomisrev.github.io/ktor-fundamentals/#/81)
- **Verwijzing**: [`server/src/Application.kt`](../server/src/Application.kt).

---

### E. Endpoint Bescherming via Rate Limiting
Om brute-force aanvallen op gevoelige routes (zoals het inlog-endpoint) te voorkomen, biedt Ktor de `RateLimit` plugin:

```kotlin
install(RateLimit) {
    register(RateLimitName("login")) {
        rateLimiter(limit = 10, refillPeriod = 1.minutes)
    }
}
```

- Routes kunnen eenvoudig beschermd worden met `rateLimit(RateLimitName("login")) { ... }`.
- Geeft conform standaard HTTP-semantiek `429 Too Many Requests` terug zodra de limiet overschreden wordt.
- **Relevante Slides**: [Ktor Fundamentals: Rate Limiting (Slide 239)](https://nomisrev.github.io/ktor-fundamentals/#/239)
- **Verwijzingen**: [`server/src/Application.kt`](../server/src/Application.kt) en [`server/src/auth/AuthRoutes.kt`](../server/src/auth/AuthRoutes.kt).

---

## 4. Routing DSL & Scheiding van Routing Boom en Handlers

### De Routing Boom
Ktor gebruikt een declaratieve DSL (`routing { ... }`) om pad-hiërarchieën op te zetten.

### Pattern: Handlers Scheiden van de Routeboom
In veel Ktor-projecten worden routebomen onleesbaar omdat routing, parsing, validatie, authenticatie en database-aanroepen in één grote geneste anonieme lambda worden gepropt.

> *"Every handler has three phases: extract the parameters, process, respond. Handlers are extensions of `RoutingContext`."* ([Slides 93–95](https://nomisrev.github.io/ktor-fundamentals/#/93))

#### Goed Voorbeeld uit dit Project (`IncidentRoutes.kt`):
De routeboom blijft overzichtelijk en puur declaratief:

```kotlin
fun Route.incidentRoutes(incidentService: IncidentService, roleAuth: RoleAuthScheme) {
    authenticateWith(roleAuth, roles = setOf(AuthRole(Role.OFFICIAL))) {
        get { getAllIncidents(incidentService) }
        get("/paginated") { getIncidentsPaginated(incidentService) }
        patch("/{incidentId}/priority") { updateIncidentPriority(incidentService) }
        patch("/{incidentId}/status") { updateIncidentStatus(incidentService) }
    }
}
```

De feitelijke verwerking staat in compacte private `suspend fun RoutingContext.method(...)` functies:

```kotlin
private suspend fun RoutingContext.getAllIncidents(incidentService: IncidentService) {
    val incidents = incidentService.findAllIncidents()
    call.respond(HttpStatusCode.OK, incidents.map(Incident::toResponse))
}
```

### Relevante Slides:
- [Ktor Fundamentals: Routing DSL & Context Extensions (Slides 86–115)](https://nomisrev.github.io/ktor-fundamentals/#/86)

### Verwijzingen in dit project:
- [`server/src/incidents/IncidentRoutes.kt`](../server/src/incidents/IncidentRoutes.kt): Volledige scheiding tussen routeboom en handler-functies.
- [`server/src/users/UserRoutes.kt`](../server/src/users/UserRoutes.kt): Analoge opzet voor gebruikersbeheer.

---

## 5. Beveiliging: Ktor 3.6+ Typed Authentication & Rolautorisatie

### A. Direct Principal Model (`jwt<User>`)
In eerdere Ktor-versies was men verplicht een custom wrapper (`class UserPrincipal(val user: User) : Principal`) te definiëren. 

Ktor 3.6+ introduceert **Typed Authentication** (`jwt<T>`):
- De JWT-verificatie levert direct het domeinmodel `User` op:
  ```kotlin
  val authScheme: SimpleAuthenticationScheme<User> = jwt<User>("jwt-auth") {
      realm = jwtRealm
      verifier(jwtVerifier)
      validate { credential ->
          val id = credential.payload.getClaim("id").asLong()
          if (audienceMatches(credential) && id != null) userService.findById(id) else null
      }
  }
  ```
- In route handlers vraag je de ingelogde gebruiker rechtstreeks op zonder cast of unwrapping:
  ```kotlin
  val user: User = call.principal
  ```
- De verouderde `UserPrincipal.kt` wrapper is hierdoor compleet overbodig en uit het project verwijderd.

---

### B. Declaratieve Rolautorisatie (`withRoles` & `authenticateWith`)
In plaats van ad-hoc `if (user.role != ADMIN)` checks in handlers, wordt autorisatie declaratief afgedwongen op routeniveau:

```kotlin
val roleAuth: RoleAuthScheme = authScheme.withRoles(
    onForbidden = {
        call.respond(
            HttpStatusCode.Forbidden,
            ApiError("You do not have permission to access this resource.")
        )
    }
) { user ->
    user.role.implied.map { AuthRole(it) }.toSet()
}
```

Routes specificeren de minimale rolvereiste:
```kotlin
authenticateWith(roleAuth, roles = setOf(AuthRole(Role.ADMIN))) {
    get("/users") { getAllUsers(userService) }
}
```

---

### C. Set Semantiek (`containsAll`) & Hiërarchische Rollen (`Role.implied`)
Ktor's `authenticateWith(..., roles = setOf(...))` controleert of de rollen van de gebruiker voldoen aan **`containsAll(requiredRoles)`** ([Slide 238](https://nomisrev.github.io/ktor-fundamentals/#/238)). 
- Een valkuil: `setOf(Teacher, Admin)` vereist dat iemand *beide* rollen bezit.
- Oplossing: In ons model bevat `Role.implied` een set van alle rollen die geïmpliceerd worden:
  - `USER` &rarr; `{USER}`
  - `OFFICIAL` &rarr; `{USER, OFFICIAL}`
  - `ADMIN` &rarr; `{USER, OFFICIAL, ADMIN}`
- Doordat de role resolver `user.role.implied` teruggeeft, voldoet een `ADMIN` automatisch aan elke route die `AuthRole(Role.OFFICIAL)` eist!

---

### D. RBAC vs. Resource Ownership (ABAC in de Handler)
Zie het fundamentele didactische principe van autorisatie ([Slide 205](https://nomisrev.github.io/ktor-fundamentals/#/205) en [Slide 246](https://nomisrev.github.io/ktor-fundamentals/#/246)):
- **Rollen (RBAC)** bepalen *welk type gebruiker* belt (bijv. is het een Ambtenaar of een Burger?).
- **Ownership (ABAC)** bepaalt *wiens data* geraakt wordt (bijv. mag deze burger dit specifieke incident wijzigen?).
- Het uitsluitend vertrouwen op rolcontroles leidt tot BOLA/IDOR-kwetsbaarheden (Broken Object Level Authorization). In `IncidentRoutes.kt` wordt eigenaarschap daarom gecontroleerd via:
  ```kotlin
  val canModify = !foundIncident.isResolved
      && (isQualifiedOfficial() || foundIncident.isReportedByCurrentUser(userId))
  ```

---

### E. Verse Database Lookups vs. Verouderde Token Claims
- Zie ([Slide 237](https://nomisrev.github.io/ktor-fundamentals/#/237)) dat het uitlezen van rollen puur uit de JWT-payload weliswaar een databasequery bespaart, maar dat ingetrokken rollen pas effect hebben nadat het token verloopt (`exp`).
- In `JwtService.kt` valideert `userService.findById(id)` de gebruiker bij ieder inkomend verzoek. Hierdoor worden gedeactiveerde accounts of gewijzigde rollen direct effectief.

### Relevante Slides:
- [Ktor Fundamentals: Authentication & Authorization (Slides 204–247)](https://nomisrev.github.io/ktor-fundamentals/#/204)
- [Ktor Fundamentals: Roles & Authorization per Route (Slides 237–238)](https://nomisrev.github.io/ktor-fundamentals/#/237)

### Verwijzingen in dit project:
- [`server/src/auth/JwtService.kt`](../server/src/auth/JwtService.kt): Typed `jwt<User>` en `withRoles`.
- [`shared/src/users/Role.kt`](../shared/src/users/Role.kt): KMP-rolmodel met `implied`.
- [`server/src/utils/ApplicationCallUtil.kt`](../server/src/utils/ApplicationCallUtil.kt): Helpers voor `userId()` en `isQualifiedOfficial()`.

---

## 6. Bestandsuploads: Streaming Multipart & Veilige Opslag

[Slides 60–64 (*"A file arrives in parts"*)](https://nomisrev.github.io/ktor-fundamentals/#/60) laten zien hoe multipart bestanden asynchroon en veilig verwerkt moeten worden:

1. **Gestreamd Ontvangen via `call.receiveMultipart()`**: Bestanden worden in chunks binnengestroomd; ze worden nooit in hun geheel in het werkgeheugen geladen.
2. **Asynchrone Schijfstreaming via `writeChannel()`**:
   ```kotlin
   part.provider().copyAndClose(destinationFile.writeChannel())
   ```
3. **Gegarandeerde Resource Release via `try ... finally`**:
   Als een upload halverwege afbreekt door een netwerkfout of schijffout, moeten buffers en streams worden vrijgegeven:
   ```kotlin
   multipartData.forEachPart { part ->
       try {
           when (part) {
               is PartData.FileItem -> { /* stream naar bestand */ }
               else -> Unit
           }
       } finally {
           part.release() // Garandeert cleanup zonder memory-leaks!
       }
   }
   ```
4. **Veilige Bestandsnamen & Validatie**: In plaats van de door de client meegeleverde bestandsnaam te vertrouwen (kwetsbaar voor directory traversal), genereert de server een deterministische naam (`incident1-image1.png`) en controleert de extensie tegen een whitelist (`jpg`, `png`, `webp`).

### Relevante Slides:
- [Ktor Fundamentals: File Uploads & Multipart (Slides 60–64)](https://nomisrev.github.io/ktor-fundamentals/#/60)

### Verwijzingen in dit project:
- [`server/src/incidents/IncidentRoutes.kt` (uploadIncidentImages)](../server/src/incidents/IncidentRoutes.kt): Veilige streaming upload met `try ... finally { part.release() }`.

---

## 7. Architectuur: Expliciete Dependency Injection Zonder Frameworks

Gebruik niet onnodig zware DI-frameworks (zoals Spring, Dagger of Koin) te gebruiken:
- **Expliciete Parameter Passing**: Alle benodigde services worden samengebracht in een getypeerde container (`Dependencies`):
  ```kotlin
  data class Dependencies(
      val userService: UserService,
      val incidentService: IncidentService,
      val jwtService: JwtService,
      val roleAuth: RoleAuthScheme
  )
  ```
- **Schone Module Signatuur**:
  ```kotlin
  fun Application.module(dependencies: Dependencies) {
      // Configureer plugins en registreer routes met dependencies
  }
  ```
- **Duidelijke Scheiding van Verantwoordelijkheden**:
  - `main()` bouwt de infrastructuur op (configuratie, database, repositories, services, dependencies).
  - `Application.module` houdt zich uitsluitend bezig met HTTP routing en plugins.

### Relevante Slides:
- [Ktor Fundamentals: Architecture & Explicit Dependencies (Slides 28–35)](https://nomisrev.github.io/ktor-fundamentals/#/28)

### Verwijzingen in dit project:
- [`server/src/Dependencies.kt`](../server/src/Dependencies.kt): Container en factory functie.
- [`server/src/Application.kt`](../server/src/Application.kt): Assemblage in `main()` en injectie in `module(dependencies)`.

---

## 8. Testen in Ktor: `testApplication` & Test Best Practices

Ktor services testen kan via `io.ktor.server.testing.testApplication`. Enkele test-principes:

### 1. Pariteit tussen Productie en Testen
Doordat `Application.module(dependencies)` afhankelijkheden als parameter accepteert, kunnen tests exact dezelfde module aanroepen met test-doubles (`testDependencies()`):
```kotlin
testApplication {
    application { module(testDependencies()) }
    // ...
}
```
Hierdoor is er geen risico op "test drift" (waarbij tests per ongeluk andere plugins of configuraties gebruiken dan productie).

### 2. Type-Veilige DTO's in Tests
*"The handler sees objects, not bytes — and so should the test."* ([Slides 79–80 en 185–188](https://nomisrev.github.io/ktor-fundamentals/#/185)).
- **Slechte Praktijk**: JSON handmatig als strings bouwen (`setBody("{\"username\":\"...\"}")`) en controleren met `response.bodyAsText().contains(...)`.
- **Beter Patroon**: Configureer de testclient met `ContentNegotiation`:
  ```kotlin
  val client = createJsonClient()
  val response = client.post("/api/auth/login") {
      contentType(ContentType.Application.Json)
      setBody(LoginRequest("Henk", "pwd"))
  }
  val tokenResponse = response.body<TokenResponse>()
  ```

### 3. Ktor Client `bearerAuth()` Helper
In plaats van handmatige header concatenatie (`header("Authorization", "Bearer $token")`) gebruiken tests direct Ktor Client's idiomatische `bearerAuth(token)`.

### 4. Snelle In-Memory Isolatie
Dankzij `FakeUserRepository` en `FakeIncidentRepository` start elke testsuite met een geïsoleerde toestand in het geheugen; alle 28 tests voeren uit binnen enkele seconden.

### Relevante Slides:
- [Ktor Fundamentals: Testing Best Practices (Slides 185–192)](https://nomisrev.github.io/ktor-fundamentals/#/185)

### Verwijzingen in dit project:
- [`server/test/TestApplicationSetup.kt`](../server/test/TestApplicationSetup.kt): `testDependencies()` en `createJsonClient()`.
- [`server/test/TestUtils.kt`](../server/test/TestUtils.kt): `authenticate(role)` helper met `bearerAuth()`.
- [`server/test/AuthTest.kt`](../server/test/AuthTest.kt), [`server/test/IncidentsTest.kt`](../server/test/IncidentsTest.kt), [`server/test/UsersTest.kt`](../server/test/UsersTest.kt): Volledige suites gebouwd op type-veilige DTO's.

---

## 9. Interactieve Demo's & Oefeningen

De theorie kan direct in de les of tijdens zelfstudie gedemonstreerd worden:

### Demo 1: API Flows met de IntelliJ IDEA HTTP Client
Navigeer naar `server/test/http-requests/`:
- [`anonymous-flow.http`](../server/test/http-requests/anonymous-flow.http): Anoniem incident indienen.
- [`new-user-flow.http`](../server/test/http-requests/new-user-flow.http): Registratie, automatische tokenopslag en uploads.
- [`official-flow.http`](../server/test/http-requests/official-flow.http) & [`admin-flow.http`](../server/test/http-requests/admin-flow.http): Rolbeveiliging en statusmutaties.

### Demo 2: Geautomatiseerde Tests Uitvoeren
Laat zien hoe snel de integratietests draaien via de Kotlin Toolchain wrapper:
```bash
.\kotlin.bat test      # Windows
./kotlin test          # Linux / macOS
```

### Demo 3: Server Lokaal Starten
Start de server zonder externe build daemons:
```bash
.\kotlin.bat run -m server   # Windows
./kotlin run -m server       # Linux / macOS
```
Bezoek `http://localhost:8080/` in de browser en observeer de real-time logging via `CallLogging`.

---

## 10. Overzicht: Best Practice Checklist

| Thema | Oude / Naïeve Aanpak | Best practice Patroon | Waar in dit Project? |
| :--- | :--- | :--- | :--- |
| **Server Startup** | `EngineMain` met HOCON reflection | Programmatische `embeddedServer(Netty)` met `AppConfig` | `server/src/Application.kt` |
| **Dependency Wiring** | Zware DI (Spring/Koin) of service lookups | Expliciete `Dependencies` container parameter | `server/src/Dependencies.kt` |
| **Authenticatie** | `UserPrincipal` wrapper om domain model | Direct typed `jwt<User>` principal (`call.principal`) | `server/src/auth/JwtService.kt` |
| **Autorisatie** | Handmatige `if/else` checks in handlers | Declaratieve `authenticateWith` met `withRoles` | `server/src/incidents/IncidentRoutes.kt` |
| **Rolhiërarchie** | Complexe role trees of duplicate roles | `Role.implied` op basis van `containsAll` evaluatie | `shared/src/users/Role.kt` |
| **Resource Ownership** | Vergeten of vermengd met rolcontroles | Expliciete ABAC eigendomscontrole in de handler | `server/src/incidents/IncidentRoutes.kt` |
| **Foutafhandeling** | Ad-hoc `mapOf("error" to ...)` en strings | Centrale `StatusPages` met typed `@Serializable ApiError` | `server/src/plugins/StatusPages.kt` |
| **Observability** | Geen request logging | `install(CallLogging)` op INFO niveau | `server/src/Application.kt` |
| **HTTP Standaarden** | Alleen GET ondersteunen | `install(AutoHeadResponse)` voor mobile caching (HEAD) | `server/src/Application.kt` |
| **Brute-Force Protectie** | Ongelimiteerd inloggen | `install(RateLimit)` met HTTP 429 semantics | `server/src/Application.kt` |
| **Routing Architectuur** | Grote geneste anonieme route lambdas | Routeboom scheiden van handlers via `RoutingContext` | `server/src/incidents/IncidentRoutes.kt` |
| **File Uploads** | Byte-arrays in geheugen, losse releases | Streaming via `writeChannel()` en `try ... finally { part.release() }` | `server/src/incidents/IncidentRoutes.kt` |
| **API Testen** | Raw JSON strings en substring checks | Type-veilige DTO's (`setBody`, `body<T>()`) en `bearerAuth()` | `server/test/` |
| **Test Pariteit** | Aparte testmodule (`installTestModules`) | Exact dezelfde `Application.module(testDependencies())` | `server/test/TestApplicationSetup.kt` |
