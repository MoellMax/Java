package testapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.swp.service.IFotoService;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
public class FotoRessourceTest {

    @Mock
    private IFotoService service;

    @InjectMocks
    private FotoRessource fotoRessource;

    @Test
    void testGetImages_All() throws Exception {
        JSONArray mockArray = new JSONArray();
        mockArray.put("image1.jpg");
        when(service.getAllImages(anyInt(), anyInt())).thenReturn(mockArray);

        Response response = fotoRessource.getImages("all", 1, 27);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(mockArray.toString(), response.getEntity());
        verify(service).getAllImages(1, 27);
    }

    @Test
    void testGetImages_WithFilter() throws Exception {
        JSONArray mockArray = new JSONArray();
        mockArray.put("filtered_image.jpg");
        when(service.getImagesByFilter(anyString(), anyInt(), anyInt())).thenReturn(mockArray);

        Response response = fotoRessource.getImages("gps", 1, 27);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(mockArray.toString(), response.getEntity());
        verify(service).getImagesByFilter("gps", 1, 27);
    }

    @Test
    void testSearchImages() throws Exception {
        JSONArray mockArray = new JSONArray();
        mockArray.put("search_result.jpg");
        when(service.searchPhotosByMetadata(anyString(), anyString(), anyInt(), anyInt())).thenReturn(mockArray);

        Response response = fotoRessource.searchImages("test", "all", 1, 27);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(mockArray.toString(), response.getEntity());
        verify(service).searchPhotosByMetadata("test", "all", 1, 27);
    }

    @Test
    void testDeleteAllImages() throws Exception {
        Response response = fotoRessource.deleteAllImages();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        verify(service).deleteAllImages();
    }

    @Test
    void testGeocode() throws Exception {
        String mockResult = "{\"lat\": 50.0, \"lon\": 8.0}";
        when(service.geocode(anyString())).thenReturn(mockResult);

        Response response = fotoRessource.geocode("Frankfurt");

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(mockResult, response.getEntity());
        verify(service).geocode("Frankfurt");
    }

    @Test
    void testOptions() {
        Response response = fotoRessource.options();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }
}
