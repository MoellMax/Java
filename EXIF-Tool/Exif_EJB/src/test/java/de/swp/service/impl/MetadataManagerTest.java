package de.swp.service.impl;

import de.swp.dao.FotoDateiDAO;
import de.swp.entity.FotoDateiTO;
import de.swp.entity.impl.FotoDatei;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UC4 – Metadaten bearbeiten (EXIF + DB Synchronisierung)
 *
 * Testfälle (praxisnah):
 * - UC4_TC_01: Bearbeitete Felder werden in DB-Entity übernommen + metadatenJson/metadaten Map synchronisiert
 * - UC4_TC_02: Wenn Bild nicht in DB gefunden wird (findByUniqueName == null), wird kein DAO.update ausgeführt
 * - UC4_TC_03: GPS + Geo-JSON werden korrekt in metadaten Map / metadatenJson übertragen
 * - UC4_TC_04: Ungültige Geo-Rohdaten führen nicht zum Crash (Fallback: String wird gespeichert)
 *
 * Nutzt Testbild: src/test/resources/images/testbild1.jpg
 */
class MetadataManagerTest {

    private FotoService service;
    private FotoDateiDAO fotoDateiDAO;

    @BeforeEach
    void setUp() throws Exception {
        service = new FotoService();
        fotoDateiDAO = mock(FotoDateiDAO.class);

        // DAO per Reflection injizieren (wie bei UC1/UC2/UC3)
        var f = FotoService.class.getDeclaredField("fotoDateiDAO");
        f.setAccessible(true);
        f.set(service, fotoDateiDAO);
    }

    private static byte[] loadResourceBytes(String classpathPath) throws Exception {
        try (InputStream is = MetadataManagerTest.class.getClassLoader().getResourceAsStream(classpathPath)) {
            assertNotNull(is, "Test-Resource nicht gefunden: " + classpathPath);
            return is.readAllBytes();
        }
    }

    private static FotoDateiTO createExistingDbTo(String uniqueName) {
        FotoDateiTO to = new FotoDateiTO();
        to.setId(1L);
        to.setUniqueName(uniqueName);
        to.setName("ALT_NAME.jpg");
        to.setTitel("Alt Titel");
        to.setBetreff("Alt Betreff");
        to.setAutoren("Alt Autor");
        to.setCopyright("Alt Copyright");
        to.setKameraHersteller("Alt Make");
        to.setKameraModell("Alt Model");
        to.setObjektivHersteller("Alt Lens Make");
        to.setObjektivModell("Alt Lens Model");
        to.setAufnahmeDatum(LocalDate.of(2020, 1, 1));
        to.setGeaendert_Am(LocalDateTime.of(2020, 1, 1, 0, 0));

        // jsonMetadaten: technische Daten (Basis) – kann leer oder JSON sein
        to.setJsonMetadaten("{\"technical\":{\"x\":1}}");

        // metadatenJson: wird in updateExif neu berechnet/überschrieben
        to.setMetadatenJson("{\"old\":\"value\"}");

        HashMap<String, String> map = new HashMap<>();
        map.put("ExistingKey", "ExistingValue");
        to.setMetadaten(map);

        return to;
    }

