package de.swp.service.impl;

import de.swp.dao.FotoDateiDAO;
import de.swp.entity.impl.FotoDatei;
import de.swp.util.FotoMetadatenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UC3 – Reverse Geocoding (GPS -> Adresse)
 *
 * Getestet wird FotoService.reverseGeocodeAsync(...):
 * - ruft FotoMetadatenUtil.reverseGeocode(...) auf
 * - lädt FotoDatei über DAO
 * - setzt fotoDateiGeo
 * - ergänzt metadatenJson um "geo"
 * - ergänzt metadaten Map um Address + Geo_* Felder
 * - ruft fotoDateiDAO.update(foto) auf
 *
 * Voraussetzung: Mockito Static Mocking -> dependency mockito-inline (test)
 */
class ReverseGeocodingTest {

    private FotoService service;
    private FotoDateiDAO fotoDateiDAO;

    @BeforeEach
    void setUp() throws Exception {
        service = new FotoService();
        fotoDateiDAO = mock(FotoDateiDAO.class);

        // DAO per Reflection injizieren
        var f = FotoService.class.getDeclaredField("fotoDateiDAO");
        f.setAccessible(true);
        f.set(service, fotoDateiDAO);
    }

    @Test
    void UC3_TC_01_withValidGps_shouldUpdateGeoFieldsAndPersist() {
        // Arrange
        long fotoId = 123L;
        double lat = 52.2799;
        double lon = 8.0472;

        FotoDatei foto = new FotoDatei();
        foto.setId(fotoId);

        // Ausgangs-Metadaten (wie im Code erwartet)
        foto.setMetadatenJson("{\"exif\":{}}");

        Map<String, String> metadaten = new HashMap<>();
        metadaten.put("ExistingKey", "ExistingValue");
        foto.setMetadaten(metadaten);

        when(fotoDateiDAO.findById(fotoId)).thenReturn(foto);

        String geoJson = """
                {
                  "display_name": "Osnabrück, Niedersachsen, Deutschland",
                  "address": {
                    "city": "Osnabrück",
                    "state": "Niedersachsen",
                    "country": "Deutschland"
                  }
                }
                """;

        try (MockedStatic<FotoMetadatenUtil> mocked = mockStatic(FotoMetadatenUtil.class)) {
            mocked.when(() -> FotoMetadatenUtil.reverseGeocode(lat, lon)).thenReturn(geoJson);

            // Act
            service.reverseGeocodeAsync(fotoId, lat, lon);

            // Assert
            ArgumentCaptor<FotoDatei> captor = ArgumentCaptor.forClass(FotoDatei.class);
            verify(fotoDateiDAO, times(1)).update(captor.capture());

            FotoDatei updated = captor.getValue();
            assertNotNull(updated);

            // 1) geo gespeichert
            assertEquals(geoJson, updated.getFotoDateiGeo(), "fotoDateiGeo muss gesetzt werden");

            // 2) metadatenJson enthält geo
            assertNotNull(updated.getMetadatenJson(), "metadatenJson muss existieren");
            assertTrue(updated.getMetadatenJson().contains("\"geo\""),
                    "metadatenJson muss 'geo' enthalten");

            // 3) metadaten Map ergänzt
            assertNotNull(updated.getMetadaten(), "metadaten Map muss existieren");
            assertEquals("ExistingValue", updated.getMetadaten().get("ExistingKey"),
                    "Bestehende Metadaten dürfen nicht verloren gehen");

            assertEquals("Osnabrück, Niedersachsen, Deutschland",
                    updated.getMetadaten().get("Address"),
                    "Address muss aus display_name übernommen werden");

            assertEquals("Osnabrück", updated.getMetadaten().get("Geo_city"));
            assertEquals("Niedersachsen", updated.getMetadaten().get("Geo_state"));
            assertEquals("Deutschland", updated.getMetadaten().get("Geo_country"));

            // Static call wurde wirklich gemacht
            mocked.verify(() -> FotoMetadatenUtil.reverseGeocode(lat, lon), times(1));
        }
    }

    @Test
    void UC3_TC_02_reverseGeocodeReturnsEmpty_shouldNotUpdateAnything() {
        // Arrange
        long fotoId = 1L;
        double lat = 1.0;
        double lon = 2.0;

        try (MockedStatic<FotoMetadatenUtil> mocked = mockStatic(FotoMetadatenUtil.class)) {
            mocked.when(() -> FotoMetadatenUtil.reverseGeocode(lat, lon)).thenReturn("");

            // Act
            service.reverseGeocodeAsync(fotoId, lat, lon);

            // Assert
            verify(fotoDateiDAO, never()).findById(anyLong());
            verify(fotoDateiDAO, never()).update(any());
            mocked.verify(() -> FotoMetadatenUtil.reverseGeocode(lat, lon), times(1));
        }
    }

