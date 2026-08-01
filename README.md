# Smart Recipe Backend ⚙️

Đây là hệ thống API Server (Backend) của dự án **Smart Recipe & Grocery Platform**, được xây dựng dựa trên kiến trúc **N-Tier (Đa tầng)** kết hợp với Java Spring Boot.

## 🛠 Công nghệ Cốt lõi
- **Ngôn ngữ**: Java 21
- **Framework**: Spring Boot (Web, Data JPA, Security, Validation)
- **Cơ sở dữ liệu**: MySQL (kết nối qua JDBC)
- **Cache & Session**: Redis
- **Bảo mật**: JWT (JSON Web Tokens)
- **Build tool**: Maven

## 📂 Cấu trúc Thư mục (Package Structure)
Kiến trúc mã nguồn tuân thủ chặt chẽ Separation of Concerns (SoC):
```text
src/main/java/com/smartrecipe/smartrecipe_backend/
├── config/       # Cấu hình Spring (CORS, Swagger, Redis, Security, v.v.)
├── controller/   # Chứa các REST API Endpoints, tiếp nhận HTTP Request
├── dto/          # Data Transfer Objects (Request/Response payload)
├── entity/       # Lớp đối tượng ánh xạ trực tiếp xuống Database (JPA/Hibernate)
├── enums/        # Các Enum dùng chung (Role, Status, Difficulty...)
├── exception/    # Quản lý lỗi tập trung (GlobalExceptionHandler)
├── repository/   # Giao tiếp với Database (Spring Data JPA)
├── security/     # Logic mã hoá JWT, Filters phân quyền
└── service/      # Business Logic cốt lõi của hệ thống
```

## 🚀 Hướng dẫn Chạy (Run Locally)

### 1. Đảm bảo Database đã chạy
Backend yêu cầu MySQL (port 3306) và Redis (port 6379) phải đang chạy. Nếu bạn dùng Docker Compose ở thư mục gốc:
```bash
cd ..
docker-compose up -d mysql-db redis-cache
```

### 2. Cấu hình (application.yaml)
Cấu hình kết nối Database mặc định nằm tại `src/main/resources/application.yaml`. Hệ thống mặc định trỏ tới `localhost:3306` (kết nối ra port của Docker MySQL) và `localhost:6379` (Redis).

### 3. Biên dịch và Chạy
Sử dụng Maven Wrapper có sẵn trong thư mục này:
```bash
# Xóa build cũ và biên dịch lại
./mvnw clean package -DskipTests

# Chạy ứng dụng
./mvnw spring-boot:run
```
> Server sẽ khởi động tại địa chỉ: `http://localhost:8080`

## 🐳 Docker Build
Bạn cũng có thể build Backend thành một Docker image độc lập (sử dụng Multi-stage build để tối ưu dung lượng):
```bash
docker build -t smartrecipe-backend .
docker run -p 8080:8080 smartrecipe-backend
```
