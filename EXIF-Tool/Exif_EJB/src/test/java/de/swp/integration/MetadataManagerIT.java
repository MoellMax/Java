package de.swp.integration;

import de.swp.dao.FotoDateiDAO;
import de.swp.entity.impl.FotoDatei;
import de.swp.service.impl.FotoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.json.JSONObject;
import org.junit.jupiter.api.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetadataManagerIT {

    private EntityManagerFactory emf;
    private EntityManager em;

    private FotoDateiDAO dao;
    private FotoService service;

    @BeforeAll
    void beforeAll() throws Exception {
        emf = Persistence.createEntityManagerFactory("SoftwarePrIT");
        em = emf.createEntityManager();

        dao = new FotoDateiDAO();
        injectEntityManagerIntoDao(dao, em);

        service = new FotoService();
        injectDaoIntoService(service, dao);
    }

    @BeforeEach
    void beforeEach() {
        rollbackIfActive();

        em.getTransaction().begin();
        em.createQuery("DELETE FROM FotoDatei").executeUpdate();
        em.getTransaction().commit();
    }

    @AfterEach
    void afterEach() {
        rollbackIfActive();
    }

    @AfterAll
    void afterAll() {
        rollbackIfActive();
        if (em != null) em.close();
        if (emf != null) emf.close();
    }

    @Test
    void UC4_IT_01_updateExif_shouldUpdateDbFields_andReturnNonEmptyBytes() throws Exception {
        // Arrange: Bildbytes + DB-Row
        byte[] original = loadResourceBytes("images/testbild1.jpg");
        String uniqueName = "uc4_edit.jpg";

        em.getTransaction().begin();
        FotoDatei f = new FotoDatei();
        f.setUniqueName(uniqueName);
        f.setName("ALT.jpg");
        f.setTitel("Alt Titel");
        f.setAutoren("Alt Autor");
        f.setAufnahmeDatum(LocalDate.of(2020, 1, 1));
        f.setGeaendert_Am(LocalDateTime.of(2020, 1, 1, 0, 0));

        // wichtig: jsonMetadaten ist "Basis" für updateExif
        f.setJsonMetadaten("{\"technical\":{\"x\":1}}");
        f.setMetadatenJson("{\"old\":\"value\"}");

        HashMap<String, String> map = new HashMap<>();
        map.put("ExistingKey", "ExistingValue");
        f.setMetadaten(map);

        em.persist(f);
        em.getTransaction().commit();

        // Daten, die "bearbeitet" werden sollen
        FotoDatei data = new FotoDatei();
        data.setUniqueName(uniqueName);
        data.setName("NEU.jpg");
        data.setTitel("Neuer Titel");
        data.setAutoren("Neuer Autor");
        data.setKameraHersteller("Canon");
        data.setKameraModell("R5");
        data.setAufnahmeDatum(LocalDate.of(2024, 5, 20));
        data.setGeaendert_Am(LocalDateTime.of(2026, 1, 12, 10, 0));

        // Act
        em.getTransaction().begin();
        byte[] updated = service.updateExif(original, data);
        em.getTransaction().commit();

        // Assert: Bytes
        assertNotNull(updated);
        assertTrue(updated.length > 0, "updateExif muss nicht-leere Bytes liefern");

        // Assert: DB wurde aktualisiert
        FotoDatei persisted = em.createQuery(
                        "SELECT f FROM FotoDatei f WHERE f.uniqueName = :u",
                        FotoDatei.class)
                .setParameter("u", uniqueName)
                .getSingleResult();

        assertEquals("NEU.jpg", persisted.getName());
        assertEquals("Neuer Titel", persisted.getTitel());
        assertEquals("Neuer Autor", persisted.getAutoren());
        assertEquals("Canon", persisted.getKameraHersteller());
        assertEquals("R5", persisted.getKameraModell());
        assertEquals(LocalDate.of(2024, 5, 20), persisted.getAufnahmeDatum());

        assertNotNull(persisted.getMetadatenJson(), "metadatenJson muss gesetzt sein");
        assertTrue(persisted.getMetadatenJson().contains("Image Description"), "metadatenJson sollte Titel enthalten");
        assertTrue(persisted.getMetadatenJson().contains("Make"), "metadatenJson sollte Make enthalten");

        assertNotNull(persisted.getMetadaten(), "metadaten Map muss gesetzt sein");
        assertEquals("ExistingValue", persisted.getMetadaten().get("ExistingKey"), "alte Metadaten dürfen nicht verloren gehen");
        assertEquals("NEU.jpg", persisted.getMetadaten().get("Name"));
        assertEquals("Neuer Titel", persisted.getMetadaten().get("Image Description"));
        assertEquals("Neuer Autor", persisted.getMetadaten().get("Artist"));
    }

    @Test
    void UC4_IT_03_updateExif_withGpsAndGeo_shouldSyncAddressAndGeoFields() throws Exception {
        // Arrange
        byte[] original = loadResourceBytes("images/testbild1.jpg");
        String uniqueName = "uc4_gps.jpg";

        em.getTransaction().begin();
        FotoDatei f = new FotoDatei();
        f.setUniqueName(uniqueName);
        f.setName("GPS.jpg");
        f.setJsonMetadaten("{\"technical\":{\"x\":1}}");
        f.setMetadatenJson("{\"old\":\"value\"}");
        f.setGeaendert_Am(LocalDateTime.now());
        f.setMetadaten(new HashMap<>());
        em.persist(f);
        em.getTransaction().commit();

        FotoDatei data = new FotoDatei();
        data.setUniqueName(uniqueName);
        data.setName("GPS.jpg");
        data.setTitel("Titel mit GPS");
        data.setBreitengrad(52.2799);
        data.setLaengengrad(8.0472);
        data.setFotoDateiGeo("""
                {
                  "display_name": "Osnabrück, Niedersachsen, Deutschland",
                  "address": {
                    "city": "Osnabrück",
                    "state": "Niedersachsen",
                    "country": "Deutschland"
                  }
                }
                """);

        // Act
        em.getTransaction().begin();
        byte[] updated = service.updateExif(original, data);
        em.getTransaction().commit();

        // Assert
        assertNotNull(updated);
        assertTrue(updated.length > 0);

        FotoDatei persisted = em.createQuery(
                        "SELECT f FROM FotoDatei f WHERE f.uniqueName = :u",
                        FotoDatei.class)
                .setParameter("u", uniqueName)
                .getSingleResult();

        assertNotNull(persisted.getMetadaten(), "metadaten Map muss existieren");
        assertEquals("8.0472", persisted.getMetadaten().get("GPS Longitude"));
        assertEquals("52.2799", persisted.getMetadaten().get("GPS Latitude"));

        assertEquals("Osnabrück, Niedersachsen, Deutschland", persisted.getMetadaten().get("Address"));
        assertEquals("Osnabrück", persisted.getMetadaten().get("Geo_city"));
        assertEquals("Niedersachsen", persisted.getMetadaten().get("Geo_state"));
        assertEquals("Deutschland", persisted.getMetadaten().get("Geo_country"));

        assertNotNull(persisted.getMetadatenJson());
        assertTrue(persisted.getMetadatenJson().contains("fotodateigeo"), "metadatenJson muss fotodateigeo enthalten");

        // Optionaler Validitätscheck: fotodateigeo als JSON-Objekt vorhanden
        JSONObject meta = new JSONObject(persisted.getMetadatenJson());
        assertTrue(meta.has("fotodateigeo"));
    }

    // ----------------- Helpers -----------------

    private void rollbackIfActive() {
        try {
            if (em != null && em.getTransaction() != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        } catch (Exception ignored) {
        }
    }

    private static byte[] loadResourceBytes(String classpathPath) throws Exception {
        try (InputStream is = MetadataManagerIT.class.getClassLoader().getResourceAsStream(classpathPath)) {
            assertNotNull(is, "Test-Resource nicht gefunden: " + classpathPath);
            return is.readAllBytes();
        }
    }

    private static void injectEntityManagerIntoDao(Object dao, EntityManager em) throws Exception {
        Class<?> c = dao.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField("em");
                f.setAccessible(true);
                f.set(dao, em);
                return;
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new IllegalStateException("Konnte EntityManager nicht in DAO injizieren (Feld 'em' nicht gefunden).");
    }

    private static void injectDaoIntoService(FotoService service, FotoDateiDAO dao) throws Exception {
        Field f = FotoService.class.getDeclaredField("fotoDateiDAO");
        f.setAccessible(true);
        f.set(service, dao);
    }
}
