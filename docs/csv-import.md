# CSV-Import bestehender Termine

## Endpoint

```text
POST /service-providers/{providerId}/appointments/import
Content-Type: multipart/form-data
Formularfeld: file
```

Der `providerId` im Pfad bestimmt eindeutig, welchem Anbieter die CSV zugeordnet wird. Die CSV muss die drei Spalten `KdnNr`, `APP_DATE` und `SERVICE` in dieser Reihenfolge enthalten; Spaltennamen werden ohne Beachtung der Groß-/Kleinschreibung und äußeren Leerzeichen verglichen. `APP_DATE` ist ein ISO-8601-Zeitpunkt mit Zeitzone und muss im 15-Minuten-Raster liegen. `SERVICE` muss ein Leistungscode sein, den dieser Anbieter anbietet. UTF-8 mit oder ohne BOM wird akzeptiert. Leere Datenzeilen werden ignoriert; eine Datei ohne Datensätze ist ungültig.

Der Dateiinhalt wird anhand von CSV-Header und Datensätzen geprüft. Dateiendung und MIME-Typ werden nicht separat validiert.

## Ablauf

1. **Plausibilität:** Der Parser prüft alle Datensätze, ordnet Servicecodes den Angeboten des Anbieters zu und sammelt Zeilenfehler. Bei mindestens einem Fehler wird der gesamte Import vor jeder Kapazitätsprüfung und Buchung abgelehnt.
2. **Kapazitätsvorprüfung:** In einer read-only Transaktion werden Bestandsbelegungen gelesen. Die CSV-Zeilen werden der Reihe nach gegen den Bestand und gegen zuvor kapazitätsmäßig akzeptierte CSV-Zeilen geprüft. Alle dabei erkannten Konfliktzeilen werden gesammelt. Diese Vorprüfung sperrt den Anbieter nicht und speichert keine Termine.
3. **Transaktionale Buchung:** Nur wenn die Vorprüfung konfliktfrei ist, beginnt die Buchungstransaktion. Sie sperrt den Anbieter und bucht jeden Termin über die normale Buchungslogik, die die Kapazität erneut prüft. Scheitert eine Buchung an einem Konflikt, wird die Transaktion abgebrochen und alle Termine dieses Imports werden zurückgerollt.

Die Vorprüfung läuft vor der Sperre, damit Berechnung und Konfliktsammlung diese Sperre nicht unnötig lange halten. Eine parallele Buchung kann die Vorprüfung überholen; deshalb wird in der gesperrten Buchungsphase erneut geprüft. Ein dort entdeckter Konflikt bricht den gesamten Import ebenfalls ab.

## Antworten

- **201 Created:** Alle Termine wurden angelegt. Der Body ist eine Liste von Appointment-Objekten; `Location` verweist auf den ersten neu angelegten Termin.
- **400 Bad Request:** Datei oder Datensätze sind ungültig. Der Body enthält `errors` mit CSV-Zeilennummer und Fehlergrund für jeden erkannten Datensatzfehler. Es wurde nichts gebucht.
- **404 Not Found:** Der Anbieter existiert nicht.
- **409 Conflict:** Mindestens eine Zeile überschreitet die Kapazität. Der Body enthält `conflicts`; kein Termin dieses Imports bleibt gespeichert.

Ein Konflikteintrag enthält die CSV-Zeilennummer, Kundennummer, Servicecode, Startzeit und den Grund. `conflictsWithExistingAppointments` zeigt an, ob Bestandsdaten allein bereits einen Kapazitätskonflikt verursachen. `overlappingAcceptedCsvLineNumbers` enthält frühere CSV-Zeilen, die in der Vorprüfung kapazitätsmäßig akzeptiert wurden und zeitlich überlappen. Bereits abgelehnte CSV-Zeilen werden nicht in diese Liste aufgenommen; deshalb kann sie leer sein, obwohl ein zeitlicher Overlap mit einer solchen Zeile besteht.

## Wiederholte Importe

Es gibt derzeit keine Duplikaterkennung. Dieselbe CSV kann erneut Termine anlegen, wenn die Kapazität noch ausreicht. Ein erfolgreicher Import verändert daher dauerhaft die Belegung.

## Manuelles Testen

Vor dem Testen Anbieter-ID und Servicecodes prüfen:

```powershell
$baseUrl = "http://localhost:8080"
Invoke-RestMethod -Method Get -Uri "$baseUrl/service-providers"
Invoke-RestMethod -Method Get -Uri "$baseUrl/service-providers/1/services"
```

Mit PowerShell 7 oder neuer:

```powershell
$filePath = (Resolve-Path -LiteralPath ".\termine\IT-Service-Mueller.csv").Path
$headers = $null
$result = Invoke-RestMethod `
    -Method Post `
    -Uri "$baseUrl/service-providers/1/appointments/import" `
    -Form @{ file = Get-Item -LiteralPath $filePath } `
    -ResponseHeadersVariable headers
$result | ConvertTo-Json -Depth 6
$headers.Location
```

Unter Windows PowerShell 5.1 unterstützt `Invoke-RestMethod` den Parameter `-Form` nicht. Dort kann `curl.exe` verwendet werden:

```powershell
$filePath = (Resolve-Path -LiteralPath ".\termine\IT-Service-Mueller.csv").Path
curl.exe --include --form "file=@$filePath;type=text/csv" `
    "$baseUrl/service-providers/1/appointments/import"
```

Die Schneider-Datei wird entsprechend mit Provider-ID `2` hochgeladen. Für manuelle Tests empfiehlt sich eine separate Datenbank, etwa `test`; erfolgreiche Importe bleiben in der verwendeten Datenbank gespeichert.
