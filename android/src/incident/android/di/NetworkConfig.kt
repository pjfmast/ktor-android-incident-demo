package incident.android.di

/**
 * Netwerkconfiguratie en documentatie voor backend-verbindingen.
 *
 * ### Emulator & IP-adressen (Verschil tussen 10.0.2.2 en 127.0.0.1):
 * - **Wat is localhost (`127.0.0.1`)?**
 *   Verwijst altijd naar het apparaat zélf. Op de host-PC is dit uw computer, maar binnen een Android-emulator
 *   is dit het virtuele Android-apparaat zelf (en dus niet de hostcomputer waar de Ktor-server op draait).
 *
 * - **Standaard emulator fallback (`10.0.2.2:8080`):**
 *   De Android-emulator (QEMU) draait in een afgeschermd virtueel netwerk met een virtuele router.
 *   Het IP-adres `10.0.2.2` is een door Google ingebouwde alias naar de loopback-interface van de host-PC.
 *   *Nadeel:* Deze virtuele routerlaag (NAT) introduceert vertragingen en kan leiden tot socket timeouts.
 *   Bovendien werkt `10.0.2.2` niet op fysieke testtoestellen die via USB zijn aangesloten.
 *
 * - **Aanbevolen tunnel-route (`127.0.0.1:8080` via `adb reverse`):**
 *   Met het commando:
 *   ```bash
 *   adb reverse tcp:8080 tcp:8080
 *   ```
 *   wordt een directe TCP-tunnel gelegd tussen poort 8080 op het Android-toestel en poort 8080 op de host-PC.
 *   *Voordelen:*
 *   1. Maximale betrouwbaarheid: Omzeilt de virtuele NAT-stack en voorkomt socket timeouts.
 *   2. Universeel: Werkt identiek op zowel Android-emulators als fysieke toestellen via USB.
 *   3. Uitvoeren in IntelliJ Terminal: Indien `adb` niet direct herkend wordt, gebruik
 *      `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8080 tcp:8080` of voeg
 *      `%LOCALAPPDATA%\Android\Sdk\platform-tools` toe aan het gebruikers-PATH.
 *
 * ### Relevantie voor Kotlin Multiplatform (KMP):
 * Is de tunnel-route ook aanbevolen voor KMP-projecten? **Ja, absoluut:**
 * - **Android (`androidMain`):** Gebruikt `http://127.0.0.1:8080` in combinatie met `adb reverse tcp:8080 tcp:8080`.
 * - **iOS Simulator (`iosMain`):** De iOS Simulator deelt de netwerkstack van macOS, waardoor `http://127.0.0.1:8080`
 *   daar standaard direct naar de host-PC verwijst (geen reverse proxy nodig).
 * - **Desktop / JVM (`jvmMain`):** Draait direct op het besturingssysteem en gebruikt eveneens `http://127.0.0.1:8080`.
 * - **Voordeel voor KMP:** Door op Android `adb reverse` in te zetten, kan in de gemeenschappelijke code (`commonMain`)
 *   eenduidig `http://127.0.0.1:8080` als basis-URL worden gebruikt voor álle platforms, wat platformspecifieke
 *   URL-overschrijvingen overbodig maakt.
 * - **Genymotion:** `http://10.0.3.2:8080`.
 * - **Fysiek toestel (via Wi-Fi/LAN):** `http://<HOST_IP>:8080`.
 *
 * ### Ktor HTTP Client Engines & Multiplatform Architectuur:
 * Om maximale stabiliteit, systeemprestaties en accuduur te garanderen en onnodige storingskosten te voorkomen,
 * adviseren JetBrains en Google voor Kotlin Multiplatform (KMP) projecten een **platform-native `expect`/`actual` architectuur**:
 *
 * 1. **Android (`androidMain`): `ktor-client-okhttp` (Aanbevolen)**
 *    - Biedt HTTP/2 multiplexing over één TCP-verbinding, volwassen connection pooling en naadloze TLS-afhandeling.
 *    - Integreert direct met de Android OS lifecycle (Doze mode, netwerkwisselingen van Wi-Fi naar mobiel netwerk).
 *
 * 2. **iOS (`iosMain`): `ktor-client-darwin` (Aanbevolen)**
 *    - Gebruikt Apple's native `NSURLSession`.
 *    - Essentieel voor iOS background network transfers, strikte battery management standaarden en native proxy-ondersteuning.
 *
 * 3. **Desktop / JVM / Server (`jvmMain`): `ktor-client-cio` of `ktor-client-apache5`**
 *    - Lichtgewicht coroutine-I/O met minimale runtime footprint en snelle opstarttijden.
 *
 * ### Voorbeeld `expect`/`actual` opzet in KMP:
 * ```kotlin
 * // commonMain
 * expect fun createHttpClient(): HttpClient
 *
 * // androidMain
 * actual fun createHttpClient(): HttpClient = HttpClient(OkHttp)
 *
 * // iosMain
 * actual fun createHttpClient(): HttpClient = HttpClient(Darwin)
 * ```
 *
 * ### Rol van CIO (Coroutine-based I/O):
 * - **Optimaal voor:** Ktor servers (FaaS/serverless, GraalVM native binaries), platformonafhankelijke SDK/library distributie en desktop tools.
 * - **Afgeraden voor mobiel (Android/iOS):** CIO is beperkt tot HTTP/1.x, mist diepe integratie met mobiele OS-mechanismen en leidt op mobiele emulators sneller tot socket- en connection timeouts over virtuele NAT-gateways.
 */
object NetworkConfig {
    const val BASE_URL = "http://127.0.0.1:8080"
}