    @Test
    void UC4_TC_01_updateExif_shouldReturnUpdatedBytes_andUpdateDbFields() throws Exception {
        // Arrange
        byte[] originalJpeg = loadResourceBytes("images/testbild1.jpg");

        String uniqueName = "testbild1.jpg";

        FotoDatei input = new FotoDatei();
        input.setUniqueName(uniqueName);
        input.setName("NEUER_NAME.jpg");
        input.setTitel("Neuer Titel");
        input.setBetreff("Neuer Betreff");
        input.setAutoren("Neuer Autor");
        input.setCopyright("© 2026");
        input.setKameraHersteller("Canon");
        input.setKameraModell("R5");
        input.setObjektivHersteller("Sigma");
        input.setObjektivModell("35mm");
        input.setAufnahmeDatum(LocalDate.of(2024, 5, 20));
        input.setGeaendert_Am(LocalDateTime.of(2026, 1, 12, 9, 0));

        FotoDateiTO existing = createExistingDbTo(uniqueName);
        when(fotoDateiDAO.findByUniqueName(uniqueName)).thenReturn(existing);

        // Act
        byte[] updatedJpeg = service.updateExif(originalJpeg, input);

        // Assert: Bytes wurden erzeugt
        assertNotNull(updatedJpeg, "updateExif muss Bytes zurückgeben");
        assertTrue(updatedJpeg.length > 0, "updateExif muss nicht-leere Bytes zurückgeben");

        // In eurem Code wird update(...) zweimal aufgerufen
        ArgumentCaptor<FotoDatei> captor = ArgumentCaptor.forClass(FotoDatei.class);
        verify(fotoDateiDAO, times(2)).update(captor.capture());

        List<FotoDatei> updates = captor.getAllValues();
        FotoDatei last = updates.get(updates.size() - 1);

        // DB-Felder übernommen?
        assertEquals(uniqueName, last.getUniqueName(), "uniqueName muss erhalten bleiben");
        assertEquals("NEUER_NAME.jpg", last.getName());
        assertEquals("Neuer Titel", last.getTitel());
        assertEquals("Neuer Betreff", last.getBetreff());
        assertEquals("Neuer Autor", last.getAutoren());
        assertEquals("© 2026", last.getCopyright());
        assertEquals("Canon", last.getKameraHersteller());
        assertEquals("R5", last.getKameraModell());
        assertEquals("Sigma", last.getObjektivHersteller());
        assertEquals("35mm", last.getObjektivModell());
        assertEquals(LocalDate.of(2024, 5, 20), last.getAufnahmeDatum());
        assertEquals(LocalDateTime.of(2026, 1, 12, 9, 0), last.getGeaendert_Am());

        // metadatenJson wird in updateExif neu aufgebaut -> sollte die bearbeitbaren Felder enthalten
        assertNotNull(last.getMetadatenJson(), "metadatenJson sollte gesetzt sein");
        assertTrue(last.getMetadatenJson().contains("Image Description"), "metadatenJson sollte Titel enthalten");
        assertTrue(last.getMetadatenJson().contains("Artist"), "metadatenJson sollte Autoren enthalten");
        assertTrue(last.getMetadatenJson().contains("Make"), "metadatenJson sollte Hersteller enthalten");

        // metadaten Map wird synchronisiert (ExistingKey darf nicht verloren gehen)
        assertNotNull(last.getMetadaten(), "metadaten Map sollte gesetzt sein");
        assertEquals("ExistingValue", last.getMetadaten().get("ExistingKey"), "Bestehende Metadaten dürfen nicht verloren gehen");
        assertEquals("NEUER_NAME.jpg", last.getMetadaten().get("Name"));
        assertEquals("Neuer Titel", last.getMetadaten().get("Image Description"));
        assertEquals("Neuer Autor", last.getMetadaten().get("Artist"));

        verify(fotoDateiDAO, times(1)).findByUniqueName(uniqueName);
    }

    @Test
    void UC4_TC_02_updateExif_whenDbEntryMissing_shouldNotCallUpdate() throws Exception {
        // Arrange
        byte[] originalJpeg = loadResourceBytes("images/testbild1.jpg");

        FotoDatei input = new FotoDatei();
        input.setUniqueName("testbild1.jpg");
        input.setTitel("Neuer Titel");

        when(fotoDateiDAO.findByUniqueName("testbild1.jpg")).thenReturn(null);

        // Act
        byte[] updated = service.updateExif(originalJpeg, input);

        // Assert
        assertNotNull(updated);
        assertTrue(updated.length > 0);

        verify(fotoDateiDAO, times(1)).findByUniqueName("testbild1.jpg");
        verify(fotoDateiDAO, never()).update(any());
    }