    @Test
    void UC3_TC_02_nominatimNotReachable_shouldNotThrowAndNotUpdate() {
        // Arrange (Nominatim/HTTP nicht erreichbar -> Exception)
        long fotoId = 42L;
        double lat = 52.0;
        double lon = 8.0;

        try (MockedStatic<FotoMetadatenUtil> mocked = mockStatic(FotoMetadatenUtil.class)) {
            mocked.when(() -> FotoMetadatenUtil.reverseGeocode(lat, lon))
                    .thenThrow(new RuntimeException("Nominatim down"));

            // Act + Assert: reverseGeocodeAsync fängt Exceptions intern
            assertDoesNotThrow(() -> service.reverseGeocodeAsync(fotoId, lat, lon));

            // kein DAO Zugriff, weil reverseGeocode schon fehlgeschlagen ist
            verify(fotoDateiDAO, never()).findById(anyLong());
            verify(fotoDateiDAO, never()).update(any());
            mocked.verify(() -> FotoMetadatenUtil.reverseGeocode(lat, lon), times(1));
        }
    }

    @Test
    void UC3_TC_03_fotoNotFound_shouldNotUpdate() {
        // Arrange
        long fotoId = 999L;
        double lat = 52.0;
        double lon = 8.0;

        when(fotoDateiDAO.findById(fotoId)).thenReturn(null);

        String geoJson = """
                {"display_name":"X","address":{"city":"Y"}}
                """;

        try (MockedStatic<FotoMetadatenUtil> mocked = mockStatic(FotoMetadatenUtil.class)) {
            mocked.when(() -> FotoMetadatenUtil.reverseGeocode(lat, lon)).thenReturn(geoJson);

            // Act
            service.reverseGeocodeAsync(fotoId, lat, lon);

            // Assert
            verify(fotoDateiDAO, times(1)).findById(fotoId);
            verify(fotoDateiDAO, never()).update(any());
            mocked.verify(() -> FotoMetadatenUtil.reverseGeocode(lat, lon), times(1));
        }
    }

    @Test
    void UC3_TC_04_photoAlreadyHasGeo_shouldOverwriteGeoAndPersist() {
        // Arrange: Foto existiert bereits und hat bereits Geo-Daten gesetzt
        long fotoId = 777L;
        double lat = 52.2799;
        double lon = 8.0472;

        FotoDatei foto = new FotoDatei();
        foto.setId(fotoId);

        // Bereits vorhandenes Geo
        String oldGeoJson = """
                {
                  "display_name": "OLD PLACE",
                  "address": { "city": "OldCity", "state": "OldState", "country": "OldCountry" }
                }
                """;
        foto.setFotoDateiGeo(oldGeoJson);

        // vorhandene Metadaten
        foto.setMetadatenJson("{\"exif\":{},\"geo\":{\"display_name\":\"OLD\"}}");
        Map<String, String> metadaten = new HashMap<>();
        metadaten.put("ExistingKey", "ExistingValue");
        metadaten.put("Address", "OLD PLACE");
        metadaten.put("Geo_city", "OldCity");
        metadaten.put("Geo_state", "OldState");
        metadaten.put("Geo_country", "OldCountry");
        foto.setMetadaten(metadaten);

        when(fotoDateiDAO.findById(fotoId)).thenReturn(foto);

        // Neues Geo kommt rein
        String newGeoJson = """
                {
                  "display_name": "Osnabrück, Niedersachsen, Deutschland",
                  "address": {
                    "city": "Osnabrück",
                    "state": "Niedersachsen",
                    "country": "Deutschland"
                  }
                }
                """;

        try (MockedStatic<FotoMetadatenUtil> mocked = mockStatic(FotoMetadatenUtil.class)) {
            mocked.when(() -> FotoMetadatenUtil.reverseGeocode(lat, lon)).thenReturn(newGeoJson);

            // Act
            assertDoesNotThrow(() -> service.reverseGeocodeAsync(fotoId, lat, lon));

            // Assert: Update muss passieren und Geo überschrieben sein
            ArgumentCaptor<FotoDatei> captor = ArgumentCaptor.forClass(FotoDatei.class);
            verify(fotoDateiDAO, times(1)).update(captor.capture());

            FotoDatei updated = captor.getValue();
            assertNotNull(updated);

            assertEquals(newGeoJson, updated.getFotoDateiGeo(), "Geo muss überschrieben/aktualisiert werden");

            assertNotNull(updated.getMetadaten(), "metadaten Map muss existieren");
            assertEquals("ExistingValue", updated.getMetadaten().get("ExistingKey"),
                    "Bestehende Metadaten dürfen nicht verloren gehen");

            // Muss neue Adresse enthalten
            assertEquals("Osnabrück, Niedersachsen, Deutschland", updated.getMetadaten().get("Address"));
            assertEquals("Osnabrück", updated.getMetadaten().get("Geo_city"));
            assertEquals("Niedersachsen", updated.getMetadaten().get("Geo_state"));
            assertEquals("Deutschland", updated.getMetadaten().get("Geo_country"));

            // metadatenJson muss weiterhin geo enthalten (aktualisiert/neu gesetzt)
            assertNotNull(updated.getMetadatenJson());
            assertTrue(updated.getMetadatenJson().contains("\"geo\""),
                    "metadatenJson muss 'geo' enthalten (auch nach Überschreiben)");

            mocked.verify(() -> FotoMetadatenUtil.reverseGeocode(lat, lon), times(1));
        }
    }
}
