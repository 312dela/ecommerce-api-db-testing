package model.order;

public record OrderItem(String product, String name, int price, String image, int stock, int quantity) {
}
