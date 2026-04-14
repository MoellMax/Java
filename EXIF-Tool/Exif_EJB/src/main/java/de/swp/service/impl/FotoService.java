package de.swp.service.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoAscii;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.json.JSONArray;
import org.json.JSONObject;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.iptc.IptcDirectory;
import com.drew.metadata.xmp.XmpDirectory;

import de.swp.dao.FotoDateiDAO;
import de.swp.entity.FotoDateiTO;
import de.swp.entity.impl.FotoDatei;
import de.swp.service.IFotoService;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class FotoService implements IFotoService {

    @Inject
    FotoDateiDAO fotoDateiDAO;

    /** Liest Metadaten aus der Bilddatei und erstellt ein FotoDatei-Objekt */
    public FotoDatei auslesen(File bildFile, String hash) throws Exception {

        FotoDatei foto = new FotoDatei();
        foto.setHash_identifikator(hash);
        foto.setName(bildFile.getName());
        foto.setAufnahmeDatum(LocalDate.now());
        foto.setGeaendert_Am(LocalDateTime.now());

        Map<String, String> metadatenMap = new HashMap<>();

        var metadata = ImageMetadataReader.readMetadata(bildFile);

        for (Directory dir : metadata.getDirectories()) {
            dir.getTags().forEach(tag -> metadatenMap.put(sanitize(tag.getTagName()), sanitize(tag.getDescription())));

            if (dir instanceof GpsDirectory gpsDir) {
                GeoLocation geo = gpsDir.getGeoLocation();
                if (geo != null && !geo.isZero()) {
                    foto.setBreitengrad(geo.getLatitude());
                    foto.setLaengengrad(geo.getLongitude());
                }
            }

            if (dir instanceof ExifIFD0Directory exifDir) {
                foto.setTitel(exifDir.getString(ExifIFD0Directory.TAG_IMAGE_DESCRIPTION));
                foto.setAutoren(exifDir.getString(ExifIFD0Directory.TAG_ARTIST));
                foto.setKameraHersteller(exifDir.getString(ExifIFD0Directory.TAG_MAKE));
                foto.setKameraModell(exifDir.getString(ExifIFD0Directory.TAG_MODEL));
            }

            if (dir instanceof ExifSubIFDDirectory subDir) {
                var date = subDir.getDateOriginal();
                if (date != null) {
                    foto.setAufnahmeDatum(LocalDate.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault()));
                }
            }
        }

        // Apache Imaging optional für zusätzliche Metadaten
        try {
            ImageMetadata imagingMeta = Imaging.getMetadata(bildFile);
            if (imagingMeta != null) {
                for (Object item : imagingMeta.getItems()) {
                    metadatenMap.put(item.toString(), item.toString());
                }

                String ext = getFileExtension(bildFile).toLowerCase();
                if ((ext.equals("jpg") || ext.equals("jpeg")) && imagingMeta instanceof JpegImageMetadata jpegMeta) {
                    jpegMeta.getExif().getAllFields().forEach(f -> {
                        try {
                            metadatenMap.put(f.getTagName(), String.valueOf(f.getValue()));
                        } catch (Exception e) {
                            metadatenMap.put(f.getTagName(), "Fehler beim Lesen");
                        }
                    });
                }
            }
        } catch (Exception ignored) {
        }

        foto.setMetadaten(metadatenMap);
        return foto;
    }

    /**
     * Sanitizes strings by removing null bytes which cause PostgreSQL UTF-8 errors
     */
    private String sanitize(String value) {
        if (value == null)
            return null;
        return value.replace("\u0000", "").trim();
    }

    // /** Speichert oder aktualisiert ein FotoDatei-Objekt in der Datenbank */
    // @Transactional
    // public FotoDatei speichern(FotoDatei foto) {
    // if (foto == null) {
    // throw new IllegalArgumentException("FotoDatei darf nicht null sein.");
    // }

    // // Prüfen, ob eine Entität mit der ID existiert
    // if (foto.getId() != null) {
    // return em.merge(foto);
    // }

    // // Prüfen, ob ein Eintrag mit demselben hash_identifikator existiert
    // if (foto.getHash_identifikator() != null) {
    // TypedQuery<FotoDatei> query = em.createNamedQuery(FotoDatei.FIND_BY_HASH,
    // FotoDatei.class);
    // query.setParameter("hash_identifikator", foto.getHash_identifikator());
    // try {
    // FotoDatei bestehend = query.getSingleResult();
    // bestehend.setName(foto.getName());
    // bestehend.setTitel(foto.getTitel());
    // bestehend.setBetreff(foto.getBetreff());
    // bestehend.setAutoren(foto.getAutoren());
    // bestehend.setAufnahmeDatum(foto.getAufnahmeDatum());
    // bestehend.setCopyright(foto.getCopyright());
    // bestehend.setKameraHersteller(foto.getKameraHersteller());
    // bestehend.setKameraModell(foto.getKameraModell());
    // bestehend.setObjektivHersteller(foto.getObjektivHersteller());
    // bestehend.setObjektivModell(foto.getObjektivModell());
    // bestehend.setLaengengrad(foto.getLaengengrad());
    // bestehend.setBreitengrad(foto.getBreitengrad());
    // bestehend.setGeaendert_Am(foto.getGeaendert_Am());
    // bestehend.setMetadaten(foto.getMetadaten());
    // return em.merge(bestehend);
    // } catch (jakarta.persistence.NoResultException e) {
    // em.persist(foto);
    // return foto;
    // }
    // }

    // em.persist(foto);
    // return foto;
    // }

    // /** Löscht ein FotoDatei-Objekt aus der Datenbank */
    // @Transactional
    // public void loeschen(FotoDatei foto) {
    // if (foto == null)
    // return;

    // FotoDatei managed = null;
    // if (foto.getId() != null) {
    // managed = em.find(FotoDatei.class, foto.getId());
    // } else if (foto.getHash_identifikator() != null) {
    // TypedQuery<FotoDatei> query = em.createNamedQuery(FotoDatei.FIND_BY_HASH,
    // FotoDatei.class);
    // query.setParameter("hash_identifikator", foto.getHash_identifikator());
    // try {
    // managed = query.getSingleResult();
    // } catch (jakarta.persistence.NoResultException ignored) {
    // }
    // }

    // if (managed != null) {
    // em.remove(managed);
    // }
    // }

    /** Hilfsmethode: Dateiendung ermitteln */
    private String getFileExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return (dot > 0 && dot < name.length() - 1) ? name.substring(dot + 1) : "";
    }

    /**
     * Bearbeitet EXIF-Daten eines Bildes und gibt das neue Bild zurück
     */
    public byte[] updateExif(byte[] imageBytes, FotoDatei data) throws Exception {

        // 1. Bestehende EXIF auslesen
        JpegImageMetadata jpegMeta = (JpegImageMetadata) Imaging.getMetadata(imageBytes);

        TiffOutputSet outputSet = null;

        if (jpegMeta != null && jpegMeta.getExif() != null) {
            TiffImageMetadata exif = jpegMeta.getExif();
            outputSet = exif.getOutputSet();
        }

        if (outputSet == null) {
            outputSet = new TiffOutputSet();
        }

        TiffOutputDirectory rootDir = outputSet.getOrCreateRootDirectory();
        TiffOutputDirectory exifDir = outputSet.getOrCreateExifDirectory();
        TiffOutputDirectory gpsDir = outputSet.getOrCreateGPSDirectory();

        // -------- TEXT-TAGS --------

        setAscii(rootDir, TiffTagConstants.TIFF_TAG_PAGE_NAME, data.getName());
        setAscii(rootDir, TiffTagConstants.TIFF_TAG_IMAGE_DESCRIPTION, data.getTitel());
        setAscii(rootDir, TiffTagConstants.TIFF_TAG_DOCUMENT_NAME, data.getBetreff());
        setAscii(rootDir, TiffTagConstants.TIFF_TAG_ARTIST, data.getAutoren());
        setAscii(rootDir, TiffTagConstants.TIFF_TAG_COPYRIGHT, data.getCopyright());
        setAscii(rootDir, TiffTagConstants.TIFF_TAG_MAKE, data.getKameraHersteller());
        setAscii(rootDir, TiffTagConstants.TIFF_TAG_MODEL, data.getKameraModell());
        setAscii(exifDir, ExifTagConstants.EXIF_TAG_LENS_MAKE, data.getObjektivHersteller());
        setAscii(exifDir, ExifTagConstants.EXIF_TAG_LENS_MODEL, data.getObjektivModell());

        // -------- DATUM --------

        if (data.getAufnahmeDatum() != null) {
            exifDir.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
            exifDir.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL,
                    toExifDate(data.getAufnahmeDatum()));
        }

        if (data.getGeaendert_Am() != null) {
            rootDir.removeField(TiffTagConstants.TIFF_TAG_DATE_TIME);
            rootDir.add(TiffTagConstants.TIFF_TAG_DATE_TIME,
                    toExifDate(data.getGeaendert_Am()));
        }

        // -------- GPS --------

        if (data.getBreitengrad() != null && data.getLaengengrad() != null) {

            RationalNumber[] latDMS = toDMS(Math.abs(data.getBreitengrad()));
            RationalNumber[] lonDMS = toDMS(Math.abs(data.getLaengengrad()));

            gpsDir.removeField(GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF);
            gpsDir.add(GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF,
                    data.getBreitengrad() >= 0 ? "N" : "S");

            gpsDir.removeField(GpsTagConstants.GPS_TAG_GPS_LATITUDE);
            gpsDir.add(GpsTagConstants.GPS_TAG_GPS_LATITUDE, latDMS);

            gpsDir.removeField(GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF);
            gpsDir.add(GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF,
                    data.getLaengengrad() >= 0 ? "E" : "W");

            gpsDir.removeField(GpsTagConstants.GPS_TAG_GPS_LONGITUDE);
            gpsDir.add(GpsTagConstants.GPS_TAG_GPS_LONGITUDE, lonDMS);
        }

        // -------- SCHREIBEN --------

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ExifRewriter().updateExifMetadataLossless(imageBytes, baos, outputSet);

        // hier wird anhand von dem Foto namen, das Foto aus der Datenbank geholt und
        // updated mit den neuen werten.
        FotoDateiTO afoto = fotoDateiDAO.findByUniqueName(data.getUniqueName());
        if (afoto != null) {
            afoto.setName(data.getName());
            afoto.setTitel(data.getTitel());
            afoto.setBetreff(data.getBetreff());
            afoto.setAutoren(data.getAutoren());
            afoto.setCopyright(data.getCopyright());
            afoto.setKameraHersteller(data.getKameraHersteller());
            afoto.setKameraModell(data.getKameraModell());
            afoto.setObjektivHersteller(data.getObjektivHersteller());
            afoto.setObjektivModell(data.getObjektivModell());
            afoto.setAufnahmeDatum(data.getAufnahmeDatum());
            afoto.setGeaendert_Am(data.getGeaendert_Am());

            if (data.getLaengengrad() != null)
                afoto.setLongitude(java.math.BigDecimal.valueOf(data.getLaengengrad()));

            if (data.getBreitengrad() != null)
                afoto.setLatitude(java.math.BigDecimal.valueOf(data.getBreitengrad()));

            // Auch die Geo-Rohdaten speichern
            afoto.setFotoDateiGeo(data.getFotoDateiGeo());

            // --- Synchronisierung von metadaten_json ---
            try {
                // 1. Basis ist das existierende jsonMetadaten (technische Daten)
                JSONObject metaJson = new JSONObject();
                if (afoto.getJsonMetadaten() != null && !afoto.getJsonMetadaten().isEmpty()) {
                    metaJson = new JSONObject(afoto.getJsonMetadaten());
                }

                // 2. Füge die bearbeitbaren Felder hinzu, damit sie aktuell sind
                if (data.getName() != null)
                    metaJson.put("Name", data.getName());
                if (data.getTitel() != null)
                    metaJson.put("Image Description", data.getTitel());
                if (data.getBetreff() != null)
                    metaJson.put("Document Name", data.getBetreff());
                if (data.getAutoren() != null)
                    metaJson.put("Artist", data.getAutoren());
                if (data.getCopyright() != null)
                    metaJson.put("Copyright", data.getCopyright());
                if (data.getKameraHersteller() != null)
                    metaJson.put("Make", data.getKameraHersteller());
                if (data.getKameraModell() != null)
                    metaJson.put("Model", data.getKameraModell());
                if (data.getObjektivHersteller() != null)
                    metaJson.put("Lens Make", data.getObjektivHersteller());
                if (data.getObjektivModell() != null)
                    metaJson.put("Lens Model", data.getObjektivModell());

                // 3. Füge die Geodaten hinzu
                if (data.getFotoDateiGeo() != null && !data.getFotoDateiGeo().isEmpty()) {
                    // Versuche das Geo-JSON zu parsen
                    try {
                        JSONObject geoObj = new JSONObject(data.getFotoDateiGeo());
                        metaJson.put("fotodateigeo", geoObj);
                    } catch (Exception e) {
                        // Fallback, falls String kein valides JSON ist
                        metaJson.put("fotodateigeo", data.getFotoDateiGeo());
                    }
                }

                // 4. Setze das aktualisierte JSON zurück
                afoto.setMetadatenJson(metaJson.toString());

            } catch (Exception e) {
                System.err.println("Fehler beim Aktualisieren von metadaten_json: " + e.getMessage());
                e.printStackTrace();
            }

            fotoDateiDAO.update(afoto.toFotoDatei());

            // --- Synchronisierung der metadaten Map ---
            Map<String, String> metadaten = afoto.getMetadaten();
            if (metadaten == null) {
                metadaten = new HashMap<>();
            }
            if (data.getName() != null)
                metadaten.put("Name", sanitize(data.getName()));
            if (data.getTitel() != null)
                metadaten.put("Image Description", sanitize(data.getTitel()));
            if (data.getBetreff() != null)
                metadaten.put("Document Name", sanitize(data.getBetreff()));
            if (data.getAutoren() != null)
                metadaten.put("Artist", sanitize(data.getAutoren()));
            if (data.getCopyright() != null)
                metadaten.put("Copyright", sanitize(data.getCopyright()));
            if (data.getKameraHersteller() != null)
                metadaten.put("Make", sanitize(data.getKameraHersteller()));
            if (data.getKameraModell() != null)
                metadaten.put("Model", sanitize(data.getKameraModell()));
            if (data.getObjektivHersteller() != null)
                metadaten.put("Lens Make", sanitize(data.getObjektivHersteller()));
            if (data.getObjektivModell() != null)
                metadaten.put("Lens Model", sanitize(data.getObjektivModell()));

            // --- Synchronisierung von GPS & Adresse in die Metadaten-Tabelle ---
            if (data.getLaengengrad() != null)
                metadaten.put("GPS Longitude", String.valueOf(data.getLaengengrad()));
            if (data.getBreitengrad() != null)
                metadaten.put("GPS Latitude", String.valueOf(data.getBreitengrad()));

            if (data.getFotoDateiGeo() != null && !data.getFotoDateiGeo().isEmpty()) {
                try {
                    JSONObject geoObj = new JSONObject(data.getFotoDateiGeo());
                    if (geoObj.has("display_name")) {
                        metadaten.put("Address", sanitize(geoObj.getString("display_name")));
                    }
                    if (geoObj.has("address")) {
                        JSONObject addr = geoObj.getJSONObject("address");
                        for (String key : addr.keySet()) {
                            metadaten.put("Geo_" + sanitize(key), sanitize(addr.optString(key, "")));
                        }
                    }
                } catch (Exception e) {
                    // Falls kein valides JSON, überspringen wir die detaillierte Synchronisierung
                }
            }

            afoto.setMetadaten(new HashMap<>(metadaten));
            fotoDateiDAO.update(afoto.toFotoDatei());
        }

        return baos.toByteArray();
    }

    private void setAscii(TiffOutputDirectory dir, TagInfoAscii tag, String value) throws Exception {
        if (value != null && !value.isBlank()) {
            dir.removeField(tag);
            dir.add(tag, value);
        }
    }

    private RationalNumber[] toDMS(double coord) {
        int deg = (int) coord;
        double minRaw = (coord - deg) * 60;
        int min = (int) minRaw;
        double sec = (minRaw - min) * 60;

        return new RationalNumber[] {
                RationalNumber.valueOf(deg),
                RationalNumber.valueOf(min),
                RationalNumber.valueOf(sec)
        };
    }

    private String toExifDate(LocalDateTime date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"));
    }

    private String toExifDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy:MM:dd")) + " 00:00:00";
    }

    public JSONObject extractMetadata(InputStream imageStream) throws Exception {

        Metadata metadata = ImageMetadataReader.readMetadata(imageStream);
        JSONObject result = new JSONObject();

        BigDecimal latitude = null;
        BigDecimal longitude = null;
        String latRef = null;
        String lonRef = null;

        for (Directory directory : metadata.getDirectories()) {
            JSONObject dirJson = new JSONObject();

            for (Tag tag : directory.getTags()) {
                if (tag.getDescription() != null) {
                    dirJson.put(tag.getTagName(), tag.getDescription());
                }

                // -------- GPS speziell abfangen --------
                if (directory instanceof GpsDirectory gpsDir) {

                    if (tag.getTagType() == GpsDirectory.TAG_LATITUDE) {
                        Rational[] dms = gpsDir.getRationalArray(GpsDirectory.TAG_LATITUDE);
                        latRef = gpsDir.getString(GpsDirectory.TAG_LATITUDE_REF);
                        latitude = dmsToDecimal(dms, latRef);
                    }

                    if (tag.getTagType() == GpsDirectory.TAG_LONGITUDE) {
                        Rational[] dms = gpsDir.getRationalArray(GpsDirectory.TAG_LONGITUDE);
                        lonRef = gpsDir.getString(GpsDirectory.TAG_LONGITUDE_REF);
                        longitude = dmsToDecimal(dms, lonRef);
                    }
                }
            }

            if (dirJson.length() > 0) {
                result.put(directory.getName(), dirJson);
            }
        }

        // Decimal-Werte zusätzlich einfügen
        if (latitude != null || longitude != null) {
            JSONObject gpsDecimal = new JSONObject();
            if (latitude != null)
                gpsDecimal.put("LatitudeDecimal", latitude);
            if (longitude != null)
                gpsDecimal.put("LongitudeDecimal", longitude);
            result.put("GPS_DECIMAL", gpsDecimal);
        }

        return result;
    }

    private static final String BASE_DIR = System.getProperty("user.home") + File.separator + "v2metatool1"
            + File.separator + "bildDateien";

    public void saveImage(InputStream inputStream, String uniqueName, String originalFilename) throws Exception {
        // Bild in ByteArray zwischenspeichern
        byte[] imageBytes = inputStream.readAllBytes();

        // EXIF-Metadaten auslesen
        Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageBytes));

        // Foto-Objekt erstellen
        FotoDateiTO foto = new FotoDateiTO();
        foto.setUniqueName(uniqueName);
        foto.setName(originalFilename); // Store original filename in 'name' field
        foto.setAufnahmeDatum(LocalDate.now()); // Fallback

        for (Directory dir : metadata.getDirectories()) {
            if (dir instanceof ExifIFD0Directory ifd0) {
                if (foto.getTitel() == null || foto.getTitel().isEmpty())
                    foto.setTitel(ifd0.getString(ExifIFD0Directory.TAG_IMAGE_DESCRIPTION));
                foto.setAutoren(ifd0.getString(ExifIFD0Directory.TAG_ARTIST));
                foto.setKameraHersteller(ifd0.getString(ExifIFD0Directory.TAG_MAKE));
                foto.setKameraModell(ifd0.getString(ExifIFD0Directory.TAG_MODEL));
                foto.setCopyright(ifd0.getString(ExifIFD0Directory.TAG_COPYRIGHT));
                // Windows Title/Subject Fallbacks
                if (foto.getTitel() == null || foto.getTitel().isEmpty())
                    foto.setTitel(ifd0.getString(ExifIFD0Directory.TAG_WIN_TITLE));
                if (foto.getBetreff() == null || foto.getBetreff().isEmpty())
                    foto.setBetreff(ifd0.getString(ExifIFD0Directory.TAG_WIN_SUBJECT));
            }

            if (dir instanceof IptcDirectory iptc) {
                if (foto.getTitel() == null || foto.getTitel().isEmpty())
                    foto.setTitel(iptc.getString(IptcDirectory.TAG_CAPTION));
                if (foto.getAutoren() == null || foto.getAutoren().isEmpty())
                    foto.setAutoren(iptc.getString(IptcDirectory.TAG_BY_LINE));
                if (foto.getBetreff() == null || foto.getBetreff().isEmpty())
                    foto.setBetreff(iptc.getString(IptcDirectory.TAG_OBJECT_NAME));
                if (foto.getCopyright() == null || foto.getCopyright().isEmpty())
                    foto.setCopyright(iptc.getString(IptcDirectory.TAG_COPYRIGHT_NOTICE));
            }

            if (dir instanceof XmpDirectory xmp) {
                if (foto.getTitel() == null || foto.getTitel().isEmpty())
                    foto.setTitel(xmp.getXmpProperties().get("dc:title"));
                if (foto.getAutoren() == null || foto.getAutoren().isEmpty())
                    foto.setAutoren(xmp.getXmpProperties().get("dc:creator"));
                if (foto.getBetreff() == null || foto.getBetreff().isEmpty())
                    foto.setBetreff(xmp.getXmpProperties().get("dc:description"));
            }

            if (dir instanceof ExifSubIFDDirectory subDir) {
                var date = subDir.getDateOriginal();
                if (date != null) {
                    foto.setAufnahmeDatum(LocalDate.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault()));
                }
                if (foto.getObjektivHersteller() == null)
                    foto.setObjektivHersteller(subDir.getString(ExifSubIFDDirectory.TAG_LENS_MAKE));
                if (foto.getObjektivModell() == null)
                    foto.setObjektivModell(subDir.getString(ExifSubIFDDirectory.TAG_LENS_MODEL));
            }

            if (dir instanceof GpsDirectory gpsDir) {
                GeoLocation geo = gpsDir.getGeoLocation();
                if (geo != null && !geo.isZero()) {
                    foto.setLatitude(BigDecimal.valueOf(geo.getLatitude()));
                    foto.setLongitude(BigDecimal.valueOf(geo.getLongitude()));
                }
            }
        }

        // JSONObject für jsonMetadaten (technische Rohdaten)
        JSONObject exifJson = extractMetadata(new ByteArrayInputStream(imageBytes));
        // In Datenbank speichern
        foto.setJsonMetadaten(exifJson.toString());

        Map<String, Object> combinedMetadata = new HashMap<>();
        combinedMetadata.put("exif", toMap(exifJson));

        com.google.gson.Gson gson = new com.google.gson.Gson();
        foto.setMetadatenJson(gson.toJson(combinedMetadata));

        HashMap<String, String> metadatenMap = new HashMap<>();
        for (String dirKey : exifJson.keySet()) {
            Object dirObj = exifJson.get(dirKey);
            if (dirObj instanceof JSONObject) {
                JSONObject dirJson = (JSONObject) dirObj;
                for (String tagKey : dirJson.keySet()) {
                    metadatenMap.put(sanitize(tagKey), sanitize(dirJson.optString(tagKey, "")));
                }
            }
        }
        foto.setMetadaten(metadatenMap);

        // Save to database first
        FotoDatei savedFoto = fotoDateiDAO.save(foto.toFotoDatei());

        // Save image file to disk
        Path targetPath = Paths.get(BASE_DIR, uniqueName);
        Files.createDirectories(targetPath.getParent());
        Files.write(targetPath, imageBytes);

        // Trigger async reverse geocoding if GPS data is available
        if (foto.getLatitude() != null && foto.getLongitude() != null) {
            reverseGeocodeAsync(
                    savedFoto.getId(),
                    foto.getLatitude().doubleValue(),
                    foto.getLongitude().doubleValue());
        }

    }

    // Helper method to convert JSONObject to Map
    private Map<String, Object> toMap(JSONObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        if (jsonObject == null) {
            return map;
        }
        jsonObject.keys().forEachRemaining(key -> {
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.put(key, toMap((JSONObject) value));
            } else if (value instanceof org.json.JSONArray) {
                map.put(key, toList((org.json.JSONArray) value));
            } else {
                map.put(key, value);
            }
        });
        return map;
    }

    private java.util.List<Object> toList(org.json.JSONArray jsonArray) {
        java.util.List<Object> list = new java.util.ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONObject) {
                list.add(toMap((JSONObject) value));
            } else if (value instanceof org.json.JSONArray) {
                list.add(toList((org.json.JSONArray) value));
            } else {
                list.add(value);
            }
        }
        return list;
    }

    public void metaDatenAusDatenbankEntfernen(String filename) {
        FotoDateiTO aDateiTO = fotoDateiDAO.findByUniqueName(filename);
        if (aDateiTO != null) {
            fotoDateiDAO.delete(aDateiTO.toFotoDatei());
        }
    }

    private byte[] setUserCommentOnce(byte[] imageBytes, String uniqueName) throws Exception {

        JpegImageMetadata jpegMeta = (JpegImageMetadata) Imaging.getMetadata(imageBytes);

        TiffOutputSet outputSet;

        if (jpegMeta != null && jpegMeta.getExif() != null) {
            outputSet = jpegMeta.getExif().getOutputSet();
        } else {
            outputSet = new TiffOutputSet();
        }

        TiffOutputDirectory exifDir = outputSet.getOrCreateExifDirectory();

        // WICHTIG: nur beim Speichern → kein removeField nötig,
        // weil das Bild neu ist
        exifDir.removeField(ExifTagConstants.EXIF_TAG_USER_COMMENT);
        exifDir.add(
                ExifTagConstants.EXIF_TAG_USER_COMMENT,
                uniqueName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ExifRewriter()
                .updateExifMetadataLossless(imageBytes, baos, outputSet);

        return baos.toByteArray();
    }

    // für den Datenbank upload
    public static BigDecimal dmsToDecimal(String dmsString, String ref) {
        if (dmsString == null || dmsString.isBlank())
            return null;

        // Beispiel dmsString: "44° 22' 44,89\""
        dmsString = dmsString.replace("°", " ")
                .replace("'", " ")
                .replace("\"", " ")
                .replace(",", ".")
                .trim();

        String[] parts = dmsString.split("\\s+");
        if (parts.length < 3)
            return null;

        try {
            double deg = Double.parseDouble(parts[0]);
            double min = Double.parseDouble(parts[1]);
            double sec = Double.parseDouble(parts[2]);

            double decimal = deg + min / 60.0 + sec / 3600.0;

            // Richtung berücksichtigen: S oder W → negativ
            if (ref != null && (ref.equalsIgnoreCase("S") || ref.equalsIgnoreCase("W"))) {
                decimal = -decimal;
            }

            return BigDecimal.valueOf(decimal);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    public BigDecimal dmsToDecimal(
            Rational[] dms,
            String ref) {
        if (dms == null || dms.length != 3)
            return null;

        BigDecimal deg = BigDecimal.valueOf(dms[0].doubleValue());
        BigDecimal min = BigDecimal.valueOf(dms[1].doubleValue());
        BigDecimal sec = BigDecimal.valueOf(dms[2].doubleValue());

        BigDecimal decimal = deg
                .add(min.divide(BigDecimal.valueOf(60), 8, RoundingMode.HALF_UP))
                .add(sec.divide(BigDecimal.valueOf(3600), 8, RoundingMode.HALF_UP));

        if ("S".equalsIgnoreCase(ref) || "W".equalsIgnoreCase(ref)) {
            decimal = decimal.negate();
        }

        return decimal;
    }

    // Alle Bilder werden geholt
    // Galerie via Datenbank
    public JSONArray getAllImages(int page, int pageSize) throws Exception {
        List<FotoDatei> results = fotoDateiDAO.findPaginated(page, pageSize, "all");
        JSONArray array = new JSONArray();
        for (FotoDatei result : results) {
            JSONObject obj = new JSONObject();
            obj.put("uniqueName", result.getUniqueName());
            obj.put("displayName", result.getName() != null ? result.getName() : result.getUniqueName());
            array.put(obj);
        }
        return array;
    }

    public void deleteAllImages() throws Exception {
        // 1. Alle Einträge aus der Datenbank löschen
        fotoDateiDAO.deleteAll();

        // 2. Alle Dateien im BASE_DIR löschen
        File folder = new File(BASE_DIR);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        file.delete();
                    }
                }
            }
        }
    }

    @Override
    @Asynchronous
    public void reverseGeocodeAsync(Long fotoId, double latitude, double longitude) {
        try {
            // Perform reverse geocoding
            String geoJson = de.swp.util.FotoMetadatenUtil.reverseGeocode(latitude, longitude);

            if (geoJson == null || geoJson.isEmpty()) {
                System.err.println("Reverse geocoding returned empty result for foto ID: " + fotoId);
                return;
            }

            // Find the foto entity
            FotoDatei foto = fotoDateiDAO.findById(fotoId);
            if (foto == null) {
                System.err.println("Foto not found for ID: " + fotoId);
                return;
            }

            // Update fotoDateiGeo
            foto.setFotoDateiGeo(geoJson);

            // Update metadatenJson with geo data
            if (foto.getMetadatenJson() != null && !foto.getMetadatenJson().isEmpty()) {
                try {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.Map<String, Object>>() {
                    }.getType();
                    java.util.Map<String, Object> metadatenMap = gson.fromJson(foto.getMetadatenJson(), type);

                    // Add geo data to existing metadata
                    metadatenMap.put("geo", toMap(new org.json.JSONObject(geoJson)));

                    // Save updated JSON
                    foto.setMetadatenJson(gson.toJson(metadatenMap));
                } catch (Exception e) {
                    System.err.println("Error updating metadatenJson with geo data: " + e.getMessage());
                }
            }

            // Update metadaten map with geo data
            Map<String, String> metadaten = foto.getMetadaten();
            if (metadaten == null) {
                metadaten = new HashMap<>();
            }
            try {
                JSONObject geoObj = new JSONObject(geoJson);
                if (geoObj.has("display_name")) {
                    metadaten.put("Address", sanitize(geoObj.getString("display_name")));
                }
                if (geoObj.has("address")) {
                    JSONObject addr = geoObj.getJSONObject("address");
                    for (String key : addr.keySet()) {
                        metadaten.put("Geo_" + sanitize(key), sanitize(addr.optString(key, "")));
                    }
                }
            } catch (Exception e) {
                System.err.println("Error updating metadaten map with geo data: " + e.getMessage());
            }
            foto.setMetadaten(metadaten);

            // Save to database
            fotoDateiDAO.update(foto);
            System.out.println("Async geocoding completed for foto ID: " + fotoId);

        } catch (Exception e) {
            System.err.println("Error in async reverse geocoding for foto ID " + fotoId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public JSONArray searchPhotosByMetadata(String query, String filter, int page, int pageSize) {
        List<FotoDatei> results = fotoDateiDAO.findByMetadatenJson(query, filter, page, pageSize);
        JSONArray array = new JSONArray();
        for (FotoDatei result : results) {
            JSONObject obj = new JSONObject();
            obj.put("uniqueName", result.getUniqueName());
            obj.put("displayName", result.getName() != null ? result.getName() : result.getUniqueName());
            array.put(obj);
        }
        return array;
    }

    @Override
    public de.swp.entity.FotoDateiTO getFotoDatei(String uniqueName) {
        return fotoDateiDAO.findByUniqueName(uniqueName);
    }

    @Override
    public String geocode(String address) {
        return de.swp.util.FotoMetadatenUtil.geocode(address);
    }

    @Override
    public JSONArray getImagesByFilter(String filter, int page, int pageSize) {
        List<FotoDatei> results = fotoDateiDAO.findPaginated(page, pageSize, filter);

        JSONArray array = new JSONArray();
        for (FotoDatei result : results) {
            JSONObject obj = new JSONObject();
            obj.put("uniqueName", result.getUniqueName());
            obj.put("displayName", result.getName() != null ? result.getName() : result.getUniqueName());
            array.put(obj);
        }
        return array;
    }
}

