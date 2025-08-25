package api;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Map;

public class ApiClient {

    private final RequestSpecification baseSpec;
    private final AllureRestAssured allure = new AllureRestAssured();

    public ApiClient(RequestSpecification spec) {
        // Pasang Allure filter supaya request/response otomatis ke-log di report
        this.baseSpec = spec;
    }

    private RequestSpecification fresh() {
        return RestAssured.given().spec(baseSpec).filter(allure);
    }

    private String vhost() {
        try {
            URI u = URI.create(RestAssured.baseURI);
            String host = u.getHost();
            if (host == null || host.isBlank())
                return null;
            int p = RestAssured.port;
            return p > 0 ? host + ":" + p : host;
        } catch (Exception e) {
            return null;
        }
    }

    private RequestSpecification withHost(RequestSpecification req) {
        String vh = vhost();
        return (vh != null) ? req.header("Host", vh) : req;
    }

    public Response signupMultipart(String name, String email, String password,
            String address, String city, int pincode, String country, String roleOrNull) {

        RequestSpecification req = withHost(fresh())
                .contentType(ContentType.MULTIPART)
                .multiPart("name", name)
                .multiPart("email", email)
                .multiPart("password", password)
                .multiPart("address", address)
                .multiPart("city", city)
                .multiPart("pincode", String.valueOf(pincode))
                .multiPart("country", country);

        if (roleOrNull != null && !roleOrNull.isBlank()) {
            req.multiPart("role", roleOrNull); // hanya untuk admin
        }

        return req.when().post("/api/v1/user/signup");
    }

    public Response loginJson(Object body) {
        return withHost(fresh())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/user/login");
    }

    public Response profileWithCookieToken(String token) {
        return withHost(fresh())
                .cookie("token", token)
                .when()
                .get("/api/v1/user/profile");
    }

    public Response addCategory(String token, String category) {
        return withHost(fresh())
                .contentType(ContentType.JSON)
                .cookie("token", token)
                .body(Map.of("category", category))
                .when()
                .post("/api/v1/product/addcategory");
    }

    public Response addProduct(String token, String name, String description,
            String categoryId, int price, int stock, String imageFileName) {
        String imgPath = Paths.get(System.getProperty("user.dir"),
                "src", "test", "resources", imageFileName).toString();

        File img = new File(imgPath);
        if (!img.exists()) {
            throw new IllegalArgumentException("Image file not found at: " + img.getAbsolutePath());
        }

        return withHost(fresh())
                .cookie("token", token)
                .contentType(ContentType.MULTIPART)
                .multiPart("name", name)
                .multiPart("description", description)
                .multiPart("category", categoryId)
                .multiPart("price", String.valueOf(price))
                .multiPart("stock", String.valueOf(stock))
                .multiPart("file", img)
                .when()
                .post("/api/v1/product/new");
    }

    public Response deleteProduct(String token, String productId) {
        return withHost(fresh())
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .cookie("token", token)
                .when()
                .delete("/api/v1/product/single/" + productId);
    }

    public Response deleteCategory(String token, String categoryId) {
        return withHost(fresh())
                .contentType(ContentType.JSON)
                .cookie("token", token)
                .when()
                .delete("/api/v1/product/category/" + categoryId);
    }

    public Response newOrder(String token, Object body) {
        return withHost(fresh())
                .contentType(ContentType.JSON)
                .cookie("token", token)
                .body(body)
                .when()
                .post("/api/v1/order/new");
    }

    public Response updateOrderAsUser(String token, String orderId) {
        return withHost(fresh())
                .contentType(ContentType.JSON)
                .cookie("token", token)
                .body("{}")
                .when()
                .put("/api/v1/order/single/" + orderId);
    }
}