package net.buildtheearth.buildteamtools.modules.network.api;

import com.alpsbte.alpslib.utils.ChatHelper;
import net.buildtheearth.model.GeographicalCoordinate;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class PhotonAPI {
    private static final String BASE_URL = "https://photon.komoot.io/";

    public static @NotNull CompletableFuture<String> getAddressFromCoordinatesAsync(
            @NotNull GeographicalCoordinate coordinates
    ) {
        CompletableFuture<String> future = new CompletableFuture<>();

        String url = BASE_URL + "reverse?lat=" + coordinates.latitude()
                + "&lon=" + coordinates.longitude()
                + "&lang=en";

        API.getAsync(url, new API.ApiResponseCallback() {
            @Override
            public void onResponse(String response) {
                String address = formatAddressFromResponse(response);
                future.complete(address);
            }

            @Override
            public void onFailure(IOException e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private static String formatAddressFromResponse (String response) {
        JSONObject jsonObject = API.createJSONObject(response);

        ChatHelper.logDebug("Response from Photon: %s", jsonObject);

        JSONArray features = (JSONArray) jsonObject.get("features");

        if (features == null || features.isEmpty()) {
            ChatHelper.logError("No address data found for these coordinates.");
            return "";
        }

        JSONObject feature = (JSONObject) features.getFirst();
        JSONObject properties = (JSONObject) feature.get("properties");

        if (properties == null) {
            ChatHelper.logError("No properties found in Photon response.");
            return "";
        }

        String street = (String) properties.get("street");
        String city = (String) properties.get("city");
        String country = (String) properties.get("country");

        return String.join(", ",
                Stream.of(street, city, country)
                        .filter(value -> !value.isBlank())
                        .toList()
        );
    }

    public static @NotNull CompletableFuture<GeographicalCoordinate> getCoordinatesFromAddressAsync(String address){
        CompletableFuture<GeographicalCoordinate> future = new CompletableFuture<>();

        String url = BASE_URL + "api/?=q" + address+ "&lang=en";

        API.getAsync(url, new API.ApiResponseCallback() {
            @Override
            public void onResponse(String response) {
                GeographicalCoordinate coordinate = getGeoCoordinateFromResponse(response);
                future.complete(coordinate);
            }

            @Override
            public void onFailure(IOException e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private static GeographicalCoordinate getGeoCoordinateFromResponse(String response) {
        JSONObject jsonObject = API.createJSONObject(response);

        ChatHelper.logDebug("Response from Photon: %s", jsonObject);

        JSONArray features = (JSONArray) jsonObject.get("features");

        if (features == null || features.isEmpty()) {
            ChatHelper.logError("No address data found for these coordinates.");
            return new GeographicalCoordinate(0,0);
        }

        JSONObject feature = (JSONObject) features.getFirst();
        JSONObject geometry = (JSONObject) feature.get("geometry");

        if (geometry == null) {
            ChatHelper.logError("No geometry found in Photon response.");
            return new GeographicalCoordinate(0,0);
        }

        JSONArray coordinates = (JSONArray) geometry.get("coordinates");

        if (coordinates == null || coordinates.size() < 2) {
            ChatHelper.logError("Invalid coordinates in Photon response.");
            return null;
        }

        double longitude = ((Number) coordinates.get(0)).doubleValue();
        double latitude = ((Number) coordinates.get(1)).doubleValue();

        return new GeographicalCoordinate(latitude,longitude);
    }
}
