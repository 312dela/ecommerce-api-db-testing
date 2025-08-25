package tests;

import io.qameta.allure.*;
import io.qameta.allure.model.Status;
import io.restassured.response.Response;
import model.LoginRequest;
import model.order.OrderItem;
import model.order.OrderRequest;
import model.order.ShippingInfo;
import utils.JsonDataLoader;

import org.bson.Document;
import org.testng.Assert;
import org.testng.annotations.*;

import api.ApiClient;
import core.BaseTest;
import core.Helpers;
import core.RunContext;

import java.util.List;
import java.util.Map;

@Epic("Ecommerce API DB Testing")
@Feature("E2E: User/Admin, Product, Category, Order")
public class EcommerceE2E extends BaseTest {

    private ApiClient api;

    @BeforeClass(alwaysRun = true)
    public void init() {
        api = new ApiClient(spec);
    }

    @Test(priority = 1, description = "Create new account for admin role and verify profile")
    @Severity(SeverityLevel.CRITICAL)
    public void createAdminAccountAndVerifyProfile() {
        Map<String, Object> s = JsonDataLoader.signupDefaults();

        String adminEmail = Helpers.generateRegistrationEmail();
        RunContext.setAdminEmail(adminEmail);
        Allure.parameter("adminEmail", adminEmail);
        Helpers.attachText("adminEmail", adminEmail);
        Response signup = api.signupMultipart(
                (String) s.get("name"),
                adminEmail,
                (String) s.get("password"),
                (String) s.get("address"),
                (String) s.get("city"),
                (Integer) s.get("pincode"),
                (String) s.get("country"),
                (String) s.get("roleAdmin"));

        Helpers.attachJson("Create Admin - Response", signup.asPrettyString());
        assertStatus(signup, 201, "Signup Admin");
        String tokenAdmin = signup.jsonPath().getString("token");
        RunContext.setAdminToken(tokenAdmin);

        Response profile = api.profileWithCookieToken(tokenAdmin);
        Helpers.attachJson("Admin Profile - Response", profile.asPrettyString());
        assertStatus(profile, 200, "Profile (Admin)");

        String actualEmail = profile.jsonPath().getString("user.email");
        String actualRole = profile.jsonPath().getString("user.role");

        if (!adminEmail.equals(actualEmail)) {
            throw new AssertionError("Expected email is " + adminEmail + " but got: " + actualEmail);
        }
        if (!((String) s.get("roleAdmin")).equals(actualRole)) {
            throw new AssertionError("Expected role is " + s.get("roleAdmin") + " but got: " + actualRole);
        }
    }

    @DataProvider(name = "emailVariants")
    public Object[][] emailVariants() {
        String base = RunContext.getAdminEmail();
        return new Object[][] {
                { "lowercase", base.toLowerCase() },
                { "uppercase", base.toUpperCase() }
        };
    }

    @Test(priority = 2, dataProvider = "emailVariants", description = "Login using email variants")
    @Severity(SeverityLevel.NORMAL)
    public void loginAdminVariants(String variantName, String emailVariant) {
        Map<String, Object> s = JsonDataLoader.signupDefaults();

        Response login = api.loginJson(new LoginRequest(emailVariant, (String) s.get("password")));
        Helpers.attachJson("Login (" + variantName + ") - Response", login.asPrettyString());
        assertStatus(login, 200, "Login Admin (" + variantName + ")");
    }

