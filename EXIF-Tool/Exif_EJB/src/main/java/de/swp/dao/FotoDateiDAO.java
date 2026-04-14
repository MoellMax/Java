package de.swp.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.swp.entity.FotoDateiTO;
import de.swp.entity.impl.FotoDatei;
import jakarta.ejb.Stateless;

@Stateless
public class FotoDateiDAO extends GenericDAO<FotoDatei> {

    public FotoDateiDAO() {
        super(FotoDatei.class);
    }

    public FotoDatei upload(FotoDatei fotoDatei) {
        return super.save(fotoDatei);
    }

    // public FotoDatei upload(FotoDatei fotoDatei, File bildDatei) {
    // // EXIF Metadaten auslesen
    // Map<String, String> exif = FotoMetadatenUtil.leseEXIF(bildDatei);
    // fotoDatei.setMetadaten(exif);

    // // GPS
    // double[] coords = FotoMetadatenUtil.leseGPS(bildDatei);
    // if (coords != null) {
    // fotoDatei.setBreitengrad(coords[0]);
    // fotoDatei.setLaengengrad(coords[1]);

    // // Optional: Adresse über Reverse-Geocoding
    // String adresse = FotoMetadatenUtil.reverseGeocode(coords[0], coords[1]);
    // if (adresse != null) {
    // fotoDatei.getMetadaten().put("Adresse", adresse);
    // }
    // }

    // // Optional: AufnahmeDatum aus EXIF
    // String datum = exif.get("DateTimeOriginal");
    // if (datum != null) {
    // fotoDatei.setAufnahmeDatum(FotoMetadatenUtil.parseExifDate(datum));
    // }

    // return super.save(fotoDatei);
    // }

    public FotoDatei update(FotoDatei fotoDatei) {
        return super.merge(fotoDatei);
    }

    public List<FotoDatei> findAllFotoDateien() {
        return super.findAll();
    }

    // Löschen nach Objekt
    public void delete(FotoDatei fotoDatei) {
        super.delete(fotoDatei.getId(), FotoDatei.class);
    }

    // Suche nach ID
    public FotoDatei findById(Long id) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);

        return super.findOneResult(FotoDatei.FIND_BY_ID, parameters);
    }

    // Suche nach Hash
    public FotoDatei findByHash(String hash) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hash_identifikator", hash);

        return super.findOneResult(FotoDatei.FIND_BY_HASH, parameters);
    }

    // Suche nach Dateiname
    public FotoDateiTO findByName(String name) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", name);

        return super.findOneResult(FotoDatei.FIND_BY_NAME, parameters).toFotoDateiTO();
    }

    // Suche nach Metadaten-Key und -Value
    public List<FotoDatei> findByMetadaten(String key, String value) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("key", key);
        parameters.put("value", value);

        return super.findListResult(FotoDatei.FIND_BY_METADATEN_KEYVALUE, parameters);
    }

    // Optional: Suche nach Name oder Titel (Beispiel für dynamische Queries)
    public List<FotoDatei> findByNameOrTitel(String name, String titel) {
        String queryString = "SELECT f FROM FotoDatei f WHERE f.name = :name OR f.titel = :titel";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", name);
        parameters.put("titel", titel);

        return super.findListResult(queryString, parameters);
    }

    public List<FotoDatei> findByMetadatenJson(String query, String filter, int page, int pageSize) {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM fotodatei WHERE metadaten_json ILIKE :query");

            if ("gps".equalsIgnoreCase(filter)) {
                sql.append(" AND laengengrad IS NOT NULL");
            } else if ("no_gps".equalsIgnoreCase(filter)) {
                sql.append(" AND laengengrad IS NULL");
            }

            sql.append(" ORDER BY fotodatei_id DESC");

            jakarta.persistence.Query nativeQuery = super.getEm().createNativeQuery(sql.toString(), FotoDatei.class);
            nativeQuery.setParameter("query", "%" + query + "%");

            nativeQuery.setFirstResult((page - 1) * pageSize);
            nativeQuery.setMaxResults(pageSize);

            @SuppressWarnings("unchecked")
            List<FotoDatei> result = nativeQuery.getResultList();
            return result != null ? result : new java.util.ArrayList<>();
        } catch (Exception e) {
            System.out.println("Fehler bei der Suche in metadaten_json: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    public List<FotoDatei> findPaginated(int page, int pageSize, String filter) {
        String queryString = "SELECT f FROM FotoDatei f";
        if ("gps".equalsIgnoreCase(filter)) {
            queryString += " WHERE f.laengengrad IS NOT NULL";
        } else if ("no_gps".equalsIgnoreCase(filter)) {
            queryString += " WHERE f.laengengrad IS NULL";
        }
        queryString += " ORDER BY f.id DESC";

        return getEm().createQuery(queryString, FotoDatei.class)
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }

    public FotoDateiTO findByUniqueName(String name) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("name", name);

        FotoDatei result = super.findOneResult(FotoDatei.FIND_BY_UNIQUENAME, parameters);
        return result != null ? result.toFotoDateiTO() : null;
    }

    public int deleteAll() {
        return super.deleteAll();
    }

    public List<FotoDatei> findWithGPS() {
        return getEm().createQuery("SELECT f FROM FotoDatei f WHERE f.laengengrad IS NOT NULL", FotoDatei.class)
                .getResultList();
    }

    public List<FotoDatei> findWithoutGPS() {
        return getEm().createQuery("SELECT f FROM FotoDatei f WHERE f.laengengrad IS NULL", FotoDatei.class)
                .getResultList();
    }
}
