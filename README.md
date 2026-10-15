# ThreadStudio — Designer Marketplace

A full-stack marketplace where fashion-design students can publish dresses/clothes, build a brand, receive orders, and sell to shoppers.

## Stack
- Frontend: React + Vite
- Backend: Java 21, Spring Boot 3, Spring Security, JWT
- Database: MySQL 8
- Deployment: Docker Compose (frontend + API + database)

## Roles and main flows
- **USER**: browse/search products, cart, place an order, view orders, rate products.
- **DESIGNER**: register as a designer, submit products, edit/delete own products, view orders.
- **ADMIN**: approve designers, approve products, manage product listings.
- JWT bearer authentication; role-based API authorization.

## Quick start (Docker)
1. Install Docker Desktop / Docker Engine with Compose.
2. Copy `.env.example` to `.env` and change `JWT_SECRET` to a long random value.
3. Run `docker compose up --build`.
4. Open http://localhost:5173

Default database credentials are for local development only. Change them before deploying publicly.

## Local development
### Backend
Requires Java 21 and Maven.
```bash
cd backend
mvn spring-boot:run
```
Configure `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `JWT_SECRET` as environment variables, or use the local defaults in `application.yml`.

### Frontend
Requires Node 20.19+ or 22.12+.
```bash
cd frontend
npm install
npm run dev
```
Set `VITE_API_URL=http://localhost:8080/api` if the API is elsewhere.

## First admin
Register a normal account, then promote it to ADMIN directly in MySQL for the first setup:
```sql
UPDATE users SET role='ADMIN' WHERE email='your-email@example.com';
```
Restart/login again to receive an ADMIN token.

## Important production checklist
- Set strong unique DB credentials and `JWT_SECRET` (32+ random bytes).
- Use HTTPS and a managed MySQL instance or private database network.
- Configure CORS to your deployed frontend origin.
- Add email verification/password reset, rate limiting, audit logs, backups, and monitoring.
- This starter uses a **cash-on-delivery/demo order checkout**. It does not collect card details or integrate a payment gateway. Integrate Razorpay/Stripe server-side before offering online payments.
- Product image URLs are stored as URLs. For production, use object storage (S3/Cloudinary) and validate uploaded files.
- Review tax, consumer-protection, privacy, return/refund, and seller policies for your jurisdiction.
