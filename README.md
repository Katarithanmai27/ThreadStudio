# ThreadStudio — Fashion Designer Marketplace

ThreadStudio is a full-stack marketplace where fashion design students and emerging designers can showcase clothing designs, build their brands, and sell products to customers.

## Technology Stack

- **Frontend:** React.js, Vite
- **Backend:** Java 21, Spring Boot 3, Spring Security, JWT
- **Database:** MySQL 8
- **Deployment:** Docker Compose

## User Roles and Key Features

- **Customer:** Browse and search products, manage a shopping cart, place orders, view order history, and rate products.
- **Designer:** Register as a designer, submit products, manage their own products, and view orders.
- **Administrator:** Approve designer registrations, approve products, and manage product listings.

The application uses JWT bearer authentication and role-based API authorization.

## Getting Started with Docker

1. Install Docker Desktop or Docker Engine with Docker Compose.
2. Copy `.env.example` to `.env`.
3. Configure the required environment variables and set a strong, unique `JWT_SECRET`.
4. Start the application:

   ```bash
   docker compose up --build
   ```

5. Open [http://localhost:5173](http://localhost:5173) in your browser.

Use secure credentials and appropriate configuration before deploying publicly.

## Local Development

### Backend

Requirements: Java 21 and Maven.

```bash
cd backend
mvn spring-boot:run
```

Configure the database connection and JWT secret using environment variables or the application's supported local configuration.

### Frontend

Requirements: A compatible Node.js version.

```bash
cd frontend
npm install
npm run dev
```

Configure `VITE_API_URL` to point to your backend API if it differs from the project's default.

## Initial Administrator Setup

For the initial local development setup, register an account and promote it to administrator in MySQL using the appropriate table and role values for your application.

Example, if your schema uses these names:

```sql
UPDATE users
SET role = 'ADMIN'
WHERE email = 'your-email@example.com';
```

Confirm that the role values and database schema match your implementation before running this query.

## Production Considerations

- Use strong database credentials and a secure JWT secret.
- Enable HTTPS and restrict database access.
- Configure CORS for the deployed frontend origin.
- Consider email verification, password recovery, rate limiting, audit logs, backups, and monitoring.
- The current checkout flow supports cash on delivery/demo orders; online payment integration requires a payment provider and secure server-side implementation.
- Validate product image URLs and consider object storage for production.
- Review applicable privacy, consumer protection, seller, return, and refund requirements.

