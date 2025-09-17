# 🚀 WMS System Startup Guide

## Prerequisites Check

Make sure you have:
- ✅ Java 17+ installed
- ✅ Maven installed
- ✅ Docker and Docker Compose installed
- ✅ Git installed

## Quick Start (3 Steps)

### Step 1: Start Infrastructure
```bash
# Navigate to project root
cd swift_logistics_wms

# Start PostgreSQL and RabbitMQ
docker-compose up -d

# Check if services are running
docker-compose ps
```

### Step 2: Start WMS Application
```bash
# Navigate to WMS directory
cd wms

# Clean and start the application
mvn clean spring-boot:run
```

### Step 3: Test the System
```bash
# Run the test script (Windows)
test-api.bat

# OR run individual tests
curl http://localhost:8080/api/orders
curl http://localhost:8080/api/drivers
```

## 🌐 Access Points

| Service | URL | Credentials |
|---------|-----|-------------|
| WMS REST API | http://localhost:8080 | No auth required |
| RabbitMQ Management | http://localhost:15672 | wmsuser/wmspass |
| PostgreSQL | localhost:5432 | wmsuser/wmspass |

## 📋 Testing Order Processing

### 1. Send Order to RabbitMQ (Simulate CMA System)

Using RabbitMQ Management UI (http://localhost:15672):
1. Go to "Queues" tab
2. Click on "cma.orders.queue"
3. Expand "Publish message"
4. Paste this JSON in the payload:

```json
{
  "clientRef": "ORD-001",
  "customerName": "John Smith",
  "deliveryAddress": "123 Main St, Colombo",
  "deliveryDate": "2025-12-25T10:00:00"
}
```

### 2. Check if Order was Processed
```bash
curl http://localhost:8080/api/orders
```

### 3. Create a Driver
```bash
curl -X POST http://localhost:8080/api/drivers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "licenseNumber": "DL123456",
    "phoneNumber": "0771234567",
    "email": "john.doe@example.com"
  }'
```

### 4. Assign Driver to Order
```bash
# Get order ID from previous step, then:
curl -X PUT http://localhost:8080/api/orders/1/assign-driver \
  -H "Content-Type: application/json" \
  -d '{"driverId": 1}'
```

### 5. Mark Order as Delivered
```bash
curl -X PUT http://localhost:8080/api/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "DELIVERED"}'
```

## 🔧 Troubleshooting

### Application won't start?
```bash
# Check if ports are free
netstat -an | findstr :8080
netstat -an | findstr :5432
netstat -an | findstr :5672

# Clean Maven cache
mvn clean install
```

### RabbitMQ connection issues?
```bash
# Check Docker containers
docker-compose logs rabbitmq

# Restart services
docker-compose down
docker-compose up -d
```

### Database connection issues?
```bash
# Check PostgreSQL logs
docker-compose logs postgres

# Connect to database directly
docker exec -it wms-postgres psql -U wmsuser -d wmsdb
```

## 📊 Database Schema

The system automatically creates these tables:

### orders
- id (PRIMARY KEY)
- client_ref (UNIQUE)
- customer_name
- delivery_address
- delivery_date
- status (RECEIVED/READY_TO_DISPATCH/DELIVERED)
- driver_id (FOREIGN KEY)
- created_at
- updated_at

### drivers
- id (PRIMARY KEY)
- name
- license_number (UNIQUE)
- phone_number
- email
- available (BOOLEAN)
- created_at
- updated_at

## 🎯 Order Status Flow

```
CMA System → RabbitMQ → [RECEIVED] → Assign Driver → [READY_TO_DISPATCH] → Complete → [DELIVERED]
```

## 📝 Logs

Check application logs for debugging:
```bash
# In the Maven terminal, you'll see:
INFO  - Received order message: {...}
INFO  - Order ORD-001 processed and saved successfully
INFO  - Driver John Doe assigned to order ORD-001
```

## 🛑 Shutdown

```bash
# Stop Spring Boot (Ctrl+C in terminal)
# Stop Docker services
docker-compose down
```