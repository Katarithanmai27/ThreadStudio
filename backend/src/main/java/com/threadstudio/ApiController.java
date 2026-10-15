package com.threadstudio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@RestController
@RequestMapping("/api")
class ApiController {
  private final UserRepository users; private final ProductRepository products; private final OrderRepository orders;
  private final ReviewRepository reviews; private final PasswordEncoder encoder; private final JwtService jwt;
  ApiController(UserRepository u,ProductRepository p,OrderRepository o,ReviewRepository r,PasswordEncoder e,JwtService j) {
    users=u; products=p; orders=o; reviews=r; encoder=e; jwt=j;
  }
  record RegisterRequest(@NotBlank String name,@Email @NotBlank String email,@Size(min=8) String password,String role) {}
  record LoginRequest(@Email @NotBlank String email,@NotBlank String password) {}
  record ProductRequest(@NotBlank String title,@NotBlank String description,@Positive double price,String category,String imageUrl,String size,@Min(0) int stock) {}
  record CheckoutRequest(@NotBlank String shippingAddress, @NotEmpty List<Long> productIds) {}
  record ReviewRequest(@Min(1) @Max(5) int rating,String comment) {}

  private AppUser current(Authentication a) {
    return users.findByEmail(a.getName()).orElseThrow(()->new RuntimeException("Account not found"));
  }
  private Map<String,Object> userData(AppUser u) {
    return Map.of("id",u.id,"name",u.name,"email",u.email,"role",u.role.name(),"designerApproved",u.designerApproved);
  }

  @PostMapping("/auth/register")
  ResponseEntity<?> register(@Valid @RequestBody RegisterRequest r) {
    if(users.findByEmail(r.email().toLowerCase()).isPresent()) return ResponseEntity.badRequest().body(Map.of("message","Email already registered"));
    Role role="DESIGNER".equalsIgnoreCase(r.role())?Role.DESIGNER:Role.USER;
    AppUser u=new AppUser(r.name().trim(),r.email().toLowerCase().trim(),encoder.encode(r.password()),role);
    u.designerApproved=role==Role.USER; users.save(u);
    return ResponseEntity.ok(Map.of("token",jwt.create(u),"user",userData(u)));
  }
  @PostMapping("/auth/login")
  ResponseEntity<?> login(@Valid @RequestBody LoginRequest r) {
    AppUser u=users.findByEmail(r.email().toLowerCase().trim()).orElse(null);
    if(u==null || !encoder.matches(r.password(),u.password)) return ResponseEntity.status(401).body(Map.of("message","Invalid email or password"));
    return ResponseEntity.ok(Map.of("token",jwt.create(u),"user",userData(u)));
  }
  @GetMapping("/auth/me") Map<String,Object> me(Authentication a){ return userData(current(a)); }

