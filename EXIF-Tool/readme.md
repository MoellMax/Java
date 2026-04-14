
Exif Management Tool - Kunden-Dokumentation
Diese Dokumentation beschreibt die Funktionalität, Implementierung und Nutzung des Exif Management Tools.

1. Programmübersicht & Funktionalität - 
Das Exif Management Tool ist eine moderne Webanwendung zur Verwaltung, Analyse und Bearbeitung von Foto-Metadaten. Es ermöglicht Benutzern, große Mengen an Bildern hochzuladen, deren technische Metadaten (EXIF, IPTC, XMP, GPS) automatisch zu extrahieren und diese durchsuchbar zu machen.

** Kernfunktionen **
Bilder-Upload:
Unterstützt Drag-and-Drop und Mehrfachauswahl.

High-Speed Upload: 
Parallele Verarbeitung mehrerer Bilder für schnelle Upload-Zeiten.
Automatische Speicherung der Originaldateien und Extraktion aller Metadaten in die Datenbank.

Metadaten-Management:
Automatische Extraktion: Liest EXIF, IPTC, XMP und GPS-Daten beim Upload aus (z.B. Kameramodell, Blende, ISO, Aufnahmedatum).

Reverse Geocoding: 
Ermittelt automatisch die Adresse (Stadt, Straße, Land) basierend auf den GPS-Koordinaten der Fotos (sofern vorhanden).

Forward Geocoding: 
Ermöglicht die Umwandlung von Adressen in GPS-Koordinaten.

Service: 
Nutzt den OpenStreetMap Nominatim Dienst für hochpräzise Geo-Daten.

Bearbeitung: 
Ermöglicht das Ändern von Metadaten (z.B. Titel, Autor, Copyright) über die Benutzeroberfläche.

Write-Back:
Änderungen werden nicht nur in der Datenbank gespeichert, sondern auch verlustfrei direkt in die Bilddatei (EXIF-Header) zurückgeschrieben.

Inkrementelle Suche: 
Durchsucht live alle Metadaten-Felder (technische Daten, generierte Adressen, Namen).

GPS-Filter: 
Filtermöglichkeiten, um gezielt Bilder mit oder ohne GPS-Koordinaten anzuzeigen.

Galerie & Ansicht:
Responsive Rasteransicht mit Lazy Loading für hohe Performance auch bei vielen Bildern.
Detailansicht für jedes Bild mit vollständiger Auflistung aller rohen Metadaten-Tags.
Download-Funktion für Original- und bearbeitete Bilder.

2. Technische Implementierung & Genutzte Libraries - 
Das System basiert auf einer Client-Server-Architektur, die vollständig containerisiert mit Docker betrieben wird.

Frontend (Benutzeroberfläche)
Das Frontend ist als Single-Page-Application (SPA) konzipiert, die schnelle Reaktionszeiten und ein flüssiges Benutzererlebnis bietet.

Framework: SvelteKit (mit Vite als Build-Tool)
Sprache: TypeScript / JavaScript
Design: Reaktives CSS (Grid/Flexbox) für Desktop und Mobile.

Backend (Server & Logik)
Das Backend ist eine robuste Java Enterprise (Jakarta EE) Anwendung, die auf einem Payara Application Server läuft.

Server: Payara Micro / Server (Jakarta EE 10 Laufzeitumgebung)
Sprache: Java 17

Frameworks & APIs:
JAX-RS (Jersey): Bereitstellung der RESTful API Schnittstellen für das Frontend.
EJB (Enterprise JavaBeans): Kapselung der Geschäftslogik.
JPA (Jakarta Persistence) / Hibernate: Objekt-Relationales Mapping für Datenbankzugriffe.
Spezial-Libraries für Bildverarbeitung:
Apache Commons Imaging: Zum Lesen und Schreiben von Bildformaten und EXIF-Daten.
Metadata Extractor (com.drewnoakes): Leistungsstarke Bibliothek zum Auslesen tiefgehender Metadaten-Strukturen.
JSON (org.json, Gson): Verarbeitung von strukturierten Daten für API und Datenbank.
PostgreSQL JDBC Driver: Verbindung zur Datenbank.

