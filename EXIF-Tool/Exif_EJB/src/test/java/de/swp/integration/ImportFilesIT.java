package de.swp.integration;

import de.swp.dao.FotoDateiDAO;
import de.swp.entity.impl.FotoDatei;
import de.swp.service.impl.FotoService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImportFilesIT {

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
    void UC1_IT_01_saveImage_shouldWriteFileAndPersistDbRow() throws Exception {
        // Arrange
        byte[] jpegBytes = loadResourceBytes("images/testbild1.jpg");

        String uniqueName = "it_import_" + UUID.randomUUID() + ".jpg";
        String originalFilename = "original_" + uniqueName;

        Path targetFile = baseDir().resolve(uniqueName);
        deleteIfExists(targetFile);

        // Act
        em.getTransaction().begin();
        service.saveImage(new ByteArrayInputStream(jpegBytes), uniqueName, originalFilename);
        em.getTransaction().commit();

        // Assert: Datei geschrieben
        assertTrue(Files.exists(targetFile), "Datei wurde nicht geschrieben: " + targetFile);
        assertTrue(Files.size(targetFile) > 0, "Datei ist leer: " + targetFile);

        // Assert: DB-Eintrag vorhanden
        FotoDatei persisted = em.createQuery(
                        "SELECT f FROM FotoDatei f WHERE f.uniqueName = :u",
                        FotoDatei.class)
                .setParameter("u", uniqueName)
                .getSingleResult();

        assertNotNull(persisted, "DB-Eintrag muss existieren");
        assertEquals(uniqueName, persisted.getUniqueName(), "uniqueName muss gespeichert werden");
        assertEquals(originalFilename, persisted.getName(), "Original-Dateiname muss in name gespeichert werden");

        // Cleanup
        deleteIfExists(targetFile);
    }

    @Test
    void UC1_IT_05_whenFileExists_shouldVersionUniqueName_andCreateSecondDbRow() throws Exception {
        // Arrange
        byte[] jpegBytes = loadResourceBytes("images/testbild1.jpg");

        // Wir importieren zweimal mit exakt dem selben uniqueName
        // Erwartung: beim zweiten Mal muss FotoService.ensureUniqueName() einen neuen Namen erzeugen.
        String baseUniqueName = "it_version_" + UUID.randomUUID() + ".jpg";
        String originalFilename = "original_" + baseUniqueName;

        Path file1 = baseDir().resolve(baseUniqueName);
        deleteIfExists(file1);

        // 1) erster Import -> schreibt baseUniqueName
        em.getTransaction().begin();
        service.saveImage(new ByteArrayInputStream(jpegBytes), baseUniqueName, originalFilename);
        em.getTransaction().commit();

        assertTrue(Files.exists(file1), "Erste Datei muss existieren: " + file1);

        // 2) zweiter Import mit gleichem uniqueName -> Versionierung
        em.getTransaction().begin();
        service.saveImage(new ByteArrayInputStream(jpegBytes), baseUniqueName, originalFilename);
        em.getTransaction().commit();

        // Assert: es müssen jetzt 2 Datensätze existieren
        List<FotoDatei> rows = em.createQuery(
                        "SELECT f FROM FotoDatei f WHERE f.uniqueName LIKE :p",
                        FotoDatei.class)
                .setParameter("p", baseUniqueName.replace(".jpg", "") + "%")
                .getResultList();

        assertEquals(2, rows.size(), "Es müssen 2 DB-Zeilen existieren (Original + Version)");

        // uniqueNames extrahieren
        String u1 = rows.get(0).getUniqueName();
        String u2 = rows.get(1).getUniqueName();
        assertNotNull(u1);
        assertNotNull(u2);

        // mindestens einer muss exakt baseUniqueName sein
        assertTrue(u1.equals(baseUniqueName) || u2.equals(baseUniqueName),
                "Einer der Datensätze muss den ursprünglichen uniqueName haben");

        // und einer muss davon abweichen (Version)
        assertNotEquals(u1, u2, "Die uniqueNames müssen unterschiedlich sein (Versionierung)");

        String versionedUniqueName = u1.equals(baseUniqueName) ? u2 : u1;

        // Assert: zweite Datei existiert auch auf Disk
        Path file2 = baseDir().resolve(versionedUniqueName);
        assertTrue(Files.exists(file2), "Versionierte Datei muss existieren: " + file2);
        assertTrue(Files.size(file2) > 0, "Versionierte Datei darf nicht leer sein: " + file2);

        // Cleanup
        deleteIfExists(file1);
        deleteIfExists(file2);
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

    private static Path baseDir() {
        return Paths.get(System.getProperty("user.home"), "v2metatool1", "bildDateien");
    }

    private static void deleteIfExists(Path p) throws Exception {
        if (p != null && Files.exists(p)) {
            Files.delete(p);
        }
    }

    private static byte[] loadResourceBytes(String classpathPath) throws Exception {
        try (InputStream is = ImportFilesIT.class.getClassLoader().getResourceAsStream(classpathPath)) {
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
