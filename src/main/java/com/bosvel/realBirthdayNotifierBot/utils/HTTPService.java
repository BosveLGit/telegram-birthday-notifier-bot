package com.bosvel.realBirthdayNotifierBot.utils;

import com.bosvel.realBirthdayNotifierBot.botConfig.TelegramBot;
import com.bosvel.realBirthdayNotifierBot.model.entity.ScheduledMessageStack;
import com.bosvel.realBirthdayNotifierBot.service.SocialNetworks;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class HTTPService {

    private static String geonamesUsername;
    private static String hereApiKey;
    private static String versionVKAPI;

    @Value("${geonames.token}")
    public void setGeonamesUsername(String token) {
        this.geonamesUsername = token;
    }

    @Value("${vk.versionAPI}")
    public void setVersionVKAPI(String versionVKAPI) {
        this.versionVKAPI = versionVKAPI;
    }

    @Value("${here.apikey}")
    public void setHereApiKey(String hereApiKey) {
        this.hereApiKey = hereApiKey;
    }

    public static TelegramBot.HTTPServicesResult getTimeZoneByCoordinates(double latitude, double longitude) {

        try {

            String urlString = "http://api.geonames.org/timezoneJSON" +
                    "?lat=" + latitude + "&lng=" + longitude + "&username=" + geonamesUsername;

            String response = sendGETRequest(urlString);

            if (response != null) {

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode responseJson = objectMapper.readTree(response);

                if(responseJson.has("timezoneId")) {
                    String timezoneId = responseJson.path("timezoneId").asText();
                    if (timezoneId != null && !timezoneId.isBlank()) {
                        return new TelegramBot.HTTPServicesResult(true, timezoneId, false);
                    }
                }

                if (responseJson.has("status")) {
                    String message = responseJson.path("message").asText();
                    int code = responseJson.path("value").asInt();

                    log.warn(String.format("Failed to get timezone (getTimeZoneByCoordinates) from coordinates: " +
                            "%s, %s. Message: %s, %s", latitude, longitude, code, message));

                }

            }

        } catch (Exception e) {
            log.error("An error occurred in HTTPService, getTimeZoneByCoordinates() ", e);
        }

        return new TelegramBot.HTTPServicesResult(false, null, false);
    }

    public static TelegramBot.HTTPServicesResult getTimeZoneBySearchLine(String searchLine) {

        try {

            String urlString = "https://geocode.search.hereapi.com/v1/geocode" +
                    "?apikey=" + hereApiKey + "&q=" + URLEncoder.encode(searchLine, "UTF-8") + "&types=city&show=tz&lang=RU";

            String response = sendGETRequest(urlString);

            if (response != null) {

                JSONObject jsonResponse = new JSONObject(response);

                if (jsonResponse.optJSONArray("items", null) == null) {
                    return new TelegramBot.HTTPServicesResult(false, null, false);
                }

                JSONArray items = jsonResponse.getJSONArray("items");
                if (!items.isEmpty()) {

                    JSONObject item = items.getJSONObject (0);
                    if (item.optJSONObject("timeZone", null) != null) {
                        JSONObject timeZone = item.getJSONObject ("timeZone");
                        if (timeZone.optString("name", null) != null) {
                            return new TelegramBot.HTTPServicesResult(true, timeZone.getString("name"), false);
                        }
                    }

                }
            }

        } catch (Exception e) {
            log.error("An error occurred in HTTPService, getTimeZoneBySearchLine() ", e);
        }


        return new TelegramBot.HTTPServicesResult(false, null, false);

    }

    public static TelegramBot.HTTPServicesResult getListUserBirthdaysFromVK(String accessToken, LocalDate birthday) {

        //List<ScheduledMessageStack>

        String url = "https://api.vk.com/method/users.search?" +
                "access_token=" + accessToken
                + "&v=" + versionVKAPI
                + "&birth_day=" + birthday.getDayOfMonth()
                + "&birth_month=" + birthday.getMonthValue()
                + "&from_list=friends&fields=bdate";

        try {
            String jsonResponse = sendGETRequest(url);

            if (jsonResponse != null) {

                JSONObject jsonObject = new JSONObject(jsonResponse);

                if(jsonObject.optJSONObject("error", null) != null) {

                    JSONObject error = jsonObject.getJSONObject("error");
                    int errorCode = error.optInt("error_code", -1);

                    if(errorCode == 5) {
                        return new TelegramBot.HTTPServicesResult(true, null, true);
                    } else if(errorCode >= 0) {
                        String error_msg = error.optString("error_msg", "");
                        log.error(String.format("VK API error, HTTPService, getListUserBirthdaysFromVK() - " +
                                "errorCode = %s; error_msg = %s", errorCode, error_msg));
                    }

                } else if(jsonObject.optJSONObject("response", null) != null) {

                    JSONObject response = jsonObject.getJSONObject("response");

                    int count = response.optInt("count", 0);
                    List<ScheduledMessageStack> resultArray = new ArrayList<>();

                    if (count > 0 && response.optJSONArray("items") != null) {

                        JSONArray items = response.getJSONArray("items");
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject user = items.getJSONObject(i);

                            if(user.optInt("id", -1) > 0
                                && user.optString("bdate", null) != null
                                && (user.optString("first_name", null) != null
                                    || user.optString("last_name", null) != null)) {

                                String bdate = user.optString("bdate"); // может быть null

                                int age = 0;
                                if (bdate.contains(".")) {
                                    String[] dateParts = bdate.split("\\.");
                                    if (dateParts.length == 3) {
                                        int year = Integer.parseInt(dateParts[2]);
                                        age = Period.between(LocalDate.of(year, Integer.parseInt(dateParts[1]), Integer.parseInt(dateParts[0])), birthday).getYears();
                                    }
                                }

                                resultArray.add(ScheduledMessageStack.builder()
                                        .name(user.getString("first_name") + " " + user.getString("last_name"))
                                        .birthday(birthday)
                                        .age(age)
                                        .socialNetwork(SocialNetworks.VK)
                                        .friendURL("https://vk.com/id"+user.getInt("id"))
                                        .build());

                            } // if JSON contain fields

                        } // cycle items

                    }

                    return new TelegramBot.HTTPServicesResult(true, resultArray, false);

                }

            }
        } catch (Exception e) {
            log.error("An error occurred in HTTPService, getListUserBirthdaysFromVK() ", e);
        }

        return new TelegramBot.HTTPServicesResult(false, null, false);

    }

    private static String sendGETRequest(String urlString) {

        try {

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                return response.toString();
            } else {
                log.warn(String.format("GET request failed with response code: %s - %s", responseCode, response.toString()));
                return null;
            }

        } catch (Exception e) {
            log.error("An error occurred in HTTPService, sendGetRequest() ", e.getMessage());
        }

        return null;

    }

}
