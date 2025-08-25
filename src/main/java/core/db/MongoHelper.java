package core.db;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.types.ObjectId;
import static com.mongodb.client.model.Filters.*;

import java.util.List;
public class MongoHelper {
    private final MongoClient client;
    private final MongoDatabase db;

    public MongoHelper(String uri, String dbName) {
        this.client = MongoClients.create(uri);
        this.db = client.getDatabase(dbName);
    }

    public void close() {
        client.close();
    }

    public Document findCategoryByName(String name) {
        MongoCollection<Document> col = db.getCollection("categories");
        return col.find(Filters.eq("category", name))
                    .sort(Sorts.descending("_id"))
                    .first();
    }

    // public Document findProductByName(String name) {
    //     MongoCollection<Document> col = db.getCollection("products");
    //     return col.find(eq("name", name)).first();
    // }

    public Document findUserByEmailAndRole(String email, String role) {
        MongoCollection<Document> col = db.getCollection("users");
        return col.find(and(eq("email", email), eq("role", role))).first();
    }

    public Document findLatestOrderByStatus(String status) {
        MongoCollection<Document> col = db.getCollection("orders");
        return col.find(eq("orderStatus", status))
                .sort(Sorts.descending("createdAt"))
                .first();
    }

    public Document findLatestProductByName(String name) {
        MongoCollection<Document> col = db.getCollection("products");
        return col.find(Filters.eq("name", name))
                  .sort(Sorts.descending("_id"))
                  .first();
    }

    public Document findLatestProductByNameAndCategory(String name, String categoryIdHex) {
        MongoCollection<Document> col = db.getCollection("products");
        return col.find(Filters.and(
                        Filters.eq("name", name),
                        Filters.eq("category", new ObjectId(categoryIdHex))
                ))
                .sort(Sorts.descending("_id"))
                .first();
    }

    public Document waitForProductImageUrl(String productIdHex, long timeoutMs, long intervalMs) {
        MongoCollection<Document> col = db.getCollection("products");
        ObjectId id = new ObjectId(productIdHex);
        long end = System.currentTimeMillis() + timeoutMs;
        Document last = null;

        while (System.currentTimeMillis() < end) {
            Document d = col.find(Filters.eq("_id", id)).first();
            last = d;
            if (d != null) {
                @SuppressWarnings("unchecked")
                List<Document> images = (List<Document>) d.get("images");
                if (images != null && !images.isEmpty()) {
                    String url = images.get(0).getString("url");
                    if (url != null && !url.isBlank()) return d; 
                }
            }
            try { Thread.sleep(intervalMs); } catch (InterruptedException ignored) {}
        }
        return last; 
    }

    public boolean existsById(String collection, String hexId) {
        Document d = db.getCollection(collection).find(Filters.eq("_id", new ObjectId(hexId))).first();
        return d != null;
    }
}