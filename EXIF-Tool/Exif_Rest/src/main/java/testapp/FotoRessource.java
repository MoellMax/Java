package testapp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONArray;
import org.json.JSONObject;

import de.swp.entity.FotoDateiTO;
import de.swp.entity.impl.FotoDatei;
import de.swp.service.IFotoService;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("foto")
@Stateless
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FotoRessource {

    @EJB
    IFotoService service;

    @OPTIONS
    @Path("{path: .*}")
    public Response options() {
        return Response.ok().build();
    }

    @POST
    @Path("/update-exif/{filename}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateExifByFilename(@PathParam("filename") String filename, FotoDateiTO updatedData) {
        try {
            File file = new File(BASE_DIR, filename);
            if (!file.exists()) {
                return Response.status(404)
                        .entity("{\"error\":\"Datei nicht gefunden\"}")
                        .build();
            }

            // Originalbild einlesen
            byte[] imageBytes = Files.readAllBytes(file.toPath());

            // EJB-Methode aufrufen, die die EXIF-Daten aktualisiert
            // FotoDateiTO -> FotoDatei konvertieren
            FotoDatei updatedFoto = updatedData.toFotoDatei();
            updatedFoto.setUniqueName(filename);
            byte[] updatedBytes = service.updateExif(imageBytes, updatedFoto);

            // Originaldatei überschreiben
            Files.write(file.toPath(), updatedBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            return Response.ok("{\"message\":\"Metadaten aktualisiert\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500)
                    .entity("{\"error\":\"Fehler beim Aktualisieren der Metadaten\"}")
                    .build();
        }
    }

    @GET
    @Path("/update-exif-by-name/{filename}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateExifByName(@PathParam("filename") String filename) {
        try {
            File file = new File(BASE_DIR, filename);
            if (!file.exists()) {
                return Response.status(404).entity("{\"error\":\"Datei nicht gefunden\"}").build();
            }

            try (FileInputStream fis = new FileInputStream(file)) {
                JSONObject exifData = service.extractMetadata(fis);

                try {
                    de.swp.entity.FotoDateiTO fotoTO = service.getFotoDatei(filename);
                    if (fotoTO != null) {
                        if (fotoTO.getName() != null && !fotoTO.getName().isEmpty()) {
                            exifData.put("DB_Name", fotoTO.getName());
                        }
                        if (fotoTO.getTitel() != null && !fotoTO.getTitel().isEmpty()) {
                            exifData.put("DB_Titel", fotoTO.getTitel());
                        }
                        if (fotoTO.getBetreff() != null && !fotoTO.getBetreff().isEmpty()) {
                            exifData.put("DB_Betreff", fotoTO.getBetreff());
                        }
                        if (fotoTO.getAutoren() != null && !fotoTO.getAutoren().isEmpty()) {
                            exifData.put("DB_Artist", fotoTO.getAutoren());
                        }
                        if (fotoTO.getCopyright() != null && !fotoTO.getCopyright().isEmpty()) {
                            exifData.put("DB_Copyright", fotoTO.getCopyright());
                        }
                        if (fotoTO.getAufnahmeDatum() != null) {
                            exifData.put("DB_Date", fotoTO.getAufnahmeDatum().toString());
                        }
                        if (fotoTO.getKameraHersteller() != null && !fotoTO.getKameraHersteller().isEmpty()) {
                            exifData.put("DB_CameraMake", fotoTO.getKameraHersteller());
                        }
                        if (fotoTO.getKameraModell() != null && !fotoTO.getKameraModell().isEmpty()) {
                            exifData.put("DB_CameraModel", fotoTO.getKameraModell());
                        }
                        if (fotoTO.getObjektivHersteller() != null && !fotoTO.getObjektivHersteller().isEmpty()) {
                            exifData.put("DB_LensMake", fotoTO.getObjektivHersteller());
                        }
                        if (fotoTO.getObjektivModell() != null && !fotoTO.getObjektivModell().isEmpty()) {
                            exifData.put("DB_LensModel", fotoTO.getObjektivModell());
                        }
                        if (fotoTO.getFotoDateiGeo() != null && !fotoTO.getFotoDateiGeo().isEmpty()) {
                            JSONObject geoJson = new JSONObject(fotoTO.getFotoDateiGeo());
                            if (geoJson.has("display_name")) {
                                exifData.put("Address", geoJson.getString("display_name"));
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Fehler beim Laden der Adresse: " + e.getMessage());
                }

                return Response.ok(exifData.toString()).build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).entity("{\"error\":\"Fehler beim Auslesen der Metadaten\"}").build();
        }
    }

    private static final String BASE_DIR = System.getProperty("user.home") + File.separator + "v2metatool1"
            + File.separator + "bildDateien";

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadImage(
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail) {
        try {
            System.out.println("UPLOAD START: " + fileDetail.getFileName());

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            uploadedInputStream.transferTo(buffer);
            byte[] bytes = buffer.toByteArray();

            System.out.println("BYTES RECEIVED: " + bytes.length);

            if (bytes.length == 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\":\"Leere Datei empfangen\"}")
                        .build();
            }

            Files.createDirectories(Paths.get(BASE_DIR));

            String ext = fileDetail.getFileName()
                    .substring(fileDetail.getFileName().lastIndexOf("."))
                    .toLowerCase();

            String uniqueName = UUID.randomUUID() + ext;

            java.nio.file.Path target = Paths.get(BASE_DIR, uniqueName);

            Files.write(
                    target,
                    bytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            service.saveImage(new ByteArrayInputStream(bytes), uniqueName, fileDetail.getFileName());
            return Response.ok("{\"filename\":\"" + uniqueName + "\"}").build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError()
                    .entity("{\"error\":\"Upload fehlgeschlagen\"}")
                    .build();
        }
    }

    @GET
    @Path("/images")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getImages(@jakarta.ws.rs.QueryParam("filter") String filter,
            @jakarta.ws.rs.QueryParam("page") @jakarta.ws.rs.DefaultValue("1") int page,
            @jakarta.ws.rs.QueryParam("pageSize") @jakarta.ws.rs.DefaultValue("27") int pageSize) {
        try {
            if (filter != null && !filter.isEmpty() && !filter.equals("all")) {
                return Response.ok(service.getImagesByFilter(filter, page, pageSize).toString()).build();
            }

            return Response.ok(service.getAllImages(page, pageSize).toString()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Fehler beim Laden der Bilder\"}").build();
        }
    }

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchImages(@jakarta.ws.rs.QueryParam("q") String query,
            @jakarta.ws.rs.QueryParam("filter") String filter,
            @jakarta.ws.rs.QueryParam("page") @jakarta.ws.rs.DefaultValue("1") int page,
            @jakarta.ws.rs.QueryParam("pageSize") @jakarta.ws.rs.DefaultValue("27") int pageSize) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return getImages(filter, page, pageSize);
            }
            JSONArray results = service.searchPhotosByMetadata(query, filter, page, pageSize);
            return Response.ok(results.toString()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Fehler bei der Suche\"}").build();
        }
    }

    @GET
    @Path("/image/{filename}")
    @Produces({ "image/jpeg", "image/png" })
    public Response getImage(@PathParam("filename") String filename) {
        try {
            File file = new File(BASE_DIR + File.separator + filename);

            if (!file.exists()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(file).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/delete/{filename}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteImage(@PathParam("filename") String filename) {
        try {
            File file = new File(BASE_DIR, filename);

            if (!file.exists()) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("{\"error\":\"Datei nicht gefunden\"}")
                        .build();
            }

            // 1️ Metadaten aus DB loeschen
            service.metaDatenAusDatenbankEntfernen(filename);

            // 2️ Datei loeschen
            if (file.delete()) {
                return Response.ok("{\"message\":\"Datei geloescht\"}").build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("{\"error\":\"Datei konnte nicht geloescht werden\"}")
                        .build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Fehler beim Loeschen\"}")
                    .build();
        }
    }

    @DELETE
    @Path("/delete-all")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAllImages() {
        try {
            service.deleteAllImages();
            return Response.ok("{\"message\":\"Alle Bilder geloescht\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Fehler beim Loeschen aller Bilder\"}")
                    .build();
        }
    }

    @GET
    @Path("/geocode")
    @Produces(MediaType.APPLICATION_JSON)
    public Response geocode(@jakarta.ws.rs.QueryParam("q") String query) {
        String result = service.geocode(query);
        if (result == null) {
            return Response.status(404).entity("{\"error\":\"Keine Ergebnisse gefunden\"}").build();
        }
        return Response.ok(result).build();
    }
}
