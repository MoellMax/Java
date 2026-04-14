package de.swp.service.impl;

import de.swp.dao.FotoDateiDAO;
import de.swp.entity.FotoDateiTO;
import de.swp.entity.impl.FotoDatei;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UC2 – Metadaten anzeigen
 *
 * Testfälle aus Testplan:
 * - UC2-TC-01: Datei auswählen -> Bild und Metadaten werden angezeigt
 * - UC2-TC-02: Zwischen Dateien wechseln -> korrekte Metadaten je Datei
 * - UC2-TC-03: Datei ohne EXIF -> UI bleibt stabil
 */
class ShowMetadataTest {

    private FotoService service;
    private FotoDateiDAO fotoDateiDAO;

    @BeforeEach
    void setUp() throws Exception {
        service = new FotoService();
        fotoDateiDAO = mock(FotoDateiDAO.class);

        // DAO per Reflection injizieren (wie bei UC1)
        var f = FotoService.class.getDeclaredField("fotoDateiDAO");
        f.setAccessible(true);
        f.set(service, fotoDateiDAO);
    }

    @Test
    void UC2_TC_01_dateiAuswaehlen_shouldReturnMetadataFromDao() {
        // Arrange
        String uniqueName = "testbild1.jpg";

        FotoDateiTO to = new FotoDateiTO();
        to.setUniqueName(uniqueName);
        to.setName("testbild1.jpg");
        to.setAufnahmeDatum(LocalDate.of(2024, 1, 1));

        when(fotoDateiDAO.findByUniqueName(uniqueName)).thenReturn(to);

        // Act
        FotoDateiTO result = service.getFotoDatei(uniqueName);

        // Assert
        assertNotNull(result, "getFotoDatei sollte ein TO zurückgeben");
        assertEquals(uniqueName, result.getUniqueName(), "uniqueName muss stimmen");
        assertEquals("testbild1.jpg", result.getName(), "Name muss stimmen");

        verify(fotoDateiDAO, times(1)).findByUniqueName(uniqueName);
    }

    @Test
    void UC2_TC_02_switchBetweenFiles_getAllImages_shouldContainCorrectEntries() throws Exception {
        // Arrange: Zwei verschiedene Dateien (simuliert „zwischen Dateien wechseln“)
        FotoDatei f1 = new FotoDatei();
        f1.setUniqueName("testbild1.jpg");
        f1.setName("Bild 1");

        FotoDatei f2 = new FotoDatei();
        f2.setUniqueName("testbild3.jpg");
        f2.setName("Bild 3");

        when(fotoDateiDAO.findPaginated(1, 10, "all"))
                .thenReturn(java.util.List.of(f1, f2));

        // Act
        JSONArray arr = service.getAllImages(1, 10);

        // Assert
        assertNotNull(arr);
        assertEquals(2, arr.length(), "Es müssen 2 Einträge zurückkommen");

        JSONObject o1 = arr.getJSONObject(0);
        JSONObject o2 = arr.getJSONObject(1);

        // Fallback-Logik aus FotoService:
        // displayName = name != null ? name : uniqueName
        assertEquals("testbild1.jpg", o1.getString("uniqueName"));
        assertEquals("Bild 1", o1.getString("displayName"));

        assertEquals("testbild3.jpg", o2.getString("uniqueName"));
        assertEquals("Bild 3", o2.getString("displayName"));

        verify(fotoDateiDAO, times(1)).findPaginated(1, 10, "all");
    }

    @Test
    void UC2_TC_03_fileWithoutExif_shouldNotCrashAndShouldFallbackDisplayName() throws Exception {
        // Arrange: Datei ohne EXIF / Anzeige-Name
        FotoDatei noExif = new FotoDatei();
        noExif.setUniqueName("ohne_exif.jpg");
        noExif.setName(null);

        when(fotoDateiDAO.findPaginated(1, 10, "all"))
                .thenReturn(java.util.List.of(noExif));

        // Act
        JSONArray arr = service.getAllImages(1, 10);

        // Assert
        assertNotNull(arr);
        assertEquals(1, arr.length());

        JSONObject o = arr.getJSONObject(0);
        assertEquals("ohne_exif.jpg", o.getString("uniqueName"));
        assertEquals(
                "ohne_exif.jpg",
                o.getString("displayName"),
                "Wenn name null ist, muss displayName auf uniqueName fallen"
        );

        verify(fotoDateiDAO, times(1)).findPaginated(1, 10, "all");
    }
}
