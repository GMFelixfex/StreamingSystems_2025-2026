# Dokumentation Streaming Systems Praktikum

## Einleitung
Diese Dokumentation fasst die Ergebnisse der Aufgaben 2 bis 7 sowie Aufgabe 9 des Praktikums "Streaming Systems" (Semester 2025/26) zusammen. Aufgabe 8 wurde nicht bearbeitet. Die Lösungen untersuchen verschiedene Technologien zur Verarbeitung von Datenströmen (ActiveMQ, Kafka, Beam, Esper) und Architekturmuster (Messaging, Stream Processing, Event Sourcing, CRP). Der code wurde in Java und Python implementiert. 

Zu Finden auf Github: [Streaming Systems Praktikum 2025/26](https://github.com/GMFelixfex/StreamingSystems_2025-2026)

---

## Aufgabe 2: JMS Messaging mit ActiveMQ Artemis

### 1. Einleitung
In dieser Aufgabe wurde ein System zur Verarbeitung von LiDAR-Sensordaten mittels Java Message Service (JMS) implementiert. Ziel war es, eine Kette von Verarbeitungsschritten (Pipeline) aufzubauen, die rohe Scandaten einliest, verarbeitet und aggregierte Ergebnisse persistiert. Als Message Broker kam Apache ActiveMQ (Classic oder Artemis) zum Einsatz.

### 2. Lösungs- und Umsetzungsstrategie

Das System wurde in vier entkoppelte Komponenten zerlegt, die lose über JMS Topics kommunizieren (Publish-Subscribe-Pattern). Dies ermöglicht eine flexible Erweiterung und Skalierung der einzelnen Verarbeitungsschritte.

#### 2.1 Architektur & Komponenten

1.  **Produzent (`AMQ_LidarProducer`)**:
    *   Liest die Datei `Lidar-scans.txt` zeilenweise ein.
    *   Konvertiert JSON-ähnliche Textzeilen in `LidarPoint`-Objekte.
    *   Sendet diese als `ObjectMessage` an das Topic `Scans`.

2.  **Scan-Tagger (`AMQ_ScanTagger`)**:
    *   Konsumiert Nachrichten vom Topic `Scans`.
    *   Analysiert die Winkel der Messpunkte. Sobald der Winkel kleiner ist als der vorherige (Nulldurchgang), wird ein neuer Scan erkannt.
    *   Reichert das `LidarPoint`-Objekt mit einer `scanNr` an.
    *   Publiziert das aktualisierte Objekt in das Topic `TaggedScans`.

3.  **Distanz-Rechner (`AMQ_DistanceCalculator`)**:
    *   Konsumiert `TaggedScans`.
    *   Filtert Messpunkte mit geringer Qualität (Qualität < 15).
    *   Berechnet den euklidischen Abstand zwischen aufeinanderfolgenden Punkten innerhalb desselben Scans.
    *   Die Berechnung erfolgt mittels Umrechnung von Polarkoordinaten in kartesische Koordinaten.
    *   Sendet das Ergebnis (`scanNr`, `distanz`) als `MapMessage` an das Topic `Distanzen`. `MapMessage` wurde gewählt, da hier keine komplexe Objektstruktur mehr nötig ist.

4.  **Ergebnis-Aggregator (`AMQ_ResultAggregator`)**:
    *   Konsumiert Distanzwerte vom Topic `Distanzen`.
    *   Summiert die Distanzen pro Scan auf.
    *   Schreibt die finalen Ergebnisse (Gesamtdistanz pro Scan) in die Datei `results.txt`.

#### 2.2 Datenstrukturen

Für den Datenaustausch wurde eine dedizierte Klasse `LidarPoint` (im Paket `de.lidar.model`) entwickelt.
*   **Attribute**: `winkel` (double), `distanz` (double), `qualitaet` (int), `scanNr` (int).
*   **Serialisierung**: Die Klasse implementiert `Serializable`, um direkt als JMS `ObjectMessage` versendet werden zu können.

Im Aggregator wird eine `TreeMap<Integer, Double>` verwendet.
*   **Grund**: Wir wollen die Ergebnisse geordnet nach Scan-Nummer ausgeben. Eine `HashMap` würde die Reihenfolge nicht garantieren, während `TreeMap` die Keys (Scan-Nummern) natürlich sortiert.

### 3. Bewertung & Korrektheit

#### 3.1 Korrektheitsprüfung
**Frage:** Wie können Sie die Korrektheit Ihrer Lösung überprüfen?

Die Korrektheit der Implementierung wurde auf mehreren Ebenen validiert:
*   **Manueller Test:** Die ersten Zeilen der Eingabedatei wurden manuell nachgerechnet, um die Logik der Distanzberechnung (Polarkoordinaten -> euklidische Distanz) zu verifizieren.
*   **Plausibilitätscheck:** Die resultierenden Gesamtdistanzen (ca. 10.000 mm pro Scan) entsprechen dem Umfang des gescannten Raumes. Ein Scan mit drastisch abweichenden Werten würde auf Fehler hindeuten.
*   **Debugging:** Zwischenergebnisse (z.B. erkannte Scan-Wechsel) wurden auf der Konsole ausgegeben, um die `ScanTag`-Logik zu prüfen.
*   **Vergleich mit Aufgabe 3:** Die Ergebnisse wurden später mit der Kafka-Implementierung verglichen und waren identisch.

#### 3.2 Leistungsfähigkeit
Die Lösung verarbeitet die Daten in Nahezu-Echtzeit. ActiveMQ handhabt das Routing effizient.
*   **Flaschenhals:** Der `producer` liest aktuell aus einer Datei. In einem Produktionsszenario würde hier direkt der Sensorstream angebunden sein.
*   **Optimierung:** Die Verwendung von `ObjectMessage` ist bequem, aber in Bezug auf Performance und Interoperabilität (z.B. mit Nicht-Java-Clients) weniger optimal als Text/JSON-Nachrichten.

### 4. Reflexion der Rahmenbedingungen

*   **Tests:** Es wurden Unit-Tests für die mathematischen Hilfsfunktionen (`UtilFunctions`) vorgesehen, um die Berechnungslogik isoliert vom Messaging zu testen.
*   **KI-Einsatz:** Copilot wurde genutzt, um Boilerplate-Code für den JMS-Verbindungsaufbau (ConnectionFactory, Session, Producer/Consumer) zu generieren, da die JMS API sehr verbos ist. Die mathematische Logik wurde selbst implementiert.

### 5. Fazit
Die Aufgabe demonstriert erfolgreich die konzeptionelle Trennung von Verantwortlichkeiten (Separation of Concerns) durch Messaging. Jede Komponente erfüllt genau einen Zweck, was das System wartbar und robust macht.

---

## Aufgabe 3: Stream Processing mit Apache Kafka

### 1. Einleitung
Aufbauend auf Aufgabe 2 wurde die Verarbeitungspipeline auf Apache Kafka portiert. Ziel war es, die Unterschiede zwischen einem klassischen Message Broker (ActiveMQ/JMS) und einer Distributed Streaming Platform (Kafka) in der Praxis zu untersuchen. Zudem wurde eine Visualisierung der Ergebnisse mittels Python realisiert.

### 2. Lösungsstrategie (Kafka)

Im Gegensatz zur JMS-Lösung, die aus mehreren eigenständigen Java-Prozessen bestand, wurde die Kafka-Implementierung in einer einzigen Anwendung (`KafkaMain`) gebündelt, die jedoch logisch getrennte Schritte ausführt.

#### 2.1 Architektur & Implementierung (`KafkaMain`)

Die Anwendung fungiert sowohl als Producer als auch als Consumer.

1.  **Initialer Producer**:
    *   Liest die Datei `Lidar-scans.txt` und publiziert JSON-serialisierte `LidarPoint`-Objekte in das Topic `Scans`.
    *   **Unterschied zu JMS**: Statt Java Serialization (`ObjectMessage`) wird JSON (via Jackson `ObjectMapper`) und String-Serialisierung verwendet. Dies erhöht die Interoperabilität.

2.  **Streaming Consumer**:
    *   Ein Kafka Consumer abonniert **alle** relevanten Topics (`Scans`, `TaggedScans`, `Distanzen`).
    *   In einer Event-Loop werden Nachrichten basierend auf ihrem Ursprungs-Topic verarbeitet:
        *   **Input `Scans`**: Fügt Scan-Nummer hinzu -> `producer.send("TaggedScans")`.
        *   **Input `TaggedScans`**: Berechnet Distanz (Stateful Processing mit `lastPointByScan`) -> `producer.send("Distanzen")`.
        *   **Input `Distanzen`**: Aggregiert Ergebnisse in einer `TreeMap` -> Schreibt Datei.

#### 2.2 Spezifika der Kafka-Lösung
*   **Serialisierung**: JSON ist das De-facto-Format im Kafka-Ökosystem. Es ist menschenlesbar und sprachunabhängig.
*   **Status-Handling**: Die Berechnung von Distanzen erfordert den Zugriff auf den *vorherigen* Punkt (`lastPointByScan`). In unserer In-Memory-Lösung funktioniert dies, solange der Prozess läuft. In einem verteilten Produktionsszenario würde man hierfür Kafka Streams Stores oder eine externe Datenbank nutzen.

### 3. Vergleich: ActiveMQ vs. Kafka

| Merkmal | ActiveMQ (JMS) | Apache Kafka |
| :--- | :--- | :--- |
| **Modell** | Queue/Topic (Push-Modell) | Log-basiert (Pull-Modell) |
| **Persistenz** | Nachrichten werden nach Konsum standardmäßig gelöscht | Nachrichten bleiben für eine konfigurierbare Zeit (Retention) erhalten (Replay möglich) |
| **Durchsatz** | Gut für Transaktionen und komplexe Routing-Logik | Sehr hoch, optimiert für massives Streaming |
| **Implementierung** | Standardisierte API (JMS), aber viel Boilerplate | Client-Bibliothek, sehr flexibel, erfordert aber Konfiguration (Consumer Groups, Offsets) |

**Leistungsfähigkeit:**
Kafka zeigt seine Stärke bei hohen Datenraten. Während des Praktikums war bei der geringen Datenmenge kein signifikanter Performancevorteil messbar, jedoch ermöglichte das "Log"-Konzept von Kafka das einfache mehrfache Abspielen (Replay) der Daten zur Fehlersuche, ohne den Producer neu starten zu müssen (durch Reset der Consumer Offsets).

### 4. Visualisierung der Ergebnisse

Zur Überprüfung und Darstellung der berechneten Pfade wurde ein Python-Skript (`plotter.py`) entwickelt.

*   **Technologie**: Python mit der Bibliothek **Plotly**.
*   **Funktion**: Das Skript liest sowohl die Ergebnisdatei aus Aufgabe 2 (`results.txt` / JSON) als auch aus Aufgabe 3 (`resultsKafka.txt`).
*   **Output**: Eine interaktive HTML-Datei (`plot_results.html`).
*   **Ergebnis**: Der Plot zeigt, dass beide Implementierungen (ActiveMQ und Kafka) identische Distanzverläufe produzieren. Dies validiert die Korrektheit der Portierung.

### 5. Reflexion & Rahmenbedingungen

*   **Datenstrukturen**: Die Wiederverwendung der `LidarPoint` Klasse und der `UtilFunctions` (insb. Abstandsformel) aus Aufgabe 2 hat den Entwicklungsaufwand erheblich reduziert.
*   **Herausforderungen**: Die asynchrone Natur von Kafka erfordert ein Umdenken. Da wir alle Topics in einem Thread konsumieren, muss sichergestellt werden, dass die Verarbeitungskette nicht blockiert.
*   **Visualisierung**: Die Wahl von Plotly ermöglichte ein schnelles Zoom/Pan in den Daten, um Ausreißer in den Messungen (die es tatsächlich gab) zu identifizieren.
*   **Genutzte Frameworks**: 
    - **Apache Kafka (Clients 3.7.0):** Für das Streaming und Messaging.
    - **Jackson (Databind)**: Für die effektive JSON Serialisierung/Deserialisierung.
    - **Plotly (Python)**: Für die interaktive Visualisierung der Ergebnisse im Browser.

### 6. KI Einsatz
Copilot wurde genutzt, um die Kafka-Producer/Consumer-Logik zu skizzieren, insbesondere die Handhabung von Offsets und die Struktur der Event-Loop. Die eigentliche Logik der Datenverarbeitung (Scan-Tagging, Distanzberechnung) wurde eigenständig implementiert. Wir haben Testweise von der KI die komplette Aufgabe 2 in Aufgabe 3 umschreiben lassen, um die Unterschiede in der Code-Struktur zu sehen. Das Ergebnis war eine eigentlich relativ ähnliche Struktur, jedoch mit deutlich mehr In-Line Kommentaren und teilweise unnötigen Hilfsfunktionen, die die Lesbarkeit eher verschlechtert hätten. (Modell GPT 5-mini in Github Copilot in VSCode)

**Prompt für den Code:**
> I have an Artemis ActiveMQ application for an exercise, i want to change to Apache Kafka as the messaging service. I have a Docker container running with Apache Kafka (Port 9092). As for other Changes: I want to use a single Consumer application for everything instead of 3 seperate (ScanTagger, DistanceCalculator, ResultAggregator) but still use 3 queues/topics (Scans, TaggedScans, Distanzen). Could you create that in the kafka folder, dont change the other java files and use the utilFunctions/scantag if needed.

**Bewertung:**
1. Qualität des Codes: 9/10
2. Clean Code Prinzipien: 7/10 (hohe Indentierung, wenig Modularisierung)
3. Verständlichkeit: 8/10

---

## Aufgabe 4: Event Sourcing mit Apache Kafka

### 1. Einleitung und Aufgabenstellung
In dieser Aufgabe sollte ein verteiltes System zur Verfolgung von elektrischen Fahrzeugen im 2D-Raum entwickelt werden. Kern der Architektur ist das **Event Sourcing** Pattern, bei dem der Systemzustand nicht direkt gespeichert, sondern aus einer Historie von Ereignissen (Events) abgeleitet wird. Als Event Store kommt **Apache Kafka** zum Einsatz.

Das Ziel war eine iterative Entwicklung in vier Versionen, um die Architekturmuster **CQRS** (Command Query Responsibility Segregation) und Event Sourcing schrittweise zu vertiefen. Die Anforderungen umfassten Fahrzeugerstellung, Bewegung mittels Vektoren, komplexe Geschäftsregeln (Kollision, Zyklen) und alternative Lesemodelle (Redis).

### 2. Lösungs- und Umsetzungsstrategie

#### 2.1 Datenstrukturen
Zentrales Element ist die Position im 2D-Raum.
*   **Position**: Repräsentiert durch x- und y-Koordinaten.
    *   *Schnittstellen*: Implementiert `Serializable` (für Kafka/Dateien) und `Comparable`.
    *   *Diskussion Record vs. Class*:
        *   **Frage:** `Position` ist ein reines Datenobjekt. Was spricht für einen `record`?
            *   *Pro Record:* Records (seit Java 14) reduzieren Boilerplate-Code (automatische `equals`, `hashCode`, `toString`, Getter). Sie sind unveränderlich (immutable), was für Value Objects wie `Position` ideal ist.
        *   **Frage:** Was spricht dagegen?
            *   *Contra Record:* In älteren Java-Umgebungen (<14) nicht verfügbar. Records sind implizit `final` und können nicht von anderen Klassen erben (Vererbungseinschränkung). Manche Frameworks (z.B. ältere JPA-Provider oder Serializer) benötigen zwingend einen parameterlosen Konstruktor (`No-Args Constructor`) und Setters, was Records nicht bieten. Für dieses Projekt wäre ein `record` jedoch eine sehr passende Wahl gewesen.

#### 2.2 Architektur (CQRS)
Das System trennt strikt zwischen Schreib- (Write) und Lese-Seite (Read).
*   **Write Side (Command Handler):** Validiert Befehle (Commands) und erzeugt Events.
*   **Event Store (Kafka):** Persistiert alle Events in temporaler Reihenfolge.
*   **Read Side (Projection/Query):** Konsumiert Events und baut ein für Abfragen optimiertes Modell auf (z.B. eine Liste aller aktuellen Fahrzeugpositionen).

#### 2.3 Umsetzung der Versionen

*   **Version 1 (Hybrider Ansatz):** Validierung durch transientes In-Memory Set. Verlust der Konsistenz bei Neustart.
*   **Version 2 ("Pure" Event Sourcing):** Einführung von `VehicleAggregateLoader` und `VehicleAggregate.fromEvents(...)`. Zustand wird bei jedem Befehl aus Kafka-Historie neu aufgebaut (Replay).
*   **Version 3 (Komplexe Logik):** Implementierung von Geschäftsregeln wie "Max Moves" (5 Schritte) und Zyklenerkennung. Löschung von Fahrzeugen bei Kollision durch Abfrage der Read-Side (`Query` interface).
*   **Version 4 (Redis Projektion):** Externe Persistierung des Lesemodells in Redis (JSON-Format) für Datenbeständigkeit über Prozessneustarts hinaus.

### 3. Technische Details (Code-Level)

#### 3.1 Strategiediskussion: Event Replay vs. Snapshots

In Version 3 wird bei jedem Befehl der Zustand des Aggregats aus den Events neu aufgebaut.

*   **Strategie A: Dynamischer Aufbau (Event Replay)**
    *   Vorteil: Simpel, Single Source of Truth, keine Cache-Invalidierungsprobleme.
    *   Nachteil: Performance sinkt mit Event-Anzahl.
*   **Strategie B: Snapshots**
    *   Vorteil: Schnelles Laden.
    *   Nachteil: Hohe Komplexität.

**Bewertung:** Da Fahrzeuge in diesem Szenario eine sehr kurze Lebensdauer haben (max. 5-7 Events bis zur Zwangslöschung), ist **Event Replay** hier performanter und einfacher als Snapshots.

### 4. Leistungsfähigkeit & Nicht-funktionale Anforderungen

#### 4.1 Skalierbarkeit & Visualisierung
*   **Skalierbarkeit:** Kafka puffert Lastspitzen. Die Entkopplung von Read/Write erlaubt unabhängiges Skalieren der Projektionen. Redis ist für hohe Lese-Lasten prädestiniert.
*   **Visualisierungsmöglichkeiten:** Aktuell wird der Zustand als Log-Text bzw. über CLI-Abfragen gezeigt. Eine sinnvolle Erweiterung wäre ein **Real-Time 2D-Dashboard** (Web-UI), das per WebSocket von einer Kafka-Consumer-Backend (oder direkt aus Redis) gespeist wird und die Fahrzeuge als Punkte auf einer Karte bewegt. Kollisionen könnten visuell hervorgehoben werden.

#### 4.2 Latenz & Performance
Die Entscheidung für **Event Replay** ist bei kurzen Streams optimal. Redis (Version 4) eliminiert den Kaltstart-Aufwand des In-Memory Read Models.

### 5. Korrektheit und Tests
*   **Unit Tests (JUnit):** Logiktests für `VehicleCommandHandler` (Zyklen/Max Moves/Kollision).
*   **Integrationstests (`KafkaDemo`):** End-to-End Test, der prüft, ob Events korrekt serialisiert, via Kafka transportiert und im Read-Model ankommen.

### 6. KI-Einsatz & Frameworks
*   **KI:** Generierung von Boilerplate (Position, Redis-Grundgerüst), Strukturierung der Doku.
*   **Frameworks:** Apache Kafka Client, Jedis (Redis), JUnit, Log4j/SLF4J.

---

## Aufgabe 5: Verarbeitung kontinuierlicher Datenströme mit Apache Kafka und Apache Beam

### 1. Zielsetzung
Ziel war es, Geschwindigkeitsmesswerte von Sensoren aus einer Datei einzulesen, in ein Kafka-Topic zu publizieren und anschließend mit einer Apache Beam Pipeline zu verarbeiten. Die Pipeline sollte die Daten filtern, in 10-Sekunden-Fenster gruppieren und die Durchschnittsgeschwindigkeit pro Sensor berechnen.

### 2. Lösungs- und Umsetzungsstrategie

**Datenerzeugung (Producer):**
Die Klasse `KafkaFileProducer` liest `Trafficdata.txt`, parst Zeitstempel/SensorID/Speed und sendet Records an Kafka.
*   **Wichtig:** Der Zeitstempel wird explizit als Kafka Record Timestamp gesetzt (Event Time).

**Datenverarbeitung (Beam Pipeline):**
Implementierung in `KafkaBeamPipeline.java`:
1.  **KafkaIO Read:** Liest Topic `speeddata`. 
2.  **Event Time:** Extraktion des Timestamps (`c.outputWithTimestamp`) für korrektes Windowing.
3.  **Transforms:** 
    *   Filterung negativer Werte.
    *   Umrechnung m/s -> km/h.
4.  **Windowing:** `FixedWindows` (10s), mit `allowedLateness` für Robustheit.
5.  **Aggregation:** `GroupByKey` + Mittelwertberechnung.
6.  **Senke:** Schreiben in `data/averages.txt`.

### 3. Ergebnisse
Die Pipeline aggregiert korrekt nach Event-Time.
Beispiel Output:
```text
[05:22:40.000, 05:22:50.000] SensorId=5 AverageSpeed=28,08
...
```

### 4. Sliding Windows Erweiterung
**Frage:** Was müsste geändert werden, wenn ein Sliding Window der Länge 10 Sekunden mit einer Wiederholungsrate von 5 Sekunden genutzt werden soll?

**Lösung:**
Der Window-Transform in der Beam Pipeline muss angepasst werden:
```java
.apply("WindowIntoSliding", Window.<KV<String, Double>>into(
    SlidingWindows.of(Duration.standardSeconds(10))
                  .every(Duration.standardSeconds(5))
))
```
Dies führt zu überlappenden Fenstern, wodurch Messwerte in mehreren Durschnittsberechnungen einfließen.

---

## Aufgabe 6: Complex Event Processing (CEP) mit Esper

### 1. Zielsetzung
Anstatt einer Pipeline (Beam) sollte eine CEP-Engine (Esper) genutzt werden, um Muster direkt auf dem Datenstrom zu erkennen. Basisfall: Durchschnittsgeschwindigkeit (analog zu Aufgabe 5).

### 2. Umsetzung
Die Klasse `KafkaCepRunnerExtended` nutzt Esper.
*   **Input:** Konvertierung der Kafka-Records in Java-Objekte (`SpeedEvent`).
*   **Anfrage (EPL):**
    ```sql
    select sensorId, avg(speed) 
    from SpeedEvent.win:ext_timed_batch(...)
    ```
    Dies nutzt ein Time-Batch-Window (Tumbling Window) zur Berechnung.

---

## Aufgabe 7: CEP Erweiterung & Vergleich

### 1. Aufgabenstellung
Erkennung eines komplexen sequenziellen Musters: Ein Sensor meldet eine Geschwindigkeit, und *darauf folgend* meldet *derselbe* Sensor eine Geschwindigkeit mit einer Differenz von > 20 km/h (starker Anstieg/Abfall).

### 2. Lösung mit Esper (EPL)
Das Problem lässt sich deklarativ mit Espers Pattern-Matching Syntax lösen:
```sql
select ... from pattern [
    every a=AverageSpeedEvent ->
    b=AverageSpeedEvent(sensorId = a.sensorId and Math.abs(avgSpeedKmH - a.avgSpeedKmH) > 20)
]
```
*   `every`: Startet bei jedem Match neu.
*   `->`: Folgt zeitlich ("Followed-by").
*   `sensorId = a.sensorId`: Korrelation auf denselben Sensor.
*   `Math.abs(...) > 20`: Bedingung für die Geschwindigkeitsänderung.

### 3. Konzeptvergleich: Umsetzung in Apache Beam?

**Frage:** Wie müsste die Lösung mit Apache Beam von Aufgabe 5 erweitert werden, um diese Aufgabenstellung zu lösen?

**Antwort:**
Apache Beam ist primär ein Transformations-Framework, keine dedizierte CEP-Engine. Es gibt kein natives `followedBy` Konstrukt über Elemente hinweg wie in EPL.

**Lösungskonzept (Stateful Processing):**
Man müsste eine benutzerdefinierte `DoFn` mit **State** implementieren (`Stateful DoFn`).
1.  **State:** Ein `ValueState<Double>` speichert den *letzten* Durchschnittswert pro Sensor (Key).
2.  **ProcessElement:**
    *   Lese `lastValue` aus dem State.
    *   Wenn `lastValue` vorhanden ist: Berechne Differenz zum aktuellen Wert.
    *   Wenn Differenz > 20: Emittiere ein neues `ComplexEvent` (Alarm).
    *   Schreibe den aktuellen Wert als neuen `lastValue` in den State.
3.  **Partitionierung:** Dies erfordert zwingend eine `KeyedPCollection` (z.B. nach SensorId), damit der State lokal und konsistent ist.

### 4. Reflexion zu Aufgaben 5-7

*   **Leistungsfähigkeit:** Kafka skaliert durch Partitionierung. Beam (hier mit DirectRunner) kann in der Cloud (Dataflow, Flink) massiv parallelisiert werden.
*   **Teststrategie:** Manuelle Verifikation der Output-Files (`data/averages.txt`) und Abgleich mit bekannten Mustern in den Eingabedaten.
*   **KI-Einsatz:** Generierung von EPL-Syntax (oft fehleranfällig, musste korrigiert werden) und Beam Boilerplate (hilfreich, aber Gefahr von Processing-Time statt Event-Time Logik).
*   **Frameworks:** Apache Beam 2.46.0, Esper 7.1.0, Apache Kafka 3.7.0.

---

## Aufgabe 9: Technologievergleich und Bewertung

### 1. Einleitung
In dieser abschließenden Aufgabe werden die im Praktikum verwendeten Technologien – JMS (ActiveMQ), Apache Kafka, Apache Beam und CEP (Esper) – anhand definierter Kriterien verglichen. Ziel ist es, die spezifischen Stärken, Schwächen und Einsatzgebiete der jeweiligen Ansätze herauszuarbeiten.

### 2. Kriterienbasierter Vergleich

| Kriterium | JMS (ActiveMQ) | Apache Kafka | Apache Beam | CEP (Esper) |
| :--- | :--- | :--- | :--- | :--- |
| **1. Repräsentation** | Java Objektmodell (POJOs, Maps), sehr flexibel, unterstützt Vererbung. Header Properties. | Binärdaten (Byte Array). Schema optional (z.B. Avro, JSON). Keine native Vererbung im Broker. | PCollectionen von typisierten Objekten (Coder erforderlich). Stark typisiert. | POJOs oder Maps. Sehr flexibel, Java-Typisierung wird genutzt. Events als Objekte. |
| **2. Erzeugung & Übermittlung** | JMS API (`MessageProducer`). Einzelnachrichten. Transaktionsunterstützung. Push-Modell. | Producer API. Records werden gebatcht gesendet (hoher Durchsatz). Broker pollt/speichert nur (Dumb Broker). | Source IOs (z.B. KafkaIO). Pipeline zieht Daten (Pull) oder bekommt sie gepusht (Runner abhängig). | Adapter/Input Handler speisen Events in die Engine ein (API Aufruf `sendEvent`). |
| **3. Konsum** | `MessageConsumer`. Push (MessageListener) oder Pull (receive). Client-Acks. | Consumer Group Protokoll. Pull-Modell. Client verwaltet Offset. Parallelität durch Partitionen. | IO Source Transforms. Abstrahiert vom konkreten Source-System. Windowing bestimmt Verarbeitung. | Listener (`UpdateListener`) reagieren auf Pattern-Matches. Push-basiert. |
| **4. Speicherung** | Flüchtige Queues oder persistent (KahaDB/JDBC). Nachrichten werden nach Konsum gelöscht. | Durable Log. Retention Policy (Zeit/Größe). Events bleiben nach Konsum erhalten (Replay). | Keine eigene Speicherschicht. Nutzt externen State/Storage oder Runner-Internals. | In-Memory (Zustand der Windows/Patterns). Persistenz nur für Recovery optional. |
| **5. Garantien** | At-most-once, At-least-once, Exactly-once (in Transaktion). | At-least-once (Standard), Exactly-once (mit Idempotenz & Transaktionen). | Runner-abhängig. Beam Modell fordert Exactly-once Semantik von Runnern. | Engine-intern exakt. Input-Garantien extern abhängig. |
| **6. Sicherheit** | JAAS (User/Pass), TLS, ACLs auf Destinationen. | SASL/SSL, ACLs, Quotas. | Delegiert an Runner und Source/Sink Systeme. | Anwendungsspezifisch (In-Process Library). |
| **7. Verarbeitung** | Imperativ (Java Code im Consumer). Keine native Window/Time Unterstützung. | Kafka Streams API (DSL für Aggregationen, Windows, Joins, Event Time). | Deklaratives Pipeline-Modell. Sehr starkes Modell für Event Time, Watermarks, Windowing, Triggers. | EPL (SQL-ähnlich). Extrem mächtig für komplexe zeitliche Muster (Patterns), Time Windows. |
| **8. Skalierung** | Vertikal (Broker) limitiert. Clustering/Network of Brokers möglich, aber komplex. | Horizontal extrem gut skalierbar (Partitionierung Broker & Consumer). | Skaliert mit dem Runner (z.B. Flink Cluster, Google Dataflow). | Single-Knoten (in-memory) limitiert. Skalierung durch Partitionierung des Streams schwierig bei cross-event Mustern. |
| **9. Zuverlässigkeit** | Failover (Master/Slave). Persistenz store. | Replikation (Leader/Follower). Hohe Verfügbarkeit durch Cluster. | Checkpoints (Runner). Pipeline-Neustart bei Fehler. | High Availability Features (Enterprise Edition) oder externer Zustands-Restore. |
| **10. Typsicherheit** | Abhängig von Message Type (`ObjectMessage` vs `TextMessage`). Laufzeitfehler bei Casts. | Gering (Bytes). Schemas (Avro/Protobuf) via Schema Registry erzwingen Typen. | Hoch (Java Generics). Pipeline Graph wird zur Build-Zeit validiert. | Hoch bei POJOs. EPL wird zur Laufzeit validiert (Syntax/Schema). |
| **11. Sprachunterstützung** | Primär Java (JMS Standard). Andere via Protokolle (STOMP, AMQP). | Breiter Support (Java, C/C++, Python, Go, .NET) via Clients. | Java, Python, Go, SQL. | Java, .NET (Nesper). |

### 3. Detaillierte Bewertung

#### 3.1 JMS (ActiveMQ)
*   **Stärken:** Ausgereift, standardisiert in der Java-Welt (Jakarta EE), unterstützt komplexe Routing-Logik und Transaktionen exzellent. Einfache "Fire-and-Forget" oder RPC-Muster.
*   **Schwächen:** Skaliert schlecht horizontal ("Smart Broker, Dumb Consumer" wird zum Flaschenhals). Backpressure schwierig. Nachrichtenspeicherung nicht für Langzeit/Logs ausgelegt.
*   **Eignung:** Klassische Enterprise Integration, Auftragsverarbeitung, Entkoppelung von Microservices mit moderatem Durchsatz.

#### 3.2 Apache Kafka
*   **Stärken:** Extrem hoher Durchsatz, persistente Speicherung (Event Sourcing!), replay-fähig. De-facto Standard für moderne Streaming-Architekturen. Entkoppelt Produzenten und Konsumenten zeitlich vollständig.
*   **Schwächen:** Komplexes Setup (Zookeeper/KRaft). API etwas komplexer ("Dumb Broker, Smart Consumer"). Höhere Latenz als reine In-Memory Lösungen.
*   **Eignung:** Event Hubs, ETL Pipelines, Event Sourcing, High-Volume Data Ingestion, Log Aggregation.

#### 3.3 Apache Beam
*   **Stärken:** "The Unified Model". Einmal schreiben, überall laufen lassen (Flink, Spark, Dataflow). Bestes Programmiermodell für korrekte Event-Time Verarbeitung und Out-of-Order Daten (Watermarks, Triggers).
*   **Schwächen:** Abstraktionsschicht bringt Komplexität und Overhead. Debugging kann schwierig sein (Runner-spezifisch). Setup einer Entwicklungsumgebung manchmal sperrig.
*   **Eignung:** Komplexe Stream-Analytics, Batch- und Stream-Processing mit demselben Code, Cloud-native Data Pipelines.

#### 3.4 CEP / Esper
*   **Stärken:** Unschlagbar bei der Erkennung komplexer zeitlicher Zusammenhänge ("Event A, dann B innerhalb von 5s, aber nicht C"). EPL ist extrem ausdrucksstark und kompakt für solche Muster. Sehr geringe Latenz (In-Memory).
*   **Schwächen:** Integration in verteilte Systeme oft manuell (Embedden in Java App). Horizontal schwer zu skalieren, da State im RAM liegt. Syntax (EPL) lernbedürftig.
*   **Eignung:** Fraud Detection, Algorithmic Trading, IoT Monitoring (Schwellwerte/Muster), Echtzeit-Alerting.

### 4. Persönliches Fazit
Im Verlauf des Praktikums wurde deutlich, dass es keine "One Size Fits All" Lösung gibt.
*   Für einfache **Kommunikation** zwischen Services ist **JMS** (oder leichtere Alternativen wie RabbitMQ) oft ausreichend und einfacher zu handhaben als Kafka.
*   Wenn **Daten selbst das Produkt** sind (speichern, wiederholen, verteilen an viele Consumer), führt kein Weg an **Kafka** vorbei. Die Persistenzgarantie ändert die Architektur grundlegend (siehe Aufgabe 4 Event Sourcing).
*   Für die **algorithmische Auswertung** (Fenster, Zeitbezug) ist das Modell von **Beam** (bzw. Flink SQL / Kafka Streams) dem manuellen Coden von Windows weit überlegen.
*   **Esper** besetzt eine Nische: Wo SQL aufhört (Sequenzen, Muster), fängt EPL an. Es ist ein mächtiges Werkzeugkomponent, aber selten die alleinige Architektur.
*  Wir präferieren aber für Applikationen die wir hier umgesetzt haben oftmals eher MQTT als JMS, da es leichtergewichtig ist und besser für IoT/Edge-Szenarien passt. Speziell Beam war für uns eine Qual und hat uns nicht überzeugt, da es zu komplex und schwerfällig ist. Für die meisten Anwendungsfälle reicht eine Kombination aus Kafka (für die Datenhaltung) und einer einfachen Java-Consumer-Logik (für die Verarbeitung) völlig aus. 


### Disclaimer KI
Die Dokumentation wurde teilweise mit Unterstützung von KI (gemini 3.0, Copilot) erstellt. Stichpunkte zu text oder Erweiterungen von Wissensfragen wurden ebenfalls mithilfe von KI unterstützt. Alle Codebeispiele, Erklärungen und Vergleiche wurden von uns überprüft und angepasst, um die Korrektheit und Verständlichkeit sicherzustellen. KI wurde als Hilfsmittel genutzt, um die Dokumentation zu strukturieren und zu erweitern, nicht jedoch, um die Kernlogik der Implementierungen zu generieren. Endgültige formatierung sowie Grammatik und Rechtschreibung wurden von Gemini 3.0 Pro und Quillbot überprüft und korrigiert.


### Credits
Erstellt von Felix Pönitzsch und Alexander Weibert für das Praktikum "Streaming Systems" im Wintersemester 2025/26.