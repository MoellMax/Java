package de.swp.util;

import java.io.File;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.lang.GeoLocation;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FotoMetadatenUtil {

    public static Map<String, String> leseEXIF(File bildDatei) {
        Map<String, String> result = new HashMap<>();
        try {
            ImageMetadata metadata = Imaging.getMetadata(bildDatei);
            if (metadata instanceof JpegImageMetadata jpegMetadata) {
                for (var item : jpegMetadata.getItems()) {
                    result.put(item.toString(), item.toString()); // optional: bessere Keys vergeben
                }
            }
        } catch (Exception e) {
            result.put("Fehler", e.getMessage());
        }
        return result;
    }

    public static double[] leseGPS(File bildDatei) {
        try {
            Metadata md = ImageMetadataReader.readMetadata(bildDatei);
            GpsDirectory gpsDir = md.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDir != null) {
                GeoLocation loc = gpsDir.getGeoLocation();
                if (loc != null && !loc.isZero()) {
                    return new double[] { loc.getLatitude(), loc.getLongitude() };
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String reverseGeocode(double latitude, double longitude) {
        try {
            String url = String.format(
                    java.util.Locale.US,
                    "https://nominatim.openstreetmap.org/reverse?lat=%f&lon=%f&format=json&addressdetails=1&zoom=18&accept-language=de",
                    latitude, longitude);

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("User-Agent", "MetaTool/1.0")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }

            return response.body();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String geocode(String address) {
        try {
            String encodedAddress = java.net.URLEncoder.encode(address, java.nio.charset.StandardCharsets.UTF_8);
            String url = String.format(
                    java.util.Locale.US,
                    "https://nominatim.openstreetmap.org/search?q=%s&format=jsonv2&addressdetails=1&limit=1",
                    encodedAddress);

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("User-Agent", "MetaTool/1.0")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }

            return response.body();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    } 

    public static LocalDate parseExifDate(String exifDate) {
        // Beispiel: "2025:11:29 15:30:00"
        try {
            String[] parts = exifDate.split(" ")[0].split(":");
            return LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (Exception e) {
            return null;
        }
    }
}
