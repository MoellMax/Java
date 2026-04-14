package de.swp.integration;

import de.swp.dao.FotoDateiDAO;
import de.swp.entity.FotoDateiTO;
import de.swp.entity.impl.FotoDatei;
import de.swp.service.impl.FotoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShowMetadataIT {

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
    void UC2_IT_01_getFotoDatei_shouldReturnMetadataFromDatabase() {
        // Arrange
        String uniqueName = "uc2_one.jpg";

        em.getTransaction().begin();
        FotoDatei f = new FotoDatei();
        f.setUniqueName(uniqueName);
        f.setName("Bild Eins");
        f.setAufnahmeDatum(LocalDate.of(2024, 1, 1));
        f.setGeaendert_Am(LocalDateTime.now());
        f.setMetadatenJson("{\"Make\":\"Canon\"}");
        HashMap<String, String> map = new HashMap<>();
        map.put("Make", "Canon");
        f.setMetadaten(map);
        em.persist(f);
        em.getTransaction().commit();

        // Act
        FotoDateiTO result = service.getFotoDatei(uniqueName);

        // Assert
        assertNotNull(result, "getFotoDatei muss ein TO liefern");
        assertEquals(uniqueName, result.getUniqueName());
        assertEquals("Bild Eins", result.getName());
        assertNotNull(result.getMetadatenJson(), "metadatenJson sollte gesetzt sein");
    }

    @Test
    void UC2_IT_02_getAllImages_shouldReturnEntriesAndFallbackDisplayName() throws Exception {
        // Arrange: 2 Bilder, eins ohne name -> Fallback auf uniqueName
        em.getTransaction().begin();

        FotoDatei a = new FotoDatei();
        a.setUniqueName("uc2_a.jpg");
        a.setName("Alpha");
        a.setGeaendert_Am(LocalDateTime.now());
        em.persist(a);

        FotoDatei b = new FotoDatei();
        b.setUniqueName("uc2_b.jpg");
        b.setName(null); // -> displayName = uniqueName
        b.setGeaendert_Am(LocalDateTime.now());
        em.persist(b);

        em.getTransaction().commit();

        // Act
        JSONArray arr = service.getAllImages(1, 10);

        // Assert
        assertNotNull(arr);
        assertEquals(2, arr.length());

        // Reihenfolge ist id DESC; wir prüfen daher über "contains"
        JSONObject o0 = arr.getJSONObject(0);
        JSONObject o1 = arr.getJSONObject(1);

        assertTrue(containsUniqueName(arr, "uc2_a.jpg"));
        assertTrue(containsUniqueName(arr, "uc2_b.jpg"));

        // Spot-check: bei uc2_b.jpg muss displayName fallback sein
        JSONObject bObj = "uc2_b.jpg".equals(o0.getString("uniqueName")) ? o0 : o1;
        if ("uc2_b.jpg".equals(bObj.getString("uniqueName"))) {
            assertEquals("uc2_b.jpg", bObj.getString("displayName"));
        }
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

    private static boolean containsUniqueName(JSONArray arr, String uniqueName) {
        for (int i = 0; i < arr.length(); i++) {
            if (uniqueName.equals(arr.getJSONObject(i).getString("uniqueName"))) return true;
        }
        return false;
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