Datenbank
PostgreSQL 16: Relationale Datenbank zur Speicherung der Bildinformationen und indizierten Metadaten (JSONB Format für flexible Metadaten-Suche).


3. Nutzung des Programms (Walkthrough) - 

Voraussetzung:

-> Apache Maven 3.9.9

-> Node.js

-> Java JDK 17.0.7

Schritt-für-Schritt Anleitung:

Öffnen und extrahieren Sie den Ordner EXIF_Komplett-main. 

Starten Sie Docker Desktop.

Gehen Sie in den entpackten Ordner EXIF_KOMPLETT -> projektdocker.

Öffnen Sie Ihre Eingabeaufforderung (alternativ *Rechtsklick -> in Terminal öffnen*) im Ordner projektdocker und schreiben Sie 'docker compose build'.

Sobald der Build-Prozess abgeschlossen ist, schreiben Sie in der Eingabeaufforderung 'docker compose up'.

Nun gehen Sie in den Ordner Exif_EJB und öffnen hier Ihre Eingabeaufforderung (alternativ *Rechtsklick -> in Terminal öffnen*) und geben ein 'mvn clean install'.

Nun gehen Sie in den Ordner Exif_Rest und öffnen hier Ihre Eingabeaufforderung (alternativ *Rechtsklick -> in Terminal öffnen*) und geben ein 'mvn clean install'.

Sobald Exif_EJB und Exif_Rest nach dem 'mvn clean install' einen target-Ordner erstellt haben, öffnen Sie einen Browser und geben ein: https://localhost:4848

User: admin Passwort: admin

In https://localhost:4848 ist nun eine Webserver Ansicht zu sehen. In der Liste auf der linken Seite befindet sich der Reiter 'Applications'.

Sie müssen auf 'Applications' klicken und unter 'Deployed Applications' auf 'Deploy...' klicken. 

Nun klicken Sie auf 'Browse...', suchen im Datei-Explorer nach Ihrem Ordner Exif_Rest -> target und wählen die testapp-1.0.war aus. Nun klicken Sie auf den Button 'OK'.

Öffnen der Anwendung:
Navigieren Sie in Ihrem Browser zu http://localhost:5173. Sie sehen die Hauptgalerie.

Bilder hochladen:
Klicken Sie auf "Datei auswählen" oder ziehen Sie Bilder in den Upload-Bereich.
Der Upload startet sofort. Eine Statusanzeige informiert Sie über den Prozess.
Nach Abschluss erscheinen die Bilder automatisch in der Galerie.

Bilder suchen und filtern:
Nutzen Sie die Radio-Buttons oben ("Ohne GPS", "Mit GPS", "Alle"), um die Ansicht einzuschränken.
Geben Sie einen Suchbegriff in das Suchfeld ein (z.B. "Canon" oder "Berlin"). Die Galerie aktualisiert sich sofort und zeigt nur passende Treffer.

Metadaten anzeigen & bearbeiten:
Klicken Sie auf den Button "Metadaten anzeigen" bei einem Bild.
Sie gelangen zur Detailansicht. Hier sehen Sie alle technischen Daten.
Über die "Bearbeiten"-Funktion (sofern im UI freigeschaltet) können Sie Werte ändern und speichern. Die Datei auf dem Server wird aktualisiert.

Download & Löschen:
Nutzen Sie "Download", um das Bild (inkl. aktualisierter Metadaten) herunterzuladen.
Nutzen Sie "Löschen", um ein Bild dauerhaft aus der Datenbank und vom Server zu entfernen.

Dokumentation erstellt am: 11.01.2026
Editiert am 14.01.2026