    @Test(priority = 3, description = "Create category + product and verify DB")
    @Severity(SeverityLevel.CRITICAL)
    public void createCategoryAndProductAsAdmin() {
        Map<String, Object> p = JsonDataLoader.product();

        String tokenAdmin = RunContext.getAdminToken();
        String categoryName = (String) p.get("categoryName");

        // Create category
        Response catResp = api.addCategory(tokenAdmin, categoryName);
        Helpers.attachJson("Add Category - Response", catResp.asPrettyString());
        assertStatus(catResp, 201, "Add Category");

        // DB assert category
        Document catDoc = mongo.findCategoryByName(categoryName);
        if (catDoc == null) {
            Assert.fail("No entry found in DB using category: " + categoryName);
        } else {
            Helpers.attachJson("DB Category", catDoc.toJson());
            RunContext.setCategoryId(catDoc.getObjectId("_id").toHexString());
        }
        String categoryId = catDoc.getObjectId("_id").toHexString();
        RunContext.setCategoryId(categoryId);

        // Create product
        String name = (String) p.get("product1");
        String desc = (String) p.get("description");
        int price = (Integer) p.get("price");
        int stock = (Integer) p.get("stock");
        String img = (String) p.get("product1_img");

        Response prodResp = api.addProduct(tokenAdmin, name, desc, RunContext.getCategoryId(), price, stock, img);
        Helpers.attachJson("Add Product - Response", prodResp.asPrettyString());
        assertStatus(prodResp, 200, "Add Product");

        Document prodDoc = mongo.findLatestProductByNameAndCategory(name, RunContext.getCategoryId());
        if (prodDoc == null)
            prodDoc = mongo.findLatestProductByName(name);
        if (prodDoc == null) {
            Assert.fail("No product found in DB using name: " + name);
        }
        Helpers.attachJson("DB Product (initial)", prodDoc.toJson());

        String productId = prodDoc.getObjectId("_id").toHexString();
        RunContext.setProductId(productId);
        Helpers.attachText("productId (from DB)", productId);

        // --- Polling sampai imageUrl terisi (maks 10s, cek tiap 500ms) ---
        Document withImage = mongo.waitForProductImageUrl(productId, 10_000, 500);
        String imageUrl = null;
        if (withImage != null) {
            @SuppressWarnings("unchecked")
            java.util.List<org.bson.Document> images = (java.util.List<org.bson.Document>) withImage.get("images");
            if (images != null && !images.isEmpty()) {
                imageUrl = images.get(0).getString("url");
            }
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            Helpers.attachJson("DB Product (last polled)", withImage == null ? "{}" : withImage.toJson());
            Assert.fail("Product created but image URL not found in DB within timeout. productId=" + productId);
        }

        RunContext.setImageUrl(imageUrl);
        Helpers.attachText("imageUrl (from DB)", imageUrl);
        boolean catExists = mongo.existsById("categories", categoryId);
        if (catExists) {
            Allure.step(categoryName + " is created as new category and recorded in DB with _id: " + categoryId);
        }
        boolean prodExists = mongo.existsById("products", productId);
        if (prodExists) {
            Allure.step(name + " is created as new product and recorded in DB with _id: " + productId);
        }

    }

    @Test(priority = 4, description = "Create user account & verify in DB")
    @Severity(SeverityLevel.CRITICAL)
    public void createUserAccountAndVerifyDB() {
        Map<String, Object> s = JsonDataLoader.signupDefaults();
        String userEmail = Helpers.generateRegistrationEmail();
        RunContext.setUserEmail(userEmail);
        Allure.parameter("userEmail", userEmail);
        Helpers.attachText("userEmail", userEmail);
        Response signup = api.signupMultipart(
                (String) s.get("name"),
                userEmail,
                (String) s.get("password"),
                (String) s.get("address"),
                (String) s.get("city"),
                (Integer) s.get("pincode"),
                (String) s.get("country"),
                null);

        Helpers.attachJson("Create User - Raw Response", signup.asPrettyString());

        if (signup.getStatusCode() != 201) {
            String msg = Helpers.extractMessage(signup);
            Helpers.attachText("Create User - Error Message", msg);
            Allure.step("Create User returned 400 • message: " + msg,
                    Status.FAILED);
        }
        assertStatus(signup, 201, "Signup User");
        RunContext.setUserToken(signup.jsonPath().getString("token"));

        Document userDoc = mongo.findUserByEmailAndRole(userEmail, "user");
        if (userDoc == null) {
            Assert.fail("No account found in DB using email: " + userEmail);
        } else {
            Helpers.attachJson("DB User", userDoc.toJson());
            Allure.step("Account using email: " + userEmail + " is created successfully as user and recorded in DB");
        }
    }

