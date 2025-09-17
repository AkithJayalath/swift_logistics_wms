# Swift Logistics WMS (Warehouse Management System)

A simplified Warehouse Management System that processes orders from the CMA system through RabbitMQ and provides REST APIs for order management and driver assignment.

## Features

- **Order Processing**: Listens to RabbitMQ queue for incoming orders from CMA system
- **Driver Management**: Create and manage drivers
- **Order Assignment**: Assign drivers to orders and update status
- **Order Filtering**: Filter orders by delivery date and status
- **Status Management**: Three simple statuses: RECEIVED, READY_TO_DISPATCH, DELIVERED

## Order Status Flow

1. **RECEIVED**: Order received from CMA system via RabbitMQ
2. **READY_TO_DISPATCH**: Driver assigned to order by warehouse worker
3. **DELIVERED**: Order marked as delivered

## Prerequisites

- Java 17+
- PostgreSQL database
- RabbitMQ server
- Maven

## Database Setup

Create PostgreSQL database:
```sql
CREATE DATABASE wmsdb;
CREATE USER wmsuser WITH PASSWORD 'wmspass';
GRANT ALL PRIVILEGES ON DATABASE wmsdb TO wmsuser;
```

## Running the Application

1. Start PostgreSQL and RabbitMQ
2. Navigate to the project directory
3. Run: `mvn spring-boot:run`

The application will start on port 8080.

## API Endpoints

### Orders

#### Get All Orders
```
GET /api/orders
```

#### Get Order by ID
```
GET /api/orders/{id}
```

#### Get Order by Client Reference
```
GET /api/orders/client-ref/{clientRef}
```

#### Get Orders by Status
```
GET /api/orders/status/{status}
```
Status values: `RECEIVED`, `READY_TO_DISPATCH`, `DELIVERED`

#### Get Unassigned Orders
```
GET /api/orders/unassigned
```

#### Get Orders by Driver
```
GET /api/orders/driver/{driverId}
```

#### Filter Orders by Delivery Date
```
GET /api/orders/delivery-date?startDate={startDate}&endDate={endDate}&status={status}
```
- `startDate` and `endDate`: ISO DateTime format (e.g., 2023-12-01T00:00:00)
- `status`: Optional filter parameter

#### Assign Driver to Order
```
PUT /api/orders/{id}/assign-driver
Content-Type: application/json

{
  "driverId": 1
}
```

#### Update Order Status
```
PUT /api/orders/{id}/status
Content-Type: application/json

{
  "status": "DELIVERED"
}
```

#### Update Order Delivery Date
```
PUT /api/orders/{id}/delivery-date
Content-Type: application/json

{
  "deliveryDate": "2023-12-25T10:00:00"
}
```

### Drivers

#### Get All Drivers
```
GET /api/drivers
```

#### Get Available Drivers
```
GET /api/drivers/available
```

#### Get Driver by ID
```
GET /api/drivers/{id}
```

#### Create Driver
```
POST /api/drivers
Content-Type: application/json

{
  "name": "John Doe",
  "licenseNumber": "DL123456",
  "phoneNumber": "0771234567",
  "email": "john.doe@example.com",
  "available": true
}
```

#### Update Driver Availability
```
PUT /api/drivers/{id}/availability
Content-Type: application/json

{
  "available": false
}
```

## RabbitMQ Integration

The system listens to the `cma.orders.queue` for incoming orders from the CMA system.

### Expected Order Message Format
```json
{
  "clientRef": "ORD-12345",
  "customerName": "John Smith",
  "deliveryAddress": "123 Main St, Colombo",
  "deliveryDate": "2023-12-25T10:00:00"
}
```

If `clientRef` is not provided, the system will generate a unique reference starting with "ORD-".

## Database Schema

### Orders Table
- `id`: Primary key
- `client_ref`: Unique order reference
- `customer_name`: Customer name
- `delivery_address`: Delivery address
- `delivery_date`: Scheduled delivery date
- `status`: Order status (RECEIVED, READY_TO_DISPATCH, DELIVERED)
- `driver_id`: Foreign key to assigned driver
- `created_at`: Order creation timestamp
- `updated_at`: Last update timestamp

### Drivers Table
- `id`: Primary key
- `name`: Driver name
- `license_number`: Unique license number
- `phone_number`: Contact number
- `email`: Email address
- `available`: Availability status
- `created_at`: Driver creation timestamp
- `updated_at`: Last update timestamp

## Configuration

Key configuration in `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/wmsdb
    username: wmsuser
    password: wmspass
  rabbitmq:
    host: localhost
    port: 5672
    username: wmsuser
    password: wmspass
```

## Example Usage

1. **Create a driver**:
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

2. **Send order to RabbitMQ** (orders will be automatically processed):
```json
{
  "clientRef": "ORD-001",
  "customerName": "Jane Smith",
  "deliveryAddress": "456 Oak St, Kandy",
  "deliveryDate": "2023-12-25T14:00:00"
}
```

3. **Assign driver to order**:
```bash
curl -X PUT http://localhost:8080/api/orders/1/assign-driver \
  -H "Content-Type: application/json" \
  -d '{"driverId": 1}'
```

4. **Mark order as delivered**:
```bash
curl -X PUT http://localhost:8080/api/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "DELIVERED"}'
```