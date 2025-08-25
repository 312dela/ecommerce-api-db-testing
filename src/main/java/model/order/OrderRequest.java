package model.order;

import java.util.List;
import java.util.Map;

public record OrderRequest(
        ShippingInfo shippingInfo,
        List<OrderItem> orderItems,
        String paymentMethod,
        Map<String, Object> paymentInfo,
        int itemsPrice,
        int taxPrice,
        int shippingCharges,
        int totalAmount) {
}
