#!/bin/bash

# Test Script for WMS System
echo "🧪 Testing WMS System..."

BASE_URL="http://localhost:8080"

echo "1. Testing Health Check..."
curl -s "$BASE_URL/api/orders" > /dev/null
if [ $? -eq 0 ]; then
    echo "✅ WMS is running"
else
    echo "❌ WMS is not running"
    exit 1
fi

echo "2. Creating a test driver..."
DRIVER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/drivers" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Driver",
    "licenseNumber": "TD123456",
    "phoneNumber": "0771234567",
    "email": "test@example.com"
  }')

DRIVER_ID=$(echo $DRIVER_RESPONSE | grep -o '"id":[0-9]*' | grep -o '[0-9]*')

if [ ! -z "$DRIVER_ID" ]; then
    echo "✅ Driver created with ID: $DRIVER_ID"
else
    echo "❌ Failed to create driver"
    echo "Response: $DRIVER_RESPONSE"
fi

echo "3. Getting all orders..."
curl -s "$BASE_URL/api/orders" | head -20

echo "4. Getting unassigned orders..."
curl -s "$BASE_URL/api/orders/unassigned" | head -20

echo "5. Getting available drivers..."
curl -s "$BASE_URL/api/drivers/available" | head -20

echo "🎉 Test completed!"