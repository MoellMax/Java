package de.swp.entity.impl;

import java.util.*;

import de.swp.converter.*;
import de.swp.entity.FotoDateiTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.io.Serializable;

import jakarta.persistence.*;

@Entity
@NamedQueries({
        @NamedQuery(name = "FotoDatei.findByMetadatenJson", query = "SELECT f FROM FotoDatei f WHERE LOWER(f.metadatenJson) LIKE LOWER(:query)"),
        @NamedQuery(name = "FotoDatei.findByUniqueName", query = "SELECT f FROM FotoDatei f WHERE f.uniqueName = :name"),
        @NamedQuery(name = "FotoDatei.findByID", query = "SELECT f FROM FotoDatei f WHERE f.id = :id"),
        @NamedQuery(name = "FotoDatei.findByHash", query = "SELECT f FROM FotoDatei f WHERE f.hash_identifikator = :hash_identifikator"),
        @NamedQuery(name = "FotoDatei.findByFilename", query = "SELECT f FROM FotoDatei f WHERE f.name = :name"),
        @NamedQuery(name = "FotoDatei.findByMetadateiKeyValue", query = "SELECT f FROM FotoDatei f JOIN f.metadaten m WHERE KEY(m) = :key AND VALUE(m) = :value")
})
@Table(name = "fotodatei")
public class FotoDatei implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String FIND_BY_UNIQUENAME = "FotoDatei.findByUniqueName";
    public static final String FIND_BY_HASH = "FotoDatei.findByHash";
    public static final String FIND_BY_ID = "FotoDatei.findByID";
    public static final String FIND_BY_NAME = "FotoDatei.findByFilename";
    public static final String FIND_BY_METADATEN_KEYVALUE = "FotoDatei.findByMetadateiKeyValue";
    public static final String FIND_BY_METADATEN_JSON = "FotoDatei.findByMetadatenJson";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fotodatei_id")
    private Long id;

    @Column(name = "hash_identifikator", columnDefinition = "TEXT")
    private String hash_identifikator;

    @Column(name = "uniqueName", columnDefinition = "TEXT")
    private String uniqueName;

    @Column(name = "name", columnDefinition = "TEXT")
    private String name;

    @Column(name = "titel", columnDefinition = "TEXT")
    private String titel;

    @Column(name = "betreff", columnDefinition = "TEXT")
    private String betreff;

    @Column(name = "autoren", columnDefinition = "TEXT")
    private String autoren;

    @Column(name = "aufnahmeDatum")
    @Convert(converter = LocalDateConverter.class)
    private LocalDate aufnahmeDatum;

    @Column(name = "copyright", columnDefinition = "TEXT")
    private String copyright;

    @Column(name = "kameraHersteller", columnDefinition = "TEXT")
    private String kameraHersteller;

    @Column(name = "kameraModell", columnDefinition = "TEXT")
    private String kameraModell;

    @Column(name = "objektivHersteller", columnDefinition = "TEXT")
    private String objektivHersteller;

    @Column(name = "objektivModell", columnDefinition = "TEXT")
    private String objektivModell;

    @Column(name = "laengengrad")
    private Double laengengrad;

    @Column(name = "breitengrad")
    private Double breitengrad;

    @Column(name = "geaendert_Am")
    private LocalDateTime geaendert_Am;

    @Column(name = "jsonmetadaten", columnDefinition = "TEXT")
    private String jsonMetadaten;

    @Column(name = "fotodateigeo", columnDefinition = "TEXT")
    private String fotoDateiGeo;

    @Column(name = "metadaten_json", columnDefinition = "TEXT")
    private String metadatenJson;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "fotodatei_metadaten", joinColumns = @JoinColumn(name = "fotodatei_id"))
    @MapKeyColumn(name = "metadaten_key", columnDefinition = "TEXT")
    @Column(name = "metadaten_wert", columnDefinition = "TEXT")
    private Map<String, String> metadaten = new HashMap<>();

    public FotoDatei() {
    }

    public FotoDatei(String name, String titel, String betreff, String autoren, LocalDate aufnahmeDatum,
            String copyright,
            String kameraHersteller, String kameraModell, String objektivHersteller, String objektivModell,
            Double laengengrad, Double breitengrad, LocalDateTime geaendert_Am, Map<String, String> metadaten,
            String jsonMetadaten, String fotoDateiGeo, String metadatenJson) {
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
        this.laengengrad = laengengrad;
        this.breitengrad = breitengrad;
        this.geaendert_Am = geaendert_Am;
        this.metadaten = metadaten;
        this.jsonMetadaten = jsonMetadaten;
        this.fotoDateiGeo = fotoDateiGeo;
        this.metadatenJson = metadatenJson;
    }

    public FotoDatei(String hash_identifikator, String name, String titel, String betreff, String autoren,
            LocalDate aufnahmeDatum, String copyright, String kameraHersteller, String kameraModell,
            String objektivHersteller, String objektivModell, Double laengengrad, Double breitengrad,
            LocalDateTime geaendert_Am, Map<String, String> metadaten, String jsonMetadaten, String fotoDateiGeo,
            String metadatenJson) {
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
        this.laengengrad = laengengrad;
        this.breitengrad = breitengrad;
        this.geaendert_Am = geaendert_Am;
        this.metadaten = metadaten;
        this.jsonMetadaten = jsonMetadaten;
        this.fotoDateiGeo = fotoDateiGeo;
        this.metadatenJson = metadatenJson;
    }

    public FotoDatei(Long id, String hash_identifikator, String name, String titel, String betreff, String autoren,
            LocalDate aufnahmeDatum, String copyright, String kameraHersteller, String kameraModell,
            String objektivHersteller, String objektivModell, Double laengengrad, Double breitengrad,
            LocalDateTime geaendert_Am, Map<String, String> metadaten, String jsonMetadaten, String fotoDateiGeo,
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
        this.laengengrad = laengengrad;
        this.breitengrad = breitengrad;
        this.geaendert_Am = geaendert_Am;
        this.metadaten = metadaten;
        this.jsonMetadaten = jsonMetadaten;
        this.fotoDateiGeo = fotoDateiGeo;
        this.metadatenJson = metadatenJson;
    }

    public FotoDateiTO toFotoDateiTO() {
        FotoDateiTO to = new FotoDateiTO();

        to.setId(this.id);
        to.setHash_identifikator(this.hash_identifikator);
        to.setUniqueName(this.uniqueName);
        to.setName(this.name);
        to.setTitel(this.titel);
        to.setBetreff(this.betreff);
        to.setAutoren(this.autoren);
        to.setAufnahmeDatum(this.aufnahmeDatum);
        to.setCopyright(this.copyright);
        to.setKameraHersteller(this.kameraHersteller);
        to.setKameraModell(this.kameraModell);
        to.setObjektivHersteller(this.objektivHersteller);
        to.setObjektivModell(this.objektivModell);

        to.setLongitude(this.laengengrad != null ? java.math.BigDecimal.valueOf(this.laengengrad) : null);
        to.setLatitude(this.breitengrad != null ? java.math.BigDecimal.valueOf(this.breitengrad) : null);

        to.setGeaendert_Am(this.geaendert_Am);
        to.setMetadaten(this.metadaten != null ? new java.util.HashMap<>(this.metadaten) : new java.util.HashMap<>());
        to.setJsonMetadaten(this.jsonMetadaten);
        to.setFotoDateiGeo(this.fotoDateiGeo);
        to.setMetadatenJson(this.metadatenJson);

        return to;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

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

    public Double getLaengengrad() {
        return laengengrad;
    }

    public void setLaengengrad(Double laengengrad) {
        this.laengengrad = laengengrad;
    }

    public Double getBreitengrad() {
        return breitengrad;
    }

    public void setBreitengrad(Double breitengrad) {
        this.breitengrad = breitengrad;
    }

    public LocalDateTime getGeaendert_Am() {
        return geaendert_Am;
    }

    public void setGeaendert_Am(LocalDateTime geaendert_Am) {
        this.geaendert_Am = geaendert_Am;
    }

    public Map<String, String> getMetadaten() {
        return metadaten;
    }

    public void setMetadaten(Map<String, String> metadaten) {
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

    @Override
    public String toString() {
        return "FotoDatei {" +
                "id=" + id +
                ", hash_identifikator='" + hash_identifikator + '\'' +
                ", name='" + name + '\'' +
                ", titel='" + titel + '\'' +
                ", betreff='" + betreff + '\'' +
                ", autoren='" + autoren + '\'' +
                ", aufnahmeDatum=" + aufnahmeDatum +
                ", copyright='" + copyright + '\'' +
                ", kameraHersteller='" + kameraHersteller + '\'' +
                ", kameraModell='" + kameraModell + '\'' +
                ", objektivHersteller='" + objektivHersteller + '\'' +
                ", objektivModell='" + objektivModell + '\'' +
                ", laengengrad=" + laengengrad +
                ", breitengrad=" + breitengrad +
                ", geaendert_Am=" + geaendert_Am +
                ", metadaten=" + metadaten +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof FotoDatei other))
            return false;
        // Gleichheit basiert auf hash_identifikator, falls vorhanden
        return hash_identifikator != null && hash_identifikator.equals(other.hash_identifikator);
    }

    @Override
    public int hashCode() {
        return hash_identifikator != null ? hash_identifikator.hashCode() : 0;
    }

    @PreUpdate
    public void onUpdate() {
        this.geaendert_Am = LocalDateTime.now();
    }

}
