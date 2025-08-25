package model.order;

public record ShippingInfo(String address, String city, String country, int pinCode) {
}