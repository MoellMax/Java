package de.swp.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;

import de.swp.entity.impl.FotoDatei;

public class FotoDateiTO {

    private Long id;

    private String hash_identifikator;

    private String uniqueName;

    private String name;

    private String titel;

    private String betreff;

    private String autoren;

    private LocalDate aufnahmeDatum;

    private String copyright;

    private String kameraHersteller;

    private String kameraModell;

    private String objektivHersteller;

    private String objektivModell;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private LocalDateTime geaendert_Am;

    private HashMap<String, String> metadaten;

    private String jsonMetadaten;

    private String fotoDateiGeo;

    private String metadatenJson;

    public FotoDateiTO() {
    }

    public FotoDateiTO(Long id, String hash_identifikator, String name, String titel, String betreff,
            String autoren, LocalDate aufnahmeDatum, String copyright, String kameraHersteller,
            String kameraModell, String objektivHersteller, String objektivModell,
            BigDecimal latitude, BigDecimal longitude, LocalDateTime geaendert_Am,
            HashMap<String, String> metadaten, String jsonMetadaten, String fotoDateiGeo,
            String metadatenJson) {
        this.id = id;
        this.hash_identifikator = hash_identifikator;
        this.name = name;
        this.titel = titel;
        this.betreff = betreff;
        this.autoren = autoren;
        this.aufnahmeDatum = aufnahmeDatum;
        this.copyright = copyright;
        this.kameraHersteller = kameraHersteller;
        this.kameraModell = kameraModell;
        this.objektivHersteller = objektivHersteller;
        this.objektivModell = objektivModell;
        this.latitude = latitude;
        this.longitude = longitude;
        this.geaendert_Am = geaendert_Am;
        this.metadaten = metadaten;
        this.jsonMetadaten = jsonMetadaten;
        this.fotoDateiGeo = fotoDateiGeo;
        this.metadatenJson = metadatenJson;
    }

    // Getter & Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHash_identifikator() {
        return hash_identifikator;
    }

    public void setHash_identifikator(String hash_identifikator) {
        this.hash_identifikator = hash_identifikator;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public String getBetreff() {
        return betreff;
    }

    public void setBetreff(String betreff) {
        this.betreff = betreff;
    }

    public String getAutoren() {
        return autoren;
    }

    public void setAutoren(String autoren) {
        this.autoren = autoren;
    }

    public LocalDate getAufnahmeDatum() {
        return aufnahmeDatum;
    }

    public void setAufnahmeDatum(LocalDate aufnahmeDatum) {
        this.aufnahmeDatum = aufnahmeDatum;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public String getKameraHersteller() {
        return kameraHersteller;
    }

    public void setKameraHersteller(String kameraHersteller) {
        this.kameraHersteller = kameraHersteller;
    }

    public String getKameraModell() {
        return kameraModell;
    }

    public void setKameraModell(String kameraModell) {
        this.kameraModell = kameraModell;
    }

    public String getObjektivHersteller() {
        return objektivHersteller;
    }

    public void setObjektivHersteller(String objektivHersteller) {
        this.objektivHersteller = objektivHersteller;
    }

    public String getObjektivModell() {
        return objektivModell;
    }

    public void setObjektivModell(String objektivModell) {
        this.objektivModell = objektivModell;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getGeaendert_Am() {
        return geaendert_Am;
    }

    public void setGeaendert_Am(LocalDateTime geaendert_Am) {
        this.geaendert_Am = geaendert_Am;
    }

    public HashMap<String, String> getMetadaten() {
        return metadaten;
    }

    public void setMetadaten(HashMap<String, String> metadaten) {
        this.metadaten = metadaten;
    }

    public String getJsonMetadaten() {
        return jsonMetadaten;
    }

    public void setJsonMetadaten(String jsonMetadaten) {
        this.jsonMetadaten = jsonMetadaten;
    }

    public String getFotoDateiGeo() {
        return fotoDateiGeo;
    }

    public void setFotoDateiGeo(String fotoDateiGeo) {
        this.fotoDateiGeo = fotoDateiGeo;
    }

    public String getMetadatenJson() {
        return metadatenJson;
    }

    public void setMetadatenJson(String metadatenJson) {
        this.metadatenJson = metadatenJson;
    }

    // Konvertierung zur Entity
    public FotoDatei toFotoDatei() {
        FotoDatei entity = new FotoDatei();
        entity.setId(this.id);
        entity.setHash_identifikator(this.hash_identifikator);
        entity.setUniqueName(this.uniqueName);
        entity.setName(this.name);
        entity.setTitel(this.titel);
        entity.setBetreff(this.betreff);
        entity.setAutoren(this.autoren);
        entity.setAufnahmeDatum(this.aufnahmeDatum);
        entity.setCopyright(this.copyright);
        entity.setKameraHersteller(this.kameraHersteller);
        entity.setKameraModell(this.kameraModell);
        entity.setObjektivHersteller(this.objektivHersteller);
        entity.setObjektivModell(this.objektivModell);
        entity.setLaengengrad(this.longitude != null ? this.longitude.doubleValue() : null);
        entity.setBreitengrad(this.latitude != null ? this.latitude.doubleValue() : null);
        entity.setGeaendert_Am(this.geaendert_Am);
        entity.setMetadaten(this.metadaten != null ? new HashMap<>(this.metadaten) : new HashMap<>());
        entity.setJsonMetadaten(this.jsonMetadaten);
        entity.setFotoDateiGeo(this.fotoDateiGeo);
        entity.setMetadatenJson(this.metadatenJson);
        return entity;
    }

    @Override
    public String toString() {
        return "FotoDateiTO [id=" + id
                + ", hash_identifikator=" + hash_identifikator
                + ", name=" + name
                + ", titel=" + titel
                + ", betreff=" + betreff
                + ", autoren=" + autoren
                + ", aufnahmeDatum=" + aufnahmeDatum
                + ", copyright=" + copyright
                + ", kameraHersteller=" + kameraHersteller
                + ", kameraModell=" + kameraModell
                + ", objektivHersteller=" + objektivHersteller
                + ", objektivModell=" + objektivModell
                + ", latitude=" + latitude
                + ", longitude=" + longitude
                + ", geaendert_Am=" + geaendert_Am
                + ", metadaten=" + metadaten + "]";
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }
}