    // =========================
    // 5) Create order and update order status using user role (expect 401)
    // =========================
    @Test(priority = 5, description = "Create order as user and try to update as user (expect 401)")
    @Severity(SeverityLevel.CRITICAL)
    public void createOrderAndUpdateAsUser() {
        Map<String, Object> o = JsonDataLoader.order();

        String tokenUser = RunContext.getUserToken();
        String productId = RunContext.getProductId();
        String imageUrl = RunContext.getImageUrl();
        if (productId == null || productId.isBlank() || imageUrl == null || imageUrl.isBlank()) {
            Helpers.attachText("Missing prerequisites",
                    "productId=" + productId + ", imageUrl=" + imageUrl +
                            "\nPastikan TC 'Create category and product' mengisi RunContext (tidak parallel).");
            Assert.fail("Prerequisite missing: productId or imageUrl is null/blank. Cannot create order.");
        }
        String productName = (String) JsonDataLoader.product().get("product1");
        int price = (Integer) JsonDataLoader.product().get("price");
        int stock = (Integer) JsonDataLoader.product().get("stock");

        Map<String, Object> ship = (Map<String, Object>) o.get("shippingInfo");

        OrderRequest req = new OrderRequest(
                new ShippingInfo(
                        (String) ship.get("address"),
                        (String) ship.get("city"),
                        (String) ship.get("country"),
                        (Integer) ship.get("pinCode")),
                List.of(new OrderItem(productId, productName, price, imageUrl, stock, 1)),
                "COD",
                Map.of(), // paymentInfo
                price,
                (Integer) o.get("taxPrice"),
                (Integer) o.get("shippingCharges"),
                price + (Integer) o.get("taxPrice") + (Integer) o.get("shippingCharges"));

        Response createOrder = api.newOrder(tokenUser, req);
        Helpers.attachJson("Create Order (User) - Response", createOrder.asPrettyString());
        assertStatus(createOrder, 201, "Create Order (User)");

        Document latestPreparing = mongo.findLatestOrderByStatus("Preparing");
        if (latestPreparing == null) {
            Assert.fail("No latest order found in DB with status: Preparing");
        }
        String orderId = latestPreparing.getObjectId("_id").toHexString();
        RunContext.setOrderId(orderId);
        Helpers.attachJson("DB Latest Preparing Order", latestPreparing.toJson());

        // Update status as user -> expect 401
        Response updateAsUser = api.updateOrderAsUser(tokenUser, orderId);
        Helpers.attachJson("Update Order as User - Response", updateAsUser.asPrettyString());
        assertStatus(updateAsUser, 401, "Update Order (User)");
    }

    // =========================
    // 6) Create order as user role and update order status using admin role (expect
    // 200)
    // (Order sudah dibuat di test sebelumnya; kita pakai orderId terbaru Preparing)
    // =========================
    @Test(priority = 6, description = "Update order status as admin (expect 200)")
    @Severity(SeverityLevel.CRITICAL)
    public void updateOrderAsAdmin() {
        String tokenAdmin = RunContext.getAdminToken();
        String orderId = RunContext.getOrderId();

        Response updateAsAdmin = api.updateOrderAsUser(tokenAdmin, orderId);
        Helpers.attachJson("Update Order as Admin - Response", updateAsAdmin.asPrettyString());
        assertStatus(updateAsAdmin, 200, "Update Order (Admin)");
    }

    // =========================
    // 7) Delete product and category using admin role
    // =========================
    @Test(priority = 7, description = "Delete product & category, verify not exists in DB")
    @Severity(SeverityLevel.CRITICAL)
    public void deleteProductAndCategoryAsAdmin() {
        String tokenAdmin = RunContext.getAdminToken();
        String productId = RunContext.getProductId();
        String categoryId = RunContext.getCategoryId();

        // Delete Product
        Response delProd = api.deleteProduct(tokenAdmin, productId);
        Helpers.attachJson("Delete Product - Response", delProd.asPrettyString());
        assertStatus(delProd, 200, "Delete Product");

        boolean prodExists = mongo.existsById("products", productId);
        if (prodExists) {
            Assert.fail("Product still exists in DB with _id: " + productId);
        }

        // Delete Category
        Response delCat = api.deleteCategory(tokenAdmin, categoryId);
        Helpers.attachJson("Delete Category - Response", delCat.asPrettyString());
        assertStatus(delCat, 200, "Delete Category");

        boolean catExists = mongo.existsById("categories", categoryId);
        if (catExists) {
            Assert.fail("Category still exists in DB with _id: " + categoryId);
        }
    }
}