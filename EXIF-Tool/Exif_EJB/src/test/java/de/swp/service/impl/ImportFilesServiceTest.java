package de.swp.service.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.swp.dao.FotoDateiDAO;
import de.swp.entity.impl.FotoDatei;

/**
 * UC1 – Dateien importieren (JUnit)
 *
 * Testbilder aus src/test/resources/images: - testbild1.jpg - testbild2.png -
 * testbild3.jpg
 */
class ImportFilesServiceTest {

    private FotoService service;
    private FotoDateiDAO fotoDateiDAO;

    @BeforeEach
    void setUp(@TempDir Path tempHome) throws Exception {
        // user.home setzen ist ok, aber FotoService.BASE_DIR kann static final sein
        System.setProperty("user.home", tempHome.toAbsolutePath().toString());

        service = spy(new FotoService());
        fotoDateiDAO = mock(FotoDateiDAO.class);

        // DAO per Reflection injizieren
        var f = FotoService.class.getDeclaredField("fotoDateiDAO");
        f.setAccessible(true);
        f.set(service, fotoDateiDAO);

        // save(..) liefert Entity mit ID zurück
        when(fotoDateiDAO.save(any(FotoDatei.class))).thenAnswer(invocation -> {
            FotoDatei arg = invocation.getArgument(0, FotoDatei.class);
            if (arg.getId() == null) {
                arg.setId(1L);
            }
            return arg;
        });
    }

    // Helpers
    private static InputStream resource(String classpathPath) {
        InputStream is = ImportFilesServiceTest.class.getClassLoader().getResourceAsStream(classpathPath);
        assertNotNull(is, "Test-Resource nicht gefunden: " + classpathPath);
        return is;
    }

    private static byte[] bytesOfResource(String classpathPath) throws Exception {
        try (InputStream is = resource(classpathPath)) {
            return is.readAllBytes();
        }
    }

    private static void assertLooksLikeJsonIfPresent(String s) {
        if (s == null) {
            return;
        }
        String t = s.trim();
        assertTrue(t.startsWith("{") && t.endsWith("}"),
                "Kein JSON: " + (t.length() > 80 ? t.substring(0, 80) + "..." : t));
    }

    private static Path fotoServiceBaseDir() {
        try {
            var baseDirField = FotoService.class.getDeclaredField("BASE_DIR");
            baseDirField.setAccessible(true);
            Object v = baseDirField.get(null);
            if (v instanceof String s) {
                return Path.of(s);
            }
            if (v instanceof Path p) {
                return p;
            }
            throw new IllegalStateException("Unerwarteter Typ für BASE_DIR: " + (v == null ? "null" : v.getClass().getName()));
        } catch (Exception e) {
            throw new IllegalStateException("Konnte FotoService.BASE_DIR nicht lesen", e);
        }
    }

    private static void assertFileExistsAndNonEmpty(Path p) throws Exception {
        assertTrue(Files.exists(p), "Datei wurde nicht geschrieben: " + p);
        assertTrue(Files.size(p) > 0, "Datei ist leer: " + p);
    }

