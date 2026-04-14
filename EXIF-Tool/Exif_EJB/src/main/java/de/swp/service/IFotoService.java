package de.swp.service;

import de.swp.entity.impl.FotoDatei;

import java.io.File;
import java.io.InputStream;

import org.json.JSONArray;
import org.json.JSONObject;

public interface IFotoService {

    // /**
    // * Prüft, ob die Bilddatei existiert und vom erlaubten Typ ist (JPEG/PNG)
    // *
    // * @param bildFile Die zu prüfende Bilddatei
    // * @throws IllegalArgumentException wenn die Datei nicht existiert oder
    // ungültig ist
    // */
    // void pruefeDatei(File bildFile);

    /**
     * Liest Metadaten aus der Bilddatei und erstellt ein FotoDatei-Objekt
     *
     * @param bildFile Die Bilddatei
     * @param hash     Eindeutiger Hash (z.B. SHA512)
     * @return FotoDatei-Entity mit gefüllten Metadaten
     * @throws Exception bei Problemen beim Auslesen
     */
    FotoDatei auslesen(File bildFile, String hash) throws Exception;

    /**
     * Speichert oder aktualisiert ein FotoDatei-Objekt in der Datenbank
     *
     * @param foto Das zu speichernde FotoDatei-Objekt
     * @return das gespeicherte FotoDatei-Objekt
     */
    // FotoDatei speichern(FotoDatei foto);

    /**
     * Löscht ein FotoDatei-Objekt aus der Datenbank
     *
     * @param foto Das zu löschende FotoDatei-Objekt
     */

    public byte[] updateExif(byte[] imageBytes, FotoDatei data) throws Exception;

    public JSONObject extractMetadata(InputStream imageStream) throws Exception;

    public void saveImage(InputStream inputStream, String uniqueName, String originalFilename) throws Exception;

    public JSONArray getAllImages(int page, int pageSize) throws Exception;

    public void metaDatenAusDatenbankEntfernen(String filename);

    public void deleteAllImages() throws Exception;

    /**
     * Asynchronously performs reverse geocoding for an image and updates the
     * database.
     * This method runs in a background thread and does not block the upload
     * process.
     * 
     * @param fotoId    ID of the foto to update
     * @param latitude  Latitude coordinate
     * @param longitude Longitude coordinate
     */
    public void reverseGeocodeAsync(Long fotoId, double latitude, double longitude);

    public JSONArray searchPhotosByMetadata(String query, String filter, int page, int pageSize);

    public de.swp.entity.FotoDateiTO getFotoDatei(String uniqueName);

    public String geocode(String address);

    public JSONArray getImagesByFilter(String filter, int page, int pageSize);
}
