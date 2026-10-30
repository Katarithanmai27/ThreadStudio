
package com.threadstudio;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Payment;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.*;

@Service
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final EmailService emailService;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    public PaymentService(
            RazorpayClient razorpayClient,
            ProductRepository products,
            OrderRepository orders,
        EmailService emailService) {

        this.razorpayClient = razorpayClient;
        this.products = products;
        this.orders = orders;
        this.emailService = emailService;
    }

    // CREATE RAZORPAY ORDER
    @Transactional
    public Map<String, Object> createPaymentOrder(
            AppUser buyer,
            String address,
            List<Long> productIds) throws RazorpayException {

        if (address == null || address.isBlank()
                || productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid checkout details");
        }

        Map<Long, Integer> quantities = new LinkedHashMap<>();

        for (Long id : productIds) {

            if (id == null) {
                throw new IllegalArgumentException(
                        "Invalid product ID");
            }

            quantities.merge(id, 1, Integer::sum);
        }

        PurchaseOrder purchase = new PurchaseOrder();

        purchase.buyer = buyer;
        purchase.shippingAddress = address.trim();
        purchase.paymentMethod = "RAZORPAY";
        purchase.paymentStatus = PaymentStatus.PENDING;

        BigDecimal total = BigDecimal.ZERO;

        // Fetch products and calculate total from database
        for (var entry : quantities.entrySet()) {

            Product product = products.findById(entry.getKey())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Product not found"));

            int quantity = entry.getValue();

            if (product.status != ProductStatus.APPROVED
                    || product.stock < quantity) {

                throw new IllegalArgumentException(
                        "Product unavailable or insufficient stock");
            }

            OrderItem item = new OrderItem();

            item.order = purchase;
            item.product = product;
            item.quantity = quantity;
            item.unitPrice = product.price;

            purchase.items.add(item);

            total = total.add(
                    BigDecimal.valueOf(product.price)
                            .multiply(BigDecimal.valueOf(quantity))
            );
        }

        total = total.setScale(2, RoundingMode.HALF_UP);
        purchase.total = total.doubleValue();

        long amountInPaise = total
                .movePointRight(2)
                .longValueExact();

        // Create Razorpay order
        JSONObject request = new JSONObject();

        request.put("amount", amountInPaise);
        request.put("currency", "INR");

        request.put(
                "receipt",
                "TS_" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 20)
        );

        com.razorpay.Order razorpayOrder =
                razorpayClient.orders.create(request);

        purchase.razorpayOrderId =
                razorpayOrder.get("id");

        // Save pending order
        orders.save(purchase);

        return Map.of(
                "keyId", keyId,
                "orderId", purchase.razorpayOrderId,
                "purchaseOrderId", purchase.id,
                "amount", amountInPaise,
                "currency", "INR"
        );
    }

    // VERIFY RAZORPAY PAYMENT
    @Transactional
    public Map<String, Object> verifyPayment(
            AppUser buyer,
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature) {

        try {

            if (razorpayOrderId == null
                    || razorpayPaymentId == null
                    || razorpaySignature == null
                    || razorpayOrderId.isBlank()
                    || razorpayPaymentId.isBlank()
                    || razorpaySignature.isBlank()) {

                throw new IllegalArgumentException(
                        "Missing payment details");
            }

            // 1. Generate expected signature
            String data =
                    razorpayOrderId + "|" + razorpayPaymentId;

            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey =
                    new SecretKeySpec(
                            keySecret.getBytes(StandardCharsets.UTF_8),
                            "HmacSHA256"
                    );

            mac.init(secretKey);

            byte[] hash = mac.doFinal(
                    data.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder expected = new StringBuilder();

            for (byte b : hash) {
                expected.append(
                        String.format("%02x", b & 0xff)
                );
            }

            // 2. Compare signatures
            boolean valid = MessageDigest.isEqual(
                    expected.toString()
                            .getBytes(StandardCharsets.US_ASCII),

                    razorpaySignature.toLowerCase()
                            .getBytes(StandardCharsets.US_ASCII)
            );

            if (!valid) {
                throw new IllegalArgumentException(
                        "Invalid payment signature");
            }

            // 3. Find order BEFORE accessing purchase.total
            PurchaseOrder purchase = orders
                    .findByRazorpayOrderId(razorpayOrderId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Order not found"));

            // 4. Verify customer ownership
            if (!purchase.buyer.id.equals(buyer.id)) {
                throw new IllegalArgumentException(
                        "Unauthorized payment");
            }

            // 5. Handle duplicate verification
            if (purchase.paymentStatus == PaymentStatus.PAID) {

                if (razorpayPaymentId.equals(
                        purchase.razorpayPaymentId)) {

                    return Map.of(
                            "message",
                            "Payment already verified",
                            "paymentStatus",
                            "PAID"
                    );
                }

                throw new IllegalArgumentException(
                        "Order already paid");
            }

            if (purchase.paymentStatus != PaymentStatus.PENDING) {
                throw new IllegalArgumentException(
                        "Payment is not pending");
            }

            // 6. Fetch actual payment details
            Payment payment =
                    razorpayClient.payments.fetch(
                            razorpayPaymentId);

           Object orderIdValue = payment.get("order_id");
String actualOrderId = orderIdValue.toString();

Object statusValue = payment.get("status");
String status = statusValue.toString();

Object amountValue = payment.get("amount");
long actualAmount = Long.parseLong(amountValue.toString());

Object currencyValue = payment.get("currency");
String currency = currencyValue.toString();
            // 7. Calculate expected amount from database
            long expectedAmount =
                    BigDecimal.valueOf(purchase.total)
                            .movePointRight(2)
                            .setScale(0, RoundingMode.HALF_UP)
                            .longValueExact();

            // 8. Validate payment
            if (!razorpayOrderId.equals(actualOrderId)
                    || !"captured".equals(status)
                    || actualAmount != expectedAmount
                    || !"INR".equals(currency)) {

                throw new IllegalArgumentException(
                        "Payment details do not match");
            }

            // 9. Reduce stock after successful payment
            for (OrderItem item : purchase.items) {

                Product product = products
                        .findById(item.product.id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Product not found"));

                if (product.stock < item.quantity) {
                    throw new IllegalArgumentException(
                            "Insufficient stock for "
                                    + product.title);
                }

                product.stock -= item.quantity;

                products.save(product);
            }

            // 10. Update payment information
            purchase.razorpayPaymentId =
                    razorpayPaymentId;

            purchase.paymentStatus =
                    PaymentStatus.PAID;

            orders.save(purchase);
            try {
    emailService.sendOrderConfirmation(
            buyer.email,
            purchase.id,
            purchase.total,
            "Razorpay"
    );
} catch (Exception e) {
    System.err.println("Order confirmation email failed: " + e.getMessage());
}

            return Map.of(
                    "message",
                    "Payment verified successfully",

                    "paymentStatus",
                    "PAID",

                    "orderId",
                    purchase.id
            );

        } catch (IllegalArgumentException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Payment verification failed", e);
        }
    }
}
