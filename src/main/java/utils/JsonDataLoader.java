package utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
public class JsonDataLoader {

    private static Map<String, Object> root;

    public static void load() {
        if (root != null) return;
        try {
            File f = new File("src/test/resources/test_data.json");
            root = new ObjectMapper().readValue(f, HashMap.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test_data.json", e);
        }
    }

    public static Map<String, Object> signupDefaults() {
        return (Map<String, Object>) root.get("signupDefaults");
    }

    public static Map<String, Object> product() {
        return (Map<String, Object>) root.get("product");
    }

    public static Map<String, Object> order() {
        return (Map<String, Object>) root.get("order");
    }

    public static Map<String, Object> service() {
        return (Map<String, Object>) root.get("service");
    }
}
