# 🍳 SmartRecipe — Backend API

> **RESTful API** cho nền tảng quản lý công thức nấu ăn và đi chợ thông minh, tích hợp trợ lý AI (Google Gemini) để gợi ý công thức từ nguyên liệu sẵn có.

---

## 📋 Mục lục

- [Tổng quan hệ thống](#-tổng-quan-hệ-thống)
- [Tech Stack](#-tech-stack)
- [Kiến trúc ứng dụng](#-kiến-trúc-ứng-dụng)
- [Cấu trúc dự án](#-cấu-trúc-dự-án)
- [Mô hình dữ liệu](#-mô-hình-dữ-liệu)
- [API Endpoints](#-api-endpoints)
- [Luồng xác thực (Auth Flow)](#-luồng-xác-thực-auth-flow)
- [Luồng AI Gợi ý Công thức](#-luồng-ai-gợi-ý-công-thức)
- [Hệ thống Caching (Redis)](#-hệ-thống-caching-redis)
- [Thiết lập & Chạy local](#-thiết-lập--chạy-local)
- [Biến môi trường](#-biến-môi-trường)

---

## 🎯 Tổng quan hệ thống

SmartRecipe Backend giải quyết bài toán **"Hôm nay nấu gì?"** bằng cách:

1. **Quản lý tủ nguyên liệu (Pantry):** Theo dõi nguyên liệu sẵn có, số lượng, hạn sử dụng và cảnh báo sắp hết.
2. **Gợi ý công thức bằng AI (Zero-Waste):** Gọi Google Gemini API với danh sách nguyên liệu trong tủ, ưu tiên nguyên liệu sắp hết hạn, để AI trả về công thức phù hợp dưới dạng JSON có cấu trúc.
3. **Tạo danh sách đi chợ thông minh:** Từ các công thức đã chọn, hệ thống tự động tính toán lượng nguyên liệu cần mua (trừ đi lượng đã có trong tủ), quy đổi đơn vị và nhóm theo kệ hàng siêu thị (9 aisles).
4. **Nhật ký nấu ăn & Mạng xã hội:** Lưu lịch sử nấu ăn, chia sẻ công thức, theo dõi, thích và bình luận.

---

## 🛠 Tech Stack

| Thành phần | Công nghệ |
|---|---|
| **Framework** | Spring Boot 4.1.0 |
| **Ngôn ngữ** | Java 21 |
| **Cơ sở dữ liệu** | MySQL 8.0 |
| **ORM** | Spring Data JPA / Hibernate |
| **Caching** | Redis 7 (Spring Data Redis) |
| **Bảo mật** | Spring Security + JWT (jjwt 0.11.5) |
| **AI Integration** | Google Gemini API (`gemini-2.0-flash`) |
| **Lưu trữ ảnh** | Cloudinary |
| **Xuất tài liệu** | Apache POI (Word `.docx`) |
| **Tiện ích** | Lombok, Jackson, spring-dotenv |
| **Container** | Docker + Docker Compose |
| **Build tool** | Maven |

---

## 🏛 Kiến trúc ứng dụng

Hệ thống áp dụng kiến trúc **Layered Architecture (3-tier)** kinh điển với Spring Boot:

```
┌─────────────────────────────────────────────────────┐
│                   CLIENT (React)                     │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP / JSON
┌──────────────────────▼──────────────────────────────┐
│               CONTROLLER LAYER (REST API)            │
│  Xử lý HTTP request, validate input, trả response   │
│  @RestController · @PreAuthorize · @Valid            │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│                 SERVICE LAYER (Business Logic)        │
│  Xử lý nghiệp vụ, gọi AI, quy đổi đơn vị           │
│  @Service · @Transactional · @Cacheable             │
└──────────┬───────────────────────────────┬──────────┘
           │                               │
┌──────────▼──────────┐    ┌───────────────▼──────────┐
│   REPOSITORY LAYER  │    │   EXTERNAL SERVICES       │
│  Truy vấn database  │    │  · Gemini AI API          │
│  Spring Data JPA    │    │  · Cloudinary (ảnh)       │
│  @Repository        │    │  · Redis (cache)          │
└──────────┬──────────┘    └───────────────────────────┘
           │
┌──────────▼──────────┐
│  MySQL 8.0 Database │
│  18 bảng · utf8mb4  │
│  utf8mb4_0900_as_ci │
└─────────────────────┘
```

---

## 📂 Cấu trúc dự án

```
smartrecipe-backend/
├── src/main/java/com/smartrecipe/smartrecipe_backend/
│   │
│   ├── config/                     # Cấu hình Spring Beans
│   │   ├── AppConfig.java          # PasswordEncoder, ModelMapper
│   │   ├── CacheConfig.java        # Redis Cache, TTL policies
│   │   ├── CloudinaryConfig.java   # Kết nối lưu trữ ảnh
│   │   ├── GeminiConfig.java       # Cấu hình Gemini AI (API key, model, daily limit)
│   │   ├── GeminiClient.java       # HTTP client gọi Gemini API, xử lý lỗi/timeout
│   │   └── WebConfig.java          # CORS configuration
│   │
│   ├── controller/                 # REST Controllers (13 controllers)
│   │   ├── AuthController.java     # POST /auth/register · /auth/login · /auth/refresh
│   │   ├── UserController.java     # GET/PUT /users (profile)
│   │   ├── FollowController.java   # POST/DELETE /users/{id}/follow
│   │   ├── RecipeController.java   # CRUD công thức, like, search, export Word
│   │   ├── CommentController.java  # CRUD bình luận
│   │   ├── TagController.java      # Quản lý tag
│   │   ├── IngredientController.java # CRUD nguyên liệu (Admin), search
│   │   ├── AisleController.java    # Quản lý kệ hàng (9 aisles)
│   │   ├── PantryController.java   # Quản lý tủ nguyên liệu của user
│   │   ├── GroceryController.java  # Danh sách đi chợ, tính toán, PDF/Word
│   │   ├── JournalController.java  # Nhật ký nấu ăn
│   │   ├── AiController.java       # AI gợi ý công thức, lưu kết quả
│   │   └── UnitConversionController.java # Quản lý quy đổi đơn vị
│   │
│   ├── service/                    # Business Logic Interfaces + Implementations
│   │   └── impl/
│   │       ├── AiServiceImpl.java  # Matching 3 lớp, rate limiting, gọi Gemini
│   │       ├── GroceryServiceImpl.java # Tính toán đi chợ, quy đổi đơn vị
│   │       ├── PantryServiceImpl.java  # Quản lý tủ lạnh, cảnh báo hết hạn
│   │       └── ...                 # Các service khác
│   │
│   ├── entity/                     # JPA Entities (18 entities)
│   │   ├── User.java, Recipe.java, Ingredient.java
│   │   ├── Aisle.java, UserPantry.java, GroceryList.java
│   │   ├── GroceryItem.java, AiSuggestionLog.java
│   │   └── ...
│   │
│   ├── repository/                 # Spring Data JPA Repositories (18 repos)
│   │
│   ├── dto/                        # Data Transfer Objects
│   │   ├── request/                # Request bodies
│   │   └── response/               # Response payloads
│   │
│   ├── enums/                      # Enumerations
│   │   ├── RecipeStatus.java       # DRAFT, PRIVATE, PUBLIC, DELETED
│   │   ├── GroceryListStatus.java  # ACTIVE, COMPLETED
│   │   └── ...
│   │
│   ├── exception/                  # Custom Exceptions + Global Handler
│   │   ├── GlobalExceptionHandler.java
│   │   ├── AiServiceException.java
│   │   ├── BadRequestException.java
│   │   ├── ResourceNotFoundException.java
│   │   ├── DuplicateResourceException.java
│   │   ├── RateLimitExceededException.java
│   │   └── UnauthorizedException.java
│   │
│   └── security/                   # JWT Authentication
│       ├── SecurityConfig.java     # Cấu hình Spring Security, filter chain
│       ├── JwtProvider.java        # Generate, parse, validate JWT tokens
│       ├── JwtAuthFilter.java      # Servlet filter kiểm tra token mỗi request
│       └── UserDetailsServiceImpl.java
│
├── src/main/resources/
│   └── application.yaml            # Cấu hình datasource, Redis, JWT, Gemini
│
├── .docker/
│   └── Dockerfile                  # Multi-stage build cho production
│
└── pom.xml                         # Maven dependencies
```

---

## 🗃 Mô hình dữ liệu

Hệ thống gồm **18 bảng** với collation `utf8mb4_0900_as_ci` (phân biệt dấu tiếng Việt, không phân biệt hoa/thường).

```
┌─────────┐     ┌─────────────┐     ┌──────────────────┐
│  users  │────<│   recipes   │────<│  recipe_steps    │
└────┬────┘     └──────┬──────┘     └──────────────────┘
     │                 │
     │           ┌─────▼──────────┐     ┌─────────────┐
     │           │recipe_ingredients│───>│ ingredients │
     │           └────────────────┘     └──────┬──────┘
     │                                         │
     │    ┌─────────────┐              ┌───────▼──────┐
     │    │ user_pantry │─────────────>│    aisles    │
     ├───<│             │              │   (9 kệ)     │
     │    └─────────────┘              └──────────────┘
     │
     │    ┌──────────────┐     ┌──────────────────────┐
     ├───<│ grocery_lists│────<│    grocery_items     │
     │    └──────────────┘     └──────────────────────┘
     │
     │    ┌─────────────────┐     ┌────────────────────┐
     ├───<│ai_suggestion_logs│    │  unit_conversions  │
     │    └─────────────────┘     └────────────────────┘
     │
     ├───<│ cooking_journals │
     ├───<│ follows          │
     └───<│ recipe_likes     │
          │ recipe_comments  │
          │ tags / recipe_tags│
```

**9 kệ hàng (Aisles)** theo lối siêu thị Việt Nam:

| ID | Tên kệ | Số nguyên liệu |
|---|---|---|
| 1 | Rau củ | 74 |
| 2 | Thịt & Gia cầm | 47 |
| 3 | Hải sản | 28 |
| 4 | Gia vị & Nước chấm | 36 |
| 5 | Đồ khô & Gạo | 32 |
| 6 | Sữa & Trứng | 19 |
| 7 | Trái cây | 34 |
| 8 | Dầu mỡ & Chất béo | 9 |
| 9 | Các loại Hạt | 11 |

**Tổng: 290 nguyên liệu** với dữ liệu dinh dưỡng từ USDA FoodData Central.

---

## 🔌 API Endpoints

> **Base URL:** `http://localhost:8080/api/v1`
> 
> 🔓 = Public · 🔐 = Cần JWT · 👑 = Chỉ ADMIN

### 🔑 Xác thực — `/auth`

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| `POST` | `/auth/register` | 🔓 | Đăng ký tài khoản mới |
| `POST` | `/auth/login` | 🔓 | Đăng nhập, nhận `accessToken` + `refreshToken` |
| `POST` | `/auth/refresh` | 🔓 | Làm mới access token bằng refresh token |

### 👤 Người dùng — `/users`

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| `GET` | `/users/me` | 🔐 | Xem thông tin cá nhân |
| `PUT` | `/users/me` | 🔐 | Cập nhật profile (tên, avatar, bio) |
| `POST` | `/users/{id}/follow` | 🔐 | Theo dõi người dùng |
| `DELETE` | `/users/{id}/follow` | 🔐 | Bỏ theo dõi |
| `GET` | `/users/{id}/followers` | 🔐 | Danh sách người theo dõi |
| `GET` | `/users/{id}/following` | 🔐 | Danh sách đang theo dõi |

### 📖 Công thức — `/recipes`

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| `GET` | `/recipes` | 🔐 | Tất cả công thức PUBLIC (có phân trang) |
| `GET` | `/recipes/my` | 🔐 | Công thức của tôi |
| `GET` | `/recipes/{id}` | 🔐 | Chi tiết công thức |
| `POST` | `/recipes` | 🔐 | Tạo công thức mới |
| `PUT` | `/recipes/{id}` | 🔐 | Cập nhật công thức (chủ sở hữu) |
| `DELETE` | `/recipes/{id}` | 🔐 | Xóa mềm (set status = DELETED) |
| `POST` | `/recipes/{id}/like` | 🔐 | Thích / Bỏ thích |
| `GET` | `/recipes/search` | 🔐 | Tìm kiếm theo tên, tag, nguyên liệu |
| `POST` | `/recipes/{id}/export/word` | 🔐 | Xuất công thức ra file `.docx` |

### 🧄 Nguyên liệu — `/ingredients`

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| `GET` | `/ingredients` | 🔐 | Tất cả nguyên liệu (phân trang) |
| `GET` | `/ingredients/search?q=` | 🔐 | Tìm kiếm nguyên liệu |
| `GET` | `/ingredients/aisle/{id}` | 🔐 | Lọc theo kệ hàng |
| `POST` | `/ingredients` | 👑 | Tạo nguyên liệu (kèm dinh dưỡng đầy đủ) |
| `POST` | `/ingredients/quick` | 🔐 | User thêm nhanh (name + aisleId, calo = 0) |
| `PUT` | `/ingredients/{id}` | 👑 | Cập nhật thông tin + dinh dưỡng |
| `DELETE` | `/ingredients/{id}` | 👑 | Xóa nguyên liệu |

### 🧊 Tủ nguyên liệu — `/pantry`

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| `GET` | `/pantry` | 🔐 | Xem tủ nguyên liệu (nhóm theo kệ) |
| `POST` | `/pantry` | 🔐 | Thêm nguyên liệu vào tủ |
| `PUT` | `/pantry/{id}` | 🔐 | Cập nhật số lượng / hạn dùng |
| `DELETE` | `/pantry/{id}` | 🔐 | Xóa khỏi tủ |
| `GET` | `/pantry/expiring` | 🔐 | Nguyên liệu sắp hết hạn |

### 🛒 Danh sách đi chợ — `/grocery`

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| `GET` | `/grocery/lists` | 🔐 | Danh sách tất cả grocery list |
| `POST` | `/grocery/lists` | 🔐 | Tạo grocery list mới |
| `POST` | `/grocery/lists/{id}/recipes` | 🔐 | Thêm công thức vào list (tự tính nguyên liệu) |
| `GET` | `/grocery/lists/{id}/items` | 🔐 | Xem items đã tổng hợp |
| `PATCH` | `/grocery/items/{id}/bought` | 🔐 | Đánh dấu đã mua |
| `POST` | `/grocery/lists/{id}/complete` | 🔐 | Hoàn thành → tự động cập nhật pantry |
| `GET` | `/grocery/lists/{id}/export` | 🔐 | Xuất danh sách ra Word |

### 🤖 Trợ lý AI — `/ai`

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| `GET` | `/ai/suggest/pantry` | 🔐 | Gợi ý công thức từ tủ lạnh (Zero-Waste) |
| `POST` | `/ai/suggest/custom` | 🔐 | Gợi ý từ nguyên liệu nhập tay |
| `POST` | `/ai/save/{logId}` | 🔐 | Lưu gợi ý AI thành công thức thật |
| `GET` | `/ai/history` | 🔐 | Lịch sử gọi AI |
| `GET` | `/ai/remaining` | 🔐 | Số lượt AI còn lại trong ngày |

### 📔 Nhật ký nấu ăn — `/journal`

| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| `GET` | `/journal` | 🔐 | Lịch sử nấu ăn của tôi |
| `POST` | `/journal` | 🔐 | Ghi nhận lần nấu mới |
| `PUT` | `/journal/{id}` | 🔐 | Cập nhật ghi chú, đánh giá |
| `DELETE` | `/journal/{id}` | 🔐 | Xóa nhật ký |

---

## 🔐 Luồng xác thực (Auth Flow)

Hệ thống dùng **JWT với Access Token + Refresh Token**:

```
Client                          Backend                      Redis
  │                                │                            │
  │── POST /auth/login ───────────>│                            │
  │                                │ Verify credentials         │
  │                                │ Generate accessToken (24h) │
  │                                │ Generate refreshToken (7d) │
  │                                │── Store refreshToken ─────>│
  │<── { accessToken, refreshToken}│                            │
  │                                │                            │
  │── GET /api/v1/... ────────────>│                            │
  │   Header: Bearer <accessToken> │                            │
  │                                │ JwtAuthFilter validates    │
  │                                │ → SecurityContext set      │
  │<── Response (200 OK) ──────────│                            │
  │                                │                            │
  │ (accessToken hết hạn)          │                            │
  │── POST /auth/refresh ─────────>│                            │
  │   { refreshToken }             │── Check token ────────────>│
  │                                │<── Valid ──────────────────│
  │                                │ Issue new accessToken      │
  │<── { accessToken } ───────────-│                            │
```

---

## 🤖 Luồng AI Gợi ý Công thức

```
User gọi GET /ai/suggest/pantry
         │
         ▼
[Rate Limit Check]  ← Redis: đếm số lượt trong ngày
  Vượt quá 10 lần? → 429 Too Many Requests
         │
         ▼
[Lấy dữ liệu Pantry]
  Sắp xếp theo expiry_date ASC (ưu tiên sắp hết hạn)
  Format: "300g Thịt heo ba chỉ, 2 quả Trứng gà, ..."
         │
         ▼
[Gọi Gemini API]  ← GeminiClient.generate(systemPrompt, userPrompt)
  Model: gemini-2.0-flash
  Config: temperature=0.7, maxOutputTokens=4096
  Response: JSON với ingredients, steps, nutrition
         │
         ▼
[Lưu vào ai_suggestion_logs]  ← Lưu input + output để tái sử dụng
         │
         ▼
[Trả về AiSuggestResponse]  ← Hiển thị cho user xem trước

User xác nhận → POST /ai/save/{logId}
         │
         ▼
[3-Layer Ingredient Matching]
  ┌─────────────────────────────────────┐
  │ Lớp 1: Exact Match                  │
  │  findFirstByNameIgnoreCase()         │
  │  "Thịt heo xay" → khớp luôn ✓      │
  └─────────────────┬───────────────────┘
                    │ Không tìm thấy
  ┌─────────────────▼───────────────────┐
  │ Lớp 2: Token Match                  │
  │  Tách token: ["thịt","heo","xay",   │
  │               "70%","nạc"]          │
  │  So với 290 nguyên liệu trong DB    │
  │  "Thịt heo xay" → 3 tokens khớp ✓  │
  └─────────────────┬───────────────────┘
                    │ Không đủ điểm (< 2 tokens)
  ┌─────────────────▼───────────────────┐
  │ Lớp 3: Auto-create                  │
  │  Tạo nguyên liệu mới                │
  │  calories = 0 (cờ chờ admin review) │
  │  baseUnit = "g"                      │
  └─────────────────────────────────────┘
         │
         ▼
[Tạo Recipe + RecipeIngredients + RecipeSteps]
[Trả về RecipeResponse]
```

---

## 🧠 Hệ thống Caching (Redis)

| Cache Key Pattern | Nội dung | TTL |
|---|---|---|
| `ai:ratelimit:{userId}` | Số lượt AI đã dùng trong ngày | Đến hết ngày (EOD) |
| `pantry:{userId}` | Danh sách nguyên liệu trong tủ | 10 phút |
| `ingredients:all` | Toàn bộ 290 nguyên liệu | 60 phút |
| `recipes:public` | Danh sách công thức PUBLIC | 5 phút |

Cache bị **invalidate** tự động khi dữ liệu liên quan thay đổi (ví dụ: thêm nguyên liệu vào pantry sẽ xóa cache `pantry:{userId}`).

---

## ⚙️ Thiết lập & Chạy local

### Yêu cầu

- Java 21+
- Maven 3.9+
- Docker Desktop

### 1. Clone và cấu hình biến môi trường

```bash
git clone <repo-url>
cd smartrecipe-backend
cp .env.example .env   # Điền GEMINI_API_KEY và các giá trị khác
```

### 2. Khởi động MySQL và Redis (Docker)

```bash
# Chạy từ thư mục gốc của project
cd ..
docker-compose up -d mysql-db redis-cache
```

> Lần đầu khởi động, MySQL sẽ tự chạy `sql/init_database.sql` để tạo 18 bảng + 9 kệ hàng + 6 tags.

### 3. Nạp dữ liệu nguyên liệu (290 dòng từ USDA)

```powershell
docker cp ../sql/seed_ingredients.sql smartrecipe-mysql:/tmp/seed.sql
docker exec smartrecipe-mysql sh -c 'mysql -uroot -proot --default-character-set=utf8mb4 smart_recipe_db < /tmp/seed.sql'
```

### 4. Chạy Backend

```bash
mvn spring-boot:run
```

API sẽ chạy tại `http://localhost:8080`.

### 5. Chạy Test

```bash
./mvnw test
```

38 test (JUnit 5 + Mockito), chạy độc lập — không cần MySQL/Redis đang bật:

| Test class | Số ca | Phạm vi |
|---|---|---|
| `AiServiceImplTest` | 14 | Rate limit, `suggestFromInput`, khớp nguyên liệu, `getHistory` |
| `PantryServiceImplTest` | 10 | Trừ kho FEFO (hết hạn trước dùng trước) |
| `RecipeServiceImplTest` | 5 | CRUD, phân quyền chủ sở hữu |
| `GroceryServiceImplTest` | 4 | Tổng cần − đã có = cần mua |
| `UnitNormalizationServiceTest` | 4 | Quy đổi đơn vị qua BFS graph |
| `BackendApplicationTests` | 1 | Context load |

---

## 🔧 Biến môi trường

Tạo file `.env` trong thư mục `smartrecipe-backend/`:

```env
# Google Gemini AI
GEMINI_API_KEY=your_gemini_api_key_here

# JWT Security
JWT_SECRET=your_very_long_secret_key_here_at_least_256_bits
JWT_EXPIRATION=86400000         # 24 giờ (ms)
JWT_REFRESH_EXPIRATION=604800000 # 7 ngày (ms)

# Cloudinary (lưu trữ ảnh)
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

> **Lưu ý:** Các giá trị mặc định trong `application.yaml` sẽ được dùng nếu biến môi trường không tồn tại — **không dùng defaults này trong production**.

---

## 📊 Quyền truy cập theo Role

| Tính năng | USER | ADMIN |
|---|---|---|
| Xem/Tìm công thức | ✅ | ✅ |
| Tạo/Sửa/Xóa công thức của mình | ✅ | ✅ |
| Quản lý tủ nguyên liệu | ✅ | ✅ |
| Tạo danh sách đi chợ | ✅ | ✅ |
| Sử dụng AI gợi ý | ✅ (10 lần/ngày) | ✅ |
| Tạo nguyên liệu (full dinh dưỡng) | ❌ | ✅ |
| Thêm nguyên liệu nhanh (calo = 0) | ✅ | ✅ |
| Sửa/Xóa nguyên liệu chuẩn | ❌ | ✅ |
| Quản lý kệ hàng (Aisles) | ❌ | ✅ |

---

*Xây dựng với ❤️ — SmartRecipe Backend v0.0.1*
