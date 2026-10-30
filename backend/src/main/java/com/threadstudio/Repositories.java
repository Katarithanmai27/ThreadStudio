package com.threadstudio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

interface UserRepository extends JpaRepository<AppUser,Long> {
  Optional<AppUser> findByEmail(String email);
  List<AppUser> findByRoleAndDesignerApprovedFalse(Role role);
}
interface ProductRepository extends JpaRepository<Product,Long> {
  List<Product> findByStatusOrderByCreatedAtDesc(ProductStatus status);
  List<Product> findByDesignerIdOrderByCreatedAtDesc(Long id);
  List<Product> findByStatusAndTitleContainingIgnoreCase(ProductStatus status,String title);
}
interface OrderRepository extends JpaRepository<PurchaseOrder,Long> {
  List<PurchaseOrder> findByBuyerIdOrderByCreatedAtDesc(Long id);
  List<PurchaseOrder> findByItemsProductDesignerIdOrderByCreatedAtDesc(Long id);
  Optional<PurchaseOrder> findByRazorpayOrderId(String razorpayOrderId);
}
interface ReviewRepository extends JpaRepository<ProductReview,Long> {
  List<ProductReview> findByProductIdOrderByCreatedAtDesc(Long id);
}