    @Test
    void UC4_TC_03_updateExif_withGpsAndGeoJson_shouldSyncToMetadatenMapAndJson() throws Exception {
        // Arrange
        byte[] originalJpeg = loadResourceBytes("images/testbild1.jpg");

        String uniqueName = "testbild1.jpg";

        FotoDatei input = new FotoDatei();
        input.setUniqueName(uniqueName);
        input.setName("testbild1.jpg");
        input.setTitel("Titel mit GPS");
        input.setBreitengrad(52.2799);
        input.setLaengengrad(8.0472);

        // Geo-Rohdaten: wird in metadatenJson als "fotodateigeo" abgelegt
        input.setFotoDateiGeo("""
                {
                  "display_name": "Osnabrück, Niedersachsen, Deutschland",
                  "address": {
                    "city": "Osnabrück",
                    "state": "Niedersachsen",
                    "country": "Deutschland"
                  }
                }
                """);

        FotoDateiTO existing = createExistingDbTo(uniqueName);
        when(fotoDateiDAO.findByUniqueName(uniqueName)).thenReturn(existing);

        // Act
        byte[] updated = service.updateExif(originalJpeg, input);

        // Assert
        assertNotNull(updated);
        assertTrue(updated.length > 0);

        ArgumentCaptor<FotoDatei> captor = ArgumentCaptor.forClass(FotoDatei.class);
        verify(fotoDateiDAO, times(2)).update(captor.capture());
        FotoDatei last = captor.getAllValues().get(1);

        // GPS in Map
        assertEquals("8.0472", last.getMetadaten().get("GPS Longitude"));
        assertEquals("52.2799", last.getMetadaten().get("GPS Latitude"));

        // Address + Geo_* in Map
        assertEquals("Osnabrück, Niedersachsen, Deutschland", last.getMetadaten().get("Address"));
        assertEquals("Osnabrück", last.getMetadaten().get("Geo_city"));
        assertEquals("Niedersachsen", last.getMetadaten().get("Geo_state"));
        assertEquals("Deutschland", last.getMetadaten().get("Geo_country"));

        // Geo auch in metadatenJson (als fotodateigeo)
        assertNotNull(last.getMetadatenJson());
        assertTrue(last.getMetadatenJson().contains("fotodateigeo"), "metadatenJson sollte fotodateigeo enthalten");
        assertTrue(last.getMetadatenJson().contains("display_name"), "metadatenJson sollte display_name enthalten");
    }

    @Test
    void UC4_TC_04_updateExif_withInvalidGeoString_shouldNotCrash_andStoreFallback() throws Exception {
        // Arrange: Geo-String ist KEIN valides JSON -> Code soll in metadatenJson einen Fallback-String setzen
        byte[] originalJpeg = loadResourceBytes("images/testbild1.jpg");

        String uniqueName = "testbild1.jpg";

        FotoDatei input = new FotoDatei();
        input.setUniqueName(uniqueName);
        input.setName("testbild1.jpg");
        input.setTitel("Titel mit kaputtem Geo");
        input.setBreitengrad(52.2799);
        input.setLaengengrad(8.0472);

        String invalidGeo = "NOT_JSON_{display_name: Osnabrueck}";
        input.setFotoDateiGeo(invalidGeo);

        FotoDateiTO existing = createExistingDbTo(uniqueName);
        when(fotoDateiDAO.findByUniqueName(uniqueName)).thenReturn(existing);

        // Act
        byte[] updated = service.updateExif(originalJpeg, input);

        // Assert
        assertNotNull(updated);
        assertTrue(updated.length > 0);

        ArgumentCaptor<FotoDatei> captor = ArgumentCaptor.forClass(FotoDatei.class);
        verify(fotoDateiDAO, times(2)).update(captor.capture());
        FotoDatei last = captor.getAllValues().get(1);

        assertNotNull(last.getMetadatenJson());
        assertTrue(last.getMetadatenJson().contains("fotodateigeo"), "metadatenJson sollte fotodateigeo enthalten");
        assertTrue(last.getMetadatenJson().contains(invalidGeo), "Fallback-Geo-String muss gespeichert werden");

        // Die Map-Synchronisierung für Geo_* erfolgt nur, wenn gültiges JSON -> hier erwarten wir KEINE Geo_city usw.
        assertNotNull(last.getMetadaten());
        assertFalse(last.getMetadaten().containsKey("Geo_city"), "Bei invalidem Geo darf Geo_city nicht gesetzt werden");
    }
}
