package de.swp.service.impl;

import de.swp.dao.FotoDateiDAO;
import de.swp.entity.impl.FotoDatei;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UC5 – Suche
 *
 * Getestet wird FotoService.searchPhotosByMetadata(query, filter, page, pageSize)
 *
 * Erwartung:
 * - Delegiert an fotoDateiDAO.findByMetadatenJson(query, filter, page, pageSize)
 * - Ergebnis wird als JSONArray zurückgegeben
 * - JSON enthält pro Treffer: uniqueName + displayName
 * - displayName = name != null ? name : uniqueName (Fallback)
 */
class SearchTest {

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
    void UC5_TC_01_searchAll_shouldReturnJsonArrayWithUniqueNameAndDisplayName() {
        // Arrange
        String query = "Canon";
        String filter = "all";
        int page = 1;
        int pageSize = 10;

        FotoDatei a = new FotoDatei();
        a.setUniqueName("testbild1.jpg");
        a.setName("Bild Eins");

        FotoDatei b = new FotoDatei();
        b.setUniqueName("testbild2.png");
        b.setName("Bild Zwei");

        when(fotoDateiDAO.findByMetadatenJson(query, filter, page, pageSize))
                .thenReturn(List.of(a, b));

        // Act
        JSONArray result = service.searchPhotosByMetadata(query, filter, page, pageSize);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.length(), "Es müssen 2 Treffer zurückkommen");

        JSONObject o1 = result.getJSONObject(0);
        JSONObject o2 = result.getJSONObject(1);

        assertEquals("testbild1.jpg", o1.getString("uniqueName"));
        assertEquals("Bild Eins", o1.getString("displayName"));

        assertEquals("testbild2.png", o2.getString("uniqueName"));
        assertEquals("Bild Zwei", o2.getString("displayName"));

        verify(fotoDateiDAO, times(1)).findByMetadatenJson(query, filter, page, pageSize);
    }

    @Test
    void UC5_TC_02_searchWithGpsFilter_shouldUseFallbackDisplayNameWhenNameNull() {
        // Arrange
        String query = "GPS";
        String filter = "gps";
        int page = 2;
        int pageSize = 5;

        FotoDatei only = new FotoDatei();
        only.setUniqueName("ohne_name.jpg");
        only.setName(null); // -> displayName soll fallback auf uniqueName sein

        when(fotoDateiDAO.findByMetadatenJson(query, filter, page, pageSize))
                .thenReturn(List.of(only));

        // Act
        JSONArray result = service.searchPhotosByMetadata(query, filter, page, pageSize);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.length());

        JSONObject o = result.getJSONObject(0);
        assertEquals("ohne_name.jpg", o.getString("uniqueName"));
        assertEquals("ohne_name.jpg", o.getString("displayName"),
                "Wenn name null ist, muss displayName auf uniqueName fallen");

        verify(fotoDateiDAO, times(1)).findByMetadatenJson(query, filter, page, pageSize);
    }

    @Test
    void UC5_TC_03_searchNoResults_shouldReturnEmptyArray() {
        // Arrange
        String query = "does-not-exist";
        String filter = "no_gps";
        int page = 1;
        int pageSize = 10;

        when(fotoDateiDAO.findByMetadatenJson(query, filter, page, pageSize))
                .thenReturn(List.of());

        // Act
        JSONArray result = service.searchPhotosByMetadata(query, filter, page, pageSize);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.length(), "Bei keinen Treffern muss ein leeres JSONArray zurückkommen");

        verify(fotoDateiDAO, times(1)).findByMetadatenJson(query, filter, page, pageSize);
    }

    @Test
    void UC5_TC_04_generalSearch_shouldHandleLargerResultSet() {
        // Arrange: sehr allgemeiner Suchbegriff (viele Treffer)
        String query = "a";
        String filter = "all";
        int page = 1;
        int pageSize = 200;

        java.util.List<FotoDatei> many = new java.util.ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            FotoDatei f = new FotoDatei();
            f.setUniqueName("img_" + i + ".jpg");
            // absichtlich manchmal null, damit Fallback displayName getestet wird
            f.setName(i % 10 == 0 ? null : "Bild " + i);
            many.add(f);
        }

        when(fotoDateiDAO.findByMetadatenJson(query, filter, page, pageSize)).thenReturn(many);

        // Act
        JSONArray result = service.searchPhotosByMetadata(query, filter, page, pageSize);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.length(), "Es müssen 100 Treffer zurückkommen");
        // Spot-check: Fallback greift
        JSONObject tenth = result.getJSONObject(9);
        assertEquals("img_10.jpg", tenth.getString("uniqueName"));
        assertEquals("img_10.jpg", tenth.getString("displayName"), "Bei name==null muss displayName uniqueName sein");

        verify(fotoDateiDAO, times(1)).findByMetadatenJson(query, filter, page, pageSize);
    }

    @Test
    void UC5_TC_05_whenNoImagesExist_searchShouldReturnEmptyArrayNotNull() {
        // Arrange: keine Bilder vorhanden
        String query = "anything";
        String filter = "all";
        int page = 1;
        int pageSize = 10;

        when(fotoDateiDAO.findByMetadatenJson(query, filter, page, pageSize)).thenReturn(List.of());

        // Act
        JSONArray result = service.searchPhotosByMetadata(query, filter, page, pageSize);

        // Assert
        assertNotNull(result, "Auch ohne Bilder muss ein (leeres) JSONArray zurückkommen");
        assertEquals(0, result.length());
        verify(fotoDateiDAO, times(1)).findByMetadatenJson(query, filter, page, pageSize);
    }
}