    /**
     * Wichtig für deterministische Tests: Löscht NUR Dateien, die zu Test
     * gehören (Prefix testbild1 + .jpg), damit der erste Import garantiert
     * "testbild1.jpg" bekommt.
     */
    private static void cleanupVersionedTestFiles(Path baseDir, String baseName, String ext) throws Exception {
        Files.createDirectories(baseDir);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir, baseName + "*" + ext)) {
            for (Path p : stream) {
                Files.deleteIfExists(p);
            }
        }
    }

    @Test
    void UC1_TC_01_importJpeg_testbild1_shouldSaveFileAndMetadata() throws Exception {
        String filename = "testbild1.jpg";
        byte[] bytes = bytesOfResource("images/" + filename);

        doNothing().when(service).reverseGeocodeAsync(anyLong(), anyDouble(), anyDouble());

        service.saveImage(new ByteArrayInputStream(bytes), filename, filename);

        ArgumentCaptor<FotoDatei> captor = ArgumentCaptor.forClass(FotoDatei.class);
        verify(fotoDateiDAO, times(1)).save(captor.capture());
        FotoDatei saved = captor.getValue();

        assertNotNull(saved.getAufnahmeDatum(), "AufnahmeDatum sollte gesetzt sein");
        assertLooksLikeJsonIfPresent(saved.getJsonMetadaten());
        assertNotNull(saved.getMetadatenJson(), "metadatenJson sollte gesetzt sein");
        assertFalse(saved.getMetadatenJson().isBlank(), "metadatenJson sollte nicht leer sein");

        Path baseDir = fotoServiceBaseDir();
        // wegen Versionierung kann uniqueName ggf. nicht exakt filename sein -> wir prüfen über saved.getUniqueName()
        assertFileExistsAndNonEmpty(baseDir.resolve(saved.getUniqueName()));

        if (saved.getBreitengrad() != null && saved.getLaengengrad() != null) {
            verify(service, times(1))
                    .reverseGeocodeAsync(eq(Objects.requireNonNull(saved.getId())), anyDouble(), anyDouble());
        } else {
            verify(service, never()).reverseGeocodeAsync(anyLong(), anyDouble(), anyDouble());
        }
    }

    @Test
    void UC1_TC_02_importPng_testbild2_shouldImportButNoGpsAndNoReverseGeocode() throws Exception {
        String filename = "testbild2.png";
        byte[] bytes = bytesOfResource("images/" + filename);

        doNothing().when(service).reverseGeocodeAsync(anyLong(), anyDouble(), anyDouble());

        service.saveImage(new ByteArrayInputStream(bytes), filename, filename);

        ArgumentCaptor<FotoDatei> captor = ArgumentCaptor.forClass(FotoDatei.class);
        verify(fotoDateiDAO, times(1)).save(captor.capture());
        FotoDatei saved = captor.getValue();

        assertEquals(filename, saved.getName());
        assertNull(saved.getBreitengrad(), "PNG sollte keine Breitengrad-Daten haben");
        assertNull(saved.getLaengengrad(), "PNG sollte keine Längengrad-Daten haben");
        verify(service, never()).reverseGeocodeAsync(anyLong(), anyDouble(), anyDouble());

        Path baseDir = fotoServiceBaseDir();
        assertFileExistsAndNonEmpty(baseDir.resolve(saved.getUniqueName()));
    }

    @Test
    void UC1_TC_03_importPdf_shouldBeRejected() throws Exception {
        byte[] pdfBytes = ("%PDF-1.4\n"
                + "1 0 obj\n<< /Type /Catalog >>\nendobj\n"
                + "xref\n0 2\n0000000000 65535 f \n0000000010 00000 n \n"
                + "trailer\n<< /Root 1 0 R >>\nstartxref\n42\n%%EOF").getBytes(StandardCharsets.US_ASCII);

        String filename = "test.pdf";

        assertThrows(Exception.class, ()
                -> service.saveImage(new ByteArrayInputStream(pdfBytes), filename, filename)
        );

        verify(fotoDateiDAO, never()).save(any());

        Path baseDir = fotoServiceBaseDir();
        assertFalse(Files.exists(baseDir.resolve(filename)), "Bei PDF darf keine Datei geschrieben werden");
    }

    @Test
    void UC1_TC_04_importMultipleFiles_shouldImportAll() throws Exception {
        String f1 = "testbild1.jpg";
        String f2 = "testbild3.jpg";

        byte[] bytes1 = bytesOfResource("images/" + f1);
        byte[] bytes2 = bytesOfResource("images/" + f2);

        doNothing().when(service).reverseGeocodeAsync(anyLong(), anyDouble(), anyDouble());

        service.saveImage(new ByteArrayInputStream(bytes1), f1, f1);
        service.saveImage(new ByteArrayInputStream(bytes2), f2, f2);

        verify(fotoDateiDAO, times(2)).save(any(FotoDatei.class));

        Path baseDir = fotoServiceBaseDir();
        // wegen Versionierung prüfen wir die tatsächlichen uniqueNames über DAO-Captor wäre möglich
    }

}
