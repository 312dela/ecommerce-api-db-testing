package core;

import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.qameta.allure.model.Status;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import utils.JsonDataLoader;

import org.testng.annotations.*;

import core.db.MongoHelper;

import java.util.Map;

public class BaseTest {
    protected static RequestSpecification spec;
    protected static MongoHelper mongo;

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        // Load test data once
        JsonDataLoader.load();

        // REST Assured base config
        Map<String, Object> svc = JsonDataLoader.service();
        String baseUrl = (String) svc.getOrDefault("baseUrl", "https://buyme.local");
        int port = (int) svc.getOrDefault("port", 5001);
        boolean relaxed = (boolean) svc.getOrDefault("relaxedHttps", true);

        RestAssured.baseURI = baseUrl;
        RestAssured.port = port;
        if (relaxed)
            RestAssured.useRelaxedHTTPSValidation();

        RestAssured.config = RestAssured.config().objectMapperConfig(
                new ObjectMapperConfig(ObjectMapperType.JACKSON_2));

        spec = RestAssured.given()
                .header("Accept", "application/json, text/plain, */*")
                .header("Connection", "keep-alive")
                .header("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8");

        String uri = System.getenv("MONGO_URI");
        if (uri == null || uri.isBlank()) {
            uri = "mongodb://localhost:27017/buyme";
        }
        mongo = new MongoHelper(uri, "buyme");
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        if (mongo != null)
            mongo.close();
        String summary = "adminEmail=" + RunContext.getAdminEmail() + "\n"
                + "userEmail=" + RunContext.getUserEmail();
        Helpers.attachText("Run Summary - Emails", summary);
    }

 

    @Step("Assert expected status code {expected} but got {actual} if mismatched")
    protected void assertStatus(Response resp, int expected, String context) {
        int actual = resp.getStatusCode();
        if (actual != expected) {
           
            String msg  = extractMessage(resp);

           
            Allure.step(context + " • expected " + expected + " but got: " + actual
                            + " | message: " + msg, Status.FAILED);

            throw new AssertionError("Expected status code is " + expected + " but got: "
                    + actual + " | message: " + msg);
        }
    }

    /** Body -> string aman */
    // // private String safeBody(Response resp) {
    //     try { return resp.asPrettyString(); } catch (Exception e) { return "<unreadable body>"; }
    // }

    /** Ambil "message" (kontrak API kamu hanya pakai field ini) */
    private String extractMessage(Response resp) {
        try {
            String m = resp.jsonPath().getString("message");
            return (m == null || m.isBlank()) ? "<no message>" : m;
        } catch (Exception e) {
            return "<unparsable body>";
        }
    }
}