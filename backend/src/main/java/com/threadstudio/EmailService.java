package com.threadstudio;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOrderConfirmation(
            String email,
            Long orderId,
            double total,
            String paymentMethod) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("ThreadStudio - Order Confirmation");

        message.setText(
            "Hello!\n\n" +
            "Your order has been placed successfully.\n\n" +
            "Order ID: " + orderId + "\n" +
            "Total Amount: Rs. " + total + "\n" +
            "Payment Method: " + paymentMethod + "\n\n" +
            "Thank you for shopping with ThreadStudio!\n\n" +
            "Regards,\nThreadStudio Team"
        );

        mailSender.send(message);
    }
}