package com.bosvel.realBirthdayNotifierBot.restController;

import com.bosvel.realBirthdayNotifierBot.model.dao.VKAuthDataDAO;
import com.bosvel.realBirthdayNotifierBot.model.entity.VKAuthData;
import com.bosvel.realBirthdayNotifierBot.service.CryptoTool;
import com.bosvel.realBirthdayNotifierBot.service.SendBotMessageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.bosvel.realBirthdayNotifierBot.command.commandConfig.CommandName.VK_INTEGRATION;

@Slf4j
@RequestMapping("/")
@RestController
public class IntegrationController {

    @Autowired
    private Environment environment;

    private final VKAuthDataDAO vkAuthDataDAO;
    private final CryptoTool cryptoTool;
    private final SendBotMessageService sendBotMessageService;

    @Autowired
    public IntegrationController(VKAuthDataDAO vkAuthDataDAO, CryptoTool cryptoTool, SendBotMessageService sendBotMessageService) {
        this.vkAuthDataDAO = vkAuthDataDAO;
        this.cryptoTool = cryptoTool;
        this.sendBotMessageService = sendBotMessageService;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/goToVKAuth")
    public void redirectToAuthVK(@RequestParam("id") String hashId, HttpServletResponse response) {

        if(hashId.isBlank() || cryptoTool.idOf(hashId) == null) {
            return;
        }

        String uuid = UUID.randomUUID().toString();

        String query = String.format("uuid=%s" +
                        "&app_id=%s" +
                        "&response_type=silent_token" +
                        "&redirect_uri=%s" +
                        "&redirect_state=%s"
                , uuid, environment.getProperty("vk.appID"), environment.getProperty("vk.URLServiceAuth"), hashId);

        try {
            response.sendRedirect("https://id.vk.com/auth?" + query);
        } catch (IOException e) {
            log.error("An error occurred in IntegrationController, redirectToAuthVK() ", e);
        }

    }

    @RequestMapping(method = RequestMethod.GET, value = "/vkAuth")
    public void getAndSaveVKAccessToken(HttpServletRequest request, HttpServletResponse response, HttpSession session) {

        String payload = request.getParameter("payload");
        String state = request.getParameter("state");

        if (payload == null || state == null) {
            log.error("Missing payload or state parameter");
            return;
        }

        JSONObject authStatus = new JSONObject();

        try {

            String decodedPayload = URLDecoder.decode(payload, StandardCharsets.UTF_8);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode payloadJson = objectMapper.readTree(decodedPayload);

            String paramSilentToken = payloadJson.path("token").asText();
            String paramUUID = payloadJson.path("uuid").asText();

            String userId = cryptoTool.idOf(state).toString();

            if (userId == null) {
                log.error("Invalid state received");
                return;
            }

            String token = generateUniqueToken();
            storeTokenUserIdMapping(session, token, userId);

            try {
                response.sendRedirect("/processingVKAuth?token=" + token);
            } catch (IOException e) {
                log.error("An error occurred in IntegrationController, getAndSaveVKAccessToken() ", e);
            }

            CompletableFuture.runAsync(() -> {
                try {

                    HttpClient httpClient = HttpClients.createDefault();
                    HttpPost httpPost = new HttpPost("https://api.vk.com/method/auth.exchangeSilentAuthToken");

                    List<BasicNameValuePair> params = new ArrayList<>();
                    params.add(new BasicNameValuePair("v", environment.getProperty("vk.versionAPI")));
                    params.add(new BasicNameValuePair("token", paramSilentToken));
                    params.add(new BasicNameValuePair("access_token", environment.getProperty("vk.appSerivceToken")));
                    params.add(new BasicNameValuePair("uuid", paramUUID));

                    try {

                        httpPost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
                        HttpResponse httpResponse = httpClient.execute(httpPost);
                        HttpEntity entity = httpResponse.getEntity();
                        if (entity == null) {

                            log.error("An error occurred in IntegrationController, getAndSaveVKAccessToken(): " +
                                    "VKontakte returned an incorrect answer (invalid JSON 1)");

                            session.setAttribute("vkAuthStatus_" + userId,
                                    getResponseToAuthVK(2, "ВКонтакте вернул некорректный ответ!").toString());
                            return;
                        }

                        BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8));
                        StringBuilder responseStringBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseStringBuilder.append(line);
                        }

                        String jsonString = responseStringBuilder.toString();
                        JSONObject jsonResponse = new JSONObject(jsonString);

                        if(jsonResponse.isEmpty()) {

                            log.error("An error occurred in IntegrationController, getAndSaveVKAccessToken(): " +
                                    "VKontakte returned an incorrect answer (invalid JSON 2)");

                            session.setAttribute("vkAuthStatus_" + userId,
                                    getResponseToAuthVK(2, "ВКонтакте вернул некорректный ответ!").toString());
                            return;
                        }

                        JSONObject responseObj = jsonResponse.optJSONObject("response", null);

                        if (responseObj == null) {

                            if(jsonResponse.optJSONObject("error", null) != null) {
                                responseObj = jsonResponse.getJSONObject("error");
                                int errorCode = responseObj.optInt("error_code", 0);
                                if(errorCode == 104) {
                                    session.setAttribute("vkAuthStatus_" + userId,
                                            getResponseToAuthVK(2, "Токен уже был использован!").toString());
                                    session.removeAttribute("vkAuthTokenExpiry_" + token);
                                    return;
                                }
                            }

                            log.error("An error occurred in IntegrationController, getAndSaveVKAccessToken(): " +
                                    "VKontakte returned an incorrect answer: " + responseObj);

                            session.setAttribute("vkAuthStatus_" + userId,
                                    getResponseToAuthVK(2, "ВКонтакте вернул неверный ответ!").toString());
                            return;
                        }

                        String accessToken = responseObj.optString("access_token", "");
                        String accessTokenId = responseObj.optString("access_token_id", "");
                        Long vkUserId = responseObj.optLong("user_id", 0);
                        int expiresIn = responseObj.optInt("expires_in", 0);

                        if(!accessToken.isBlank()) {

                            Long UserIdLong = Long.parseLong(userId);

                            VKAuthData vkAuthData = VKAuthData.builder()
                                    .telegramUserId(UserIdLong)
                                    .vkUserId(vkUserId)
                                    .accessToken(accessToken)
                                    .accessTokenId(accessTokenId)
                                    .expireDate(LocalDate.now().plusDays((expiresIn / (60*60*24))))
                                    .build();

                            vkAuthDataDAO.save(vkAuthData);

                            session.setAttribute("vkAuthStatus_" + userId,
                                    getResponseToAuthVK(1, "Все прошло успешно!").toString());
                            sendBotMessageService.updateStatusIntegration(UserIdLong, VK_INTEGRATION.getCommandName());
                            return;

                        }

                    } catch (IOException e) {
                        log.error("An error occurred in IntegrationController, getAndSaveVKAccessToken() ", e);
                        session.setAttribute("vkAuthStatus_" + userId,
                                getResponseToAuthVK(2, "Что-то пошло не так :(<br>(1)").toString());
                        return;
                    }

                    session.setAttribute("vkAuthStatus_" + userId,
                            getResponseToAuthVK(2, "Что-то пошло не так :(<br>(2)").toString());

                } catch (Exception e) {
                    log.error("Error processing VK auth", e);
                    session.setAttribute("vkAuthStatus_" + userId,
                            getResponseToAuthVK(2, "Что-то пошло не так :(<br>(3)").toString());
                }
            });

        } catch (Exception e) {
            log.error("Error decoding or parsing payload", e);
        }

    }

    @RequestMapping(method = RequestMethod.GET, value = "/processingVKAuth")
    public String processingVKAuth(@RequestParam String token, HttpSession session) {
        String userId = getUserIdFromToken(session, token);

        if (userId == null) {
            return generateHTMLProcessingVKAuth(getResponseToAuthVK(2, "Неверный токен!"));
        }

        session.setAttribute("vkAuthToken", token);

        String statusJson = (String) session.getAttribute("vkAuthStatus_" + userId);
        JSONObject authStatus = statusJson != null ? new JSONObject(statusJson) : getResponseToAuthVK(0, "Обрабатываем...");

        return generateHTMLProcessingVKAuth(authStatus);

    }

    private String generateHTMLProcessingVKAuth(JSONObject authStatus) {

        String imageUrl = "";
        int status = authStatus.getInt("status");
        String message = authStatus.getString("message");

        if (status == 0) {
            imageUrl = "/images/process.svg";
        } else if (status == 1) {
            imageUrl = "/images/success.svg";
        } else if (status == 2) {
            imageUrl = "/images/error.svg";
        }

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <title>Processing VK Auth</title>" +
                "    <style>" +
                "        body { display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }" +
                "        .container { text-align: center; }" +
                "        .status-image { width: 100px; height: 100px; }" +
                "        #statusImage { margin-bottom: 20px; }" +
                "    </style>" +
                "    <script src='https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js'></script>" +
                "    <script>" +
                "        $(document).ready(function() {" +
                "            var status = " + status + ";" +
                "            var message = '" + message + "';" +
                "            var imageUrl = '" + imageUrl + "';" +
                "            $('#statusImage').attr('src', imageUrl);" +
                "            $('#message').text(message);" +
                "            if (status === 0) {" +
                "                var intervalId = setInterval(function() {" +
                "                    $.get('/vkAuthStatus', function(data) {" +
                "                        var statusData = JSON.parse(data);" +
                "                        var newStatus = statusData.status;" +
                "                        var newMessage = statusData.message;" +
                "                        var newImageUrl = '';" +
                "                        if (newStatus === 0) {" +
                "                            newImageUrl = '/images/process.svg';" +
                "                        } else if (newStatus === 1) {" +
                "                            newImageUrl = '/images/success.svg';" +
                "                            showRedirectTimer();" +
                "                        } else if (newStatus === 2) {" +
                "                            newImageUrl = '/images/error.svg';" +
                "                        }" +
                "                        $('#statusImage').attr('src', newImageUrl);" +
                "                        $('#message').text(newMessage);" +
                "                        if (newStatus === 1 || newStatus === 2) {" +
                "                            clearInterval(intervalId);" +
                "                        }" +
                "                    });" +
                "                }, 1000);" +
                "            } else if (status === 1) {" +
                "                showRedirectTimer();" +
                "            }" +
                "        });" +
                "        function showRedirectTimer() {" +
                "            var countdown = 3;" +
                "            var countdownInterval = setInterval(function() {" +
                "                $('#redirectMessage').text('Возвращемся в бот через ' + countdown + '');" +
                "                if (countdown === 0) {" +
                "                    clearInterval(countdownInterval);" +
                "                    window.location.href = 'https://t.me/"+environment.getProperty("bot.name")+"';" +
                "                }" +
                "                countdown--;" +
                "            }, 1000);" +
                "        }" +
                "    </script>" +
                "</head>" +
                "<body>" +
                "    <div class='container'>" +
                "        <img id='statusImage' class='status-image' src='" + imageUrl + "' alt='status'>" +
                "        <h2 id='message'>" + message + "</h2>" +
                "        <h3 id='redirectMessage'></h3>" +
                "    </div>" +
                "</body>" +
                "</html>";

    }

    @RequestMapping(method = RequestMethod.GET, value = "/vkAuthStatus")
    @ResponseBody
    public String vkAuthStatus(HttpSession session) {

        String token = (String) session.getAttribute("vkAuthToken");
        if(token == null) {
            return "";
        }
        String userId = getUserIdFromToken(session, token);
        if (userId == null) {
            JSONObject authStatus = new JSONObject();
            authStatus.put("status", 2);
            authStatus.put("message", "Неверный токен!");
            return authStatus.toString();
        }

        return (String) session.getAttribute("vkAuthStatus_" + userId);
    }

    @RequestMapping("favicon.ico")
    @ResponseBody
    void returnNoFavicon() {
        // Ничего не делаем
    }

    private String generateUniqueToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void storeTokenUserIdMapping(HttpSession session, String token, String userId) {
        session.setAttribute("vkAuthToken", token);
        session.setAttribute("vkAuthUserId_" + token, userId);
        session.setAttribute("vkAuthTokenExpiry_" + token, System.currentTimeMillis() + 180000);
    }

    private String getUserIdFromToken(HttpSession session, String token) {
        Long expiry = (Long) session.getAttribute("vkAuthTokenExpiry_" + token);
        if (expiry == null || expiry < System.currentTimeMillis()) {
            return null;
        }
        return (String) session.getAttribute("vkAuthUserId_" + token);
    }

    private JSONObject getResponseToAuthVK(int status, String message) {
        JSONObject authStatus = new JSONObject();
        authStatus.put("status", status);
        authStatus.put("message", message);
        return authStatus;
    }

}
