@echo off
REM Test Script for WMS System - Windows Version
echo 🧪 Testing WMS System...

set BASE_URL=http://localhost:8080

echo 1. Testing Health Check...
curl -s "%BASE_URL%/api/orders" >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ WMS is running
) else (
    echo ❌ WMS is not running
    exit /b 1
)

echo 2. Creating a test driver...
curl -s -X POST "%BASE_URL%/api/drivers" ^
  -H "Content-Type: application/json" ^
  -d "{\"name\": \"Test Driver\",\"licenseNumber\": \"TD123456\",\"phoneNumber\": \"0771234567\",\"email\": \"test@example.com\"}"

echo.
echo 3. Getting all orders...
curl -s "%BASE_URL%/api/orders"

echo.
echo 4. Getting unassigned orders...
curl -s "%BASE_URL%/api/orders/unassigned"

echo.
echo 5. Getting available drivers...
curl -s "%BASE_URL%/api/drivers/available"

echo.
echo 🎉 Test completed!