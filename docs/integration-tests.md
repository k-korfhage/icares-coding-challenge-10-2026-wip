# PostgreSQL-Integrationstest für parallele Buchungen

## Datenbank

Die Anwendung verwendet standardmäßig `dev`. Tests laden
`src/test/resources/application.properties` und verwenden standardmäßig die
separate Datenbank `test` derselben PostgreSQL-Instanz. `TEST_DB_NAME` kann den
Testdatenbanknamen überschreiben; `DB_HOST`, `DB_PORT`, `DB_USER` und
`DB_PASSWORD` gelten für beide Verbindungen.

Das Docker-Init-Skript `db/init/02-create-test-database.sql` erstellt `test`
und initialisiert es mit denselben Schema- und Seed-Dateien wie `dev`. Die
Init-Skripte werden nur bei einem leeren PostgreSQL-Datenverzeichnis ausgeführt.

## Ausführen

Stelle sicher, dass der PostgreSQL-Container läuft und `test` angelegt ist.
Führe anschließend im Projektverzeichnis aus:

```powershell
.\mvnw.cmd "-Dtest=AppointmentBookingConcurrencyTest" test
```

Der Test startet Spring Boot auf einem zufälligen HTTP-Port. Er erzeugt einen
Provider mit Kapazität eins und ein eigenes Service-Angebot. Zwei Worker
werden mit CountDownLatch gleichzeitig freigegeben und senden echte HTTP-POSTs
an den Appointment-Endpoint. Der Test erwartet genau eine Antwort `201 Created`,
eine Antwort `409 Conflict` und genau einen gespeicherten Termin. Provider- und
Service-IDs, Response-Statuscodes und -Bodies sowie die Anzahl gespeicherter
Termine werden geloggt. `@AfterEach` löscht die Testdatensätze nach dem Test.
Für sichtbare Testlogs Surefire mit `-Dsurefire.useFile=false` starten.

Die HTTP-Verbindung hat Connect- und Read-Timeouts. Testverbindungen zur DB
haben `lock_timeout` von fünf Sekunden und `statement_timeout` von 15 Sekunden.
Bei Fehlern werden noch laufende Futures abgebrochen, der Executor beendet und
maximal zwölf Sekunden auf dessen Threads gewartet.

Der Test verwendet echte Spring-Transaktionen, Spring Data JPA und PostgreSQLs
pessimistische Zeilensperre. Er setzt daher eine erreichbare Testdatenbank mit
aktuellem Projektschema voraus. Er verändert nicht die Datenbank `dev`.