  @GetMapping("/products")
  List<Product> browse(@RequestParam(required=false) String q) {
    return (q==null||q.isBlank())?products.findByStatusOrderByCreatedAtDesc(ProductStatus.APPROVED):products.findByStatusAndTitleContainingIgnoreCase(ProductStatus.APPROVED,q);
  }
  @GetMapping("/products/{id}") ResponseEntity<?> product(@PathVariable Long id) {
    return products.findById(id).filter(p->p.status==ProductStatus.APPROVED).<ResponseEntity<?>>map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/designer/products")
  ResponseEntity<?> createProduct(@Valid @RequestBody ProductRequest r,Authentication a) {
    AppUser d=current(a);
    if(d.role!=Role.ADMIN && !d.designerApproved) return ResponseEntity.status(403).body(Map.of("message","Your designer profile is awaiting admin approval"));
    Product p=new Product(); p.title=r.title(); p.description=r.description(); p.price=r.price(); p.category=r.category(); p.imageUrl=r.imageUrl();
    p.size=r.size(); p.stock=r.stock(); p.designer=d; p.status=d.role==Role.ADMIN?ProductStatus.APPROVED:ProductStatus.PENDING;
    return ResponseEntity.ok(products.save(p));
  }
  @GetMapping("/designer/products") List<Product> myProducts(Authentication a) {
    AppUser d=current(a); return products.findByDesignerIdOrderByCreatedAtDesc(d.id);
  }
  @PutMapping("/designer/products/{id}")
  ResponseEntity<?> updateProduct(@PathVariable Long id,@Valid @RequestBody ProductRequest r,Authentication a) {
    AppUser d=current(a); Product p=products.findById(id).orElse(null);
    if(p==null) return ResponseEntity.notFound().build();
    if(d.role!=Role.ADMIN && !p.designer.id.equals(d.id)) return ResponseEntity.status(403).build();
    p.title=r.title(); p.description=r.description(); p.price=r.price(); p.category=r.category(); p.imageUrl=r.imageUrl(); p.size=r.size(); p.stock=r.stock();
    if(d.role!=Role.ADMIN) p.status=ProductStatus.PENDING;
    return ResponseEntity.ok(products.save(p));
  }
  @DeleteMapping("/designer/products/{id}") ResponseEntity<?> deleteProduct(@PathVariable Long id,Authentication a) {
    AppUser d=current(a); Product p=products.findById(id).orElse(null);
    if(p==null) return ResponseEntity.notFound().build();
    if(d.role!=Role.ADMIN && !p.designer.id.equals(d.id)) return ResponseEntity.status(403).build();
    products.delete(p); return ResponseEntity.noContent().build();
  }

  @GetMapping("/admin/designers") List<AppUser> pendingDesigners() { return users.findByRoleAndDesignerApprovedFalse(Role.DESIGNER); }
  @PostMapping("/admin/designers/{id}/approve") ResponseEntity<?> approveDesigner(@PathVariable Long id) {
    AppUser u=users.findById(id).orElse(null); if(u==null) return ResponseEntity.notFound().build();
    if(u.role!=Role.DESIGNER) return ResponseEntity.badRequest().body(Map.of("message","Not a designer"));
    u.designerApproved=true; users.save(u); return ResponseEntity.ok(userData(u));
  }
  @GetMapping("/admin/products") List<Product> allProducts() { return products.findAll(); }
  @PostMapping("/admin/products/{id}/approve") ResponseEntity<?> approveProduct(@PathVariable Long id) {
    Product p=products.findById(id).orElse(null); if(p==null) return ResponseEntity.notFound().build();
    p.status=ProductStatus.APPROVED; return ResponseEntity.ok(products.save(p));
  }
  @PostMapping("/admin/products/{id}/reject") ResponseEntity<?> rejectProduct(@PathVariable Long id) {
    Product p=products.findById(id).orElse(null); if(p==null) return ResponseEntity.notFound().build();
    p.status=ProductStatus.REJECTED; return ResponseEntity.ok(products.save(p));
  }

  @PostMapping("/orders") @Transactional
  ResponseEntity<?> checkout(@Valid @RequestBody CheckoutRequest r,Authentication a) {
    AppUser buyer=current(a); PurchaseOrder order=new PurchaseOrder(); order.buyer=buyer; order.shippingAddress=r.shippingAddress().trim();
    Map<Long,Long> quantities=new LinkedHashMap<>();
    for(Long id:r.productIds()) quantities.merge(id,1L,Long::sum);
    for(var entry:quantities.entrySet()) {
      Product p=products.findById(entry.getKey()).orElse(null); int quantity=entry.getValue().intValue();
      if(p==null||p.status!=ProductStatus.APPROVED||p.stock<quantity) return ResponseEntity.badRequest().body(Map.of("message","A product is unavailable or stock is insufficient"));
      OrderItem item=new OrderItem(); item.order=order; item.product=p; item.quantity=quantity; item.unitPrice=p.price; order.items.add(item); order.total+=p.price*quantity; p.stock-=quantity;
    }
    return ResponseEntity.ok(orders.save(order));
  }
  @GetMapping("/orders") List<PurchaseOrder> myOrders(Authentication a) { return orders.findByBuyerIdOrderByCreatedAtDesc(current(a).id); }
  @GetMapping("/designer/orders") List<PurchaseOrder> designerOrders(Authentication a) { return orders.findByItemsProductDesignerIdOrderByCreatedAtDesc(current(a).id); }
  @PostMapping("/designer/orders/{id}/accept") ResponseEntity<?> acceptOrder(@PathVariable Long id,Authentication a) {
    AppUser d=current(a); PurchaseOrder o=orders.findById(id).orElse(null); if(o==null) return ResponseEntity.notFound().build();
    boolean owns=o.items.stream().anyMatch(i->i.product.designer.id.equals(d.id));
    if(!owns && d.role!=Role.ADMIN) return ResponseEntity.status(403).build();
    o.status=OrderStatus.ACCEPTED; return ResponseEntity.ok(orders.save(o));
  }

  @GetMapping("/reviews/{productId}") List<ProductReview> productReviews(@PathVariable Long productId) { return reviews.findByProductIdOrderByCreatedAtDesc(productId); }
  @PostMapping("/reviews/{productId}") ResponseEntity<?> addReview(@PathVariable Long productId,@Valid @RequestBody ReviewRequest r,Authentication a) {
    Product p=products.findById(productId).orElse(null); if(p==null) return ResponseEntity.notFound().build();
    ProductReview v=new ProductReview(); v.product=p; v.reviewer=current(a); v.rating=r.rating(); v.comment=r.comment();
    return ResponseEntity.ok(reviews.save(v));
  }
}
