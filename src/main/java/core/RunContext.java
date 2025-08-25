package core;

public class RunContext {
    private static String adminEmail;
    private static String adminToken;
    private static String userEmail;
    private static String userToken;

    private static String categoryId;
    private static String productId;
    private static String imageUrl;
    private static String orderId;

    public static String getAdminEmail() {
        return adminEmail;
    }

    public static void setAdminEmail(String v) {
        adminEmail = v;
    }

    public static String getAdminToken() {
        return adminToken;
    }

    public static void setAdminToken(String v) {
        adminToken = v;
    }

    public static String getUserEmail() {
        return userEmail;
    }

    public static void setUserEmail(String v) {
        userEmail = v;
    }

    public static String getUserToken() {
        return userToken;
    }

    public static void setUserToken(String v) {
        userToken = v;
    }

    public static String getCategoryId() {
        return categoryId;
    }

    public static void setCategoryId(String v) {
        categoryId = v;
    }

    public static String getProductId() {
        return productId;
    }

    public static void setProductId(String v) {
        productId = v;
    }

    public static String getImageUrl() {
        return imageUrl;
    }

    public static void setImageUrl(String v) {
        imageUrl = v;
    }

    public static String getOrderId() {
        return orderId;
    }

    public static void setOrderId(String v) {
        orderId = v;
    }
}
