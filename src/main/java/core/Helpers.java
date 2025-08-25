package core;

import io.qameta.allure.Attachment;
import io.restassured.response.Response;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class Helpers {

    public static String generateRegistrationEmail() {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        return "reg" + timestamp + "@yopmail.com";
    }


    @Attachment(value = "{name}", type = "application/json")
    public static byte[] attachJson(String name, String json) {
        return json == null ? new byte[0] : json.getBytes(StandardCharsets.UTF_8);
    }


    @Attachment(value = "{name}", type = "text/plain")
    public static byte[] attachText(String name, String txt) {
        return txt == null ? new byte[0] : txt.getBytes(StandardCharsets.UTF_8);
    }


    public static String extractMessage(Response resp) {
        try {
            String m = resp.jsonPath().getString("message");
            return (m == null || m.isBlank()) ? "<no message>" : m;
        } catch (Exception e) {
            return "<unparsable body>";
        }
    }
}