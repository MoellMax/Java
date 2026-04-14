package de.swp.integration;

import de.swp.dao.FotoDateiDAO;
import de.swp.entity.impl.FotoDatei;
import de.swp.service.impl.FotoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.json.JSONArray;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SearchIT {

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
    void UC5_IT_01_searchAll_shouldReturnMatchingPhotos() {
        em.getTransaction().begin();
        persistFoto("a.jpg", "Alpha", "{\"Make\":\"Canon\"}", 8.0);
        persistFoto("b.jpg", "Beta", "{\"Make\":\"Canon\"}", null);
        persistFoto("c.jpg", "Gamma", "{\"Make\":\"Nikon\"}", 7.0);
        em.getTransaction().commit();

        JSONArray result = service.searchPhotosByMetadata("Canon", "all", 1, 10);

        assertNotNull(result);
        assertEquals(2, result.length(), "Es müssen 2 Canon-Treffer zurückkommen");

        assertTrue(containsUniqueName(result, "a.jpg"), "a.jpg muss enthalten sein");
        assertTrue(containsUniqueName(result, "b.jpg"), "b.jpg muss enthalten sein");
    }

    @Test
    void UC5_IT_02_searchGpsFilter_shouldReturnOnlyGpsPhotos() {
        em.getTransaction().begin();
        persistFoto("no_gps.jpg", "NoGps", "{\"Make\":\"Canon\"}", null);
        persistFoto("with_gps.jpg", "WithGps", "{\"Make\":\"Canon\"}", 8.0);
        em.getTransaction().commit();

        JSONArray result = service.searchPhotosByMetadata("Canon", "gps", 1, 10);

        assertNotNull(result);
        assertEquals(1, result.length());
        assertEquals("with_gps.jpg", result.getJSONObject(0).getString("uniqueName"));
        assertEquals("WithGps", result.getJSONObject(0).getString("displayName"));
    }

    @Test
    void UC5_IT_03_searchNoGpsFilter_shouldReturnOnlyNonGpsPhotos() {
        em.getTransaction().begin();
        persistFoto("no_gps.jpg", "NoGps", "{\"Make\":\"Canon\"}", null);
        persistFoto("with_gps.jpg", "WithGps", "{\"Make\":\"Canon\"}", 8.0);
        em.getTransaction().commit();

        JSONArray result = service.searchPhotosByMetadata("Canon", "no_gps", 1, 10);

        assertNotNull(result);
        assertEquals(1, result.length());
        assertEquals("no_gps.jpg", result.getJSONObject(0).getString("uniqueName"));
        assertEquals("NoGps", result.getJSONObject(0).getString("displayName"));
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

    private void persistFoto(String uniqueName, String name, String metadatenJson, Double longitude) {
        FotoDatei f = new FotoDatei();
        f.setUniqueName(uniqueName);
        f.setName(name);
        f.setMetadatenJson(metadatenJson);
        f.setLaengengrad(longitude);
        f.setGeaendert_Am(LocalDateTime.now());

        HashMap<String, String> map = new HashMap<>();
        map.put("Name", name);
        f.setMetadaten(map);

        em.persist(f);
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
