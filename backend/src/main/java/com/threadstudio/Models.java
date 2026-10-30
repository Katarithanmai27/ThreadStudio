package com.threadstudio;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

enum Role { USER, DESIGNER, ADMIN }
enum ProductStatus { PENDING, APPROVED, REJECTED }
enum OrderStatus { PLACED, ACCEPTED, SHIPPED, DELIVERED, CANCELLED }
enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED
}

@Entity @Table(name="users")
class AppUser {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Column(nullable=false) public String name;
  @Column(nullable=false, unique=true) public String email;
  @JsonIgnore @Column(nullable=false) public String password;
  @Enumerated(EnumType.STRING) @Column(nullable=false) public Role role=Role.USER;
  public boolean designerApproved=false;
  public Instant createdAt=Instant.now();
  protected AppUser() {}
  AppUser(String n,String e,String p,Role r) { name=n; email=e; password=p; role=r; }
}

@Entity
class Product {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Column(nullable=false) public String title;
  @Column(length=3000) public String description;
  @Column(nullable=false) public double price;
  public String category;
  public String imageUrl;
  public String size;
  public int stock=1;
  @Enumerated(EnumType.STRING) public ProductStatus status=ProductStatus.PENDING;
  @ManyToOne(optional=false) public AppUser designer;
  public Instant createdAt=Instant.now();
  protected Product() {}
}

@Entity
class PurchaseOrder {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(optional=false) public AppUser buyer;
  @Column(nullable=false) public double total;
  @Enumerated(EnumType.STRING) public OrderStatus status=OrderStatus.PLACED;
  @Column(nullable=false) public String shippingAddress;
  @Column(nullable=false) public String paymentMethod="CASH_ON_DELIVERY";
  @Enumerated(EnumType.STRING)
@Column(nullable=false)
public PaymentStatus paymentStatus = PaymentStatus.PENDING;

public String razorpayOrderId;

public String razorpayPaymentId;
public Instant createdAt=Instant.now();
 @JsonIgnore
@OneToMany(mappedBy="order", cascade=CascadeType.ALL, orphanRemoval=true)
public List<OrderItem> items = new ArrayList<>();
protected PurchaseOrder() {}
}

@Entity
class OrderItem {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @JsonIgnore @ManyToOne(optional=false) public PurchaseOrder order;
  @ManyToOne(optional=false) public Product product;
  public int quantity;
  public double unitPrice;
  protected OrderItem() {}
}

@Entity
class ProductReview {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(optional=false) public Product product;
  @ManyToOne(optional=false) public AppUser reviewer;
  public int rating;
  @Column(length=1000) public String comment;
  public Instant createdAt=Instant.now();
  protected ProductReview() {}
}

