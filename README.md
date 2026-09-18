# 🍳 SmartRecipe — Backend RESTful API & AI Microservices

> **RESTful API Core Engine** cho nền tảng quản lý công thức nấu ăn, tủ nguyên liệu thông minh (Pantry Zero-Waste) và tối ưu hóa danh sách đi chợ. Tích hợp sâu **Google Gemini AI**, cơ chế quy đổi đơn vị theo đồ thị BFS, tự động trừ kho theo hạn dùng (FEFO) và hệ thống quản trị chuyên sâu (Admin Dashboard).

---

## 📋 Mục lục

- [Tổng quan kiến trúc & Điểm nhấn](#-tổng-quan-kiến-trúc--điểm-nhấn)
- [Tech Stack & Thư viện](#-tech-stack--thư-viện)
- [Sơ đồ kiến trúc Layered Architecture](#-sơ-đồ-kiến-trúc-layered-architecture)
- [Cấu trúc mã nguồn](#-cấu-trúc-mã-nguồn)
- [Mô hình dữ liệu (19 Bảng)](#-mô-hình-dữ-liệu-19-bảng)
- [Tài liệu API chi tiết (15 Controllers)](#-tài-liệu-api-chi-tiết-15-controllers)
  - [1. Xác thực & Quản lý phiên (`/auth`)](#1-xác-thực--quản-lý-phiên-auth)
  - [2. Người dùng & Mạng xã hội (`/users`)](#2-người-dùng--mạng-xã-hội-users)
  - [3. Công thức nấu ăn (`/recipes`)](#3-công-thức-nấu-ăn-recipes)
  - [4. Bình luận công thức (`/comments`)](#4-bình-luận-công-thức-comments)
  - [5. Tủ nguyên liệu thông minh (`/pantry`)](#5-tủ-nguyên-liệu-thông-minh-pantry)
  - [6. Danh sách đi chợ (`/grocery`)](#6-danh-sách-đi-chợ-grocery)
  - [7. Trợ lý AI Gemini (`/ai`)](#7-trợ-lý-ai-gemini-ai)
  - [8. Nhật ký nấu ăn (`/journals`)](#8-nhật-ký-nấu-ăn-journals)
  - [9. Trung tâm Thông báo (`/notifications`)](#9-trung-tâm-thông-báo-notifications)
  - [10. Nguyên liệu dùng chung (`/ingredients`)](#10-nguyên-liệu-dùng-chung-ingredients)
  - [11. Quầy hàng siêu thị (`/aisles`)](#11-quầy-hàng-siêu-thị-aisles)
  - [12. Thẻ phân loại (`/tags`)](#12-thẻ-phân-loại-tags)
  - [13. Quy đổi đơn vị (`/unit-conversions`)](#13-quy-đổi-đơn-vị-unit-conversions)
  - [14. Quản trị hệ thống nâng cao (`/admin`)](#14-quản-trị-hệ-thống-nâng-cao-admin)
- [Thuật toán & Cơ chế cốt lõi](#-thuật-toán--cơ-chế-cốt-lõi)
  - [Thuật toán Khớp nguyên liệu 3 lớp](#1-thuật-toán-khớp-nguyên-liệu-3-lớp-ingredient-matching)
  - [Cơ chế trừ kho thông minh FEFO](#2-cơ-chế-trừ-kho-thông-minh-fefo-first-expired-first-out)
  - [Quy đổi đơn vị bằng Đồ thị BFS](#3-quy-đổi-đơn-vị-đa-bước-bằng-đồ-thị-bfs)
  - [Cơ chế Quản lý Token & OTP Email](#4-bảo-mật-jwt--xác-thực-otp-qua-email)
- [Chiến lược Caching (Redis)](#-chiến-lược-caching-redis)
- [Kiểm thử tự động (Unit Tests)](#-kiểm-thử-tự-động-unit-tests)
- [Cài đặt & Chạy cục bộ (Local Development)](#-cài-đặt--chạy-cục-bộ-local-development)
- [Biến môi trường](#-biến-môi-trường)
- [Docker & CI/CD Pipeline](#-docker--cicd-pipeline)

---

## 🎯 Tổng quan kiến trúc & Điểm nhấn

SmartRecipe Backend đóng vai trò bộ não điều phối toàn bộ luồng dữ liệu, xử lý nghiệp vụ ẩm thực phức tạp:

1. **Zero-Waste AI Engine:** Tích hợp mô hình `gemini-2.0-flash` / `gemini-3-flash-preview` đọc trạng thái thực tế của tủ lạnh, ưu tiên nguyên liệu cận date để sáng tạo công thức chống lãng phí kèm thông số dinh dưỡng chuẩn xác.
2. **Quy đổi đơn vị & Trừ kho FEFO:** Chuẩn hóa mọi đơn vị dân gian (thìa, muỗng, chén, quả, bát) về đơn vị đo lường gốc (`g`, `ml`) bằng đồ thị đa bước BFS; tự động trừ các lô nguyên liệu hết hạn trước khi người dùng thực hiện nấu.
3. **Tính toán đi chợ thông minh:** Nhập món muốn nấu $\rightarrow$ Hệ thống tự trừ lượng đang có trong tủ $\rightarrow$ Tính lượng thiếu hụt $\rightarrow$ Phân loại tự động vào 9 kệ hàng siêu thị Việt Nam.
4. **Trung tâm Quản trị Chuyên sâu (Admin Portal):** Cung cấp 15 API quản trị toàn diện: Dashboard KPI chỉ số lãng phí, kiểm duyệt công thức, kiểm duyệt nguyên liệu mới sinh bởi AI, phân quyền người dùng có chốt an toàn chống hạ quyền Admin duy nhất, và giám sát lịch sử prompt AI.

---

## 🛠 Tech Stack & Thư viện

| Phân tầng | Công nghệ / Thư viện | Phiên bản | Vai trò & Mục đích |
|---|---|---|---|
| **Core Framework** | Spring Boot | `4.1.0` | Nền tảng microservices RESTful API |
| **Runtime** | Java LTS | `21` | Tận dụng Virtual Threads, Pattern Matching, Record |
| **Cơ sở dữ liệu** | TiDB Cloud / MySQL | `8.0` | Lưu trữ quan hệ ACID, hỗ trợ phân tán quy mô lớn |
| **ORM / Persistence** | Spring Data JPA (Hibernate) | `6.x` | Mapping thực thể quan hệ, custom native queries |
| **In-memory Cache** | Redis 7 (Spring Data Redis) | `7.x` | Cache tầng ứng dụng, Rate Limit, OTP session |
| **Xác thực & Bảo mật** | Spring Security + JJWT | `0.11.5` | Stateless JWT token, filter chain, mã hóa BCrypt |
| **Trí tuệ nhân tạo (AI)** | Google Gemini API | `v1beta` | Gợi ý thực đơn, trích xuất cấu trúc JSON có schema |
| **Dịch vụ Email** | Spring Boot Starter Mail | `4.1.0` | Gửi mã OTP khôi phục mật khẩu qua Gmail SMTP |
| **Lưu trữ đa phương tiện** | Cloudinary Java SDK | `1.36.0` | CDN lưu trữ và tối ưu hóa hình ảnh món ăn, avatar |
| **Xuất tài liệu** | Apache POI OOXML | `5.2.3` | Tạo và tải xuống file Word (`.docx`) công thức & đi chợ |
| **Kiểm thử** | JUnit 5 + Mockito + AssertJ | `5.x` | 48 ca kiểm thử đơn vị độc lập hoàn toàn với database |
| **Containerization** | Docker Multi-stage | Alpine | Đóng gói bản build JRE 21 tối ưu kích thước |

---

## 🏛 Sơ đồ kiến trúc Layered Architecture

Hệ thống được thiết kế theo mô hình **Phân tầng hướng dịch vụ (3-Tier Layered Architecture)** đảm bảo tính mở rộng và độc lập:

```
┌───────────────────────────────────────────────────────────────────────────┐
│                    CLIENT APPS (React 19 / Mobile / Web)                   │
└─────────────────────────────────────┬─────────────────────────────────────┘
                                      │ HTTPS / JSON Payload (Bearer JWT)
┌─────────────────────────────────────▼─────────────────────────────────────┐
│                       SECURITY & CONTROLLER LAYER                         │
│  - JwtAuthFilter (Interception, Token Extraction & Validation)            │
│  - 15 REST Controllers (@RestController, @PreAuthorize, @Valid)           │
│  - GlobalExceptionHandler (@ControllerAdvice -> Standard ApiResponse)     │
└─────────────────────────────────────┬─────────────────────────────────────┘
                                      │ DTOs (Request / Response)
┌─────────────────────────────────────▼─────────────────────────────────────┐
│                        BUSINESS SERVICE LAYER                             │
│  - 18 Services + Implementations (@Service, @Transactional)               │
│  - UnitNormalizationService (BFS Graph Unit Conversion)                   │
│  - PantryServiceImpl (FEFO Deduction Engine)                              │
│  - AiServiceImpl (3-Layer Matching & Gemini Orchestrator)                 │
│  - Redis Cache Interceptor (@Cacheable, @CacheEvict)                      │
└──────────┬──────────────────────────┬──────────────────────────┬──────────┘
           │                          │                          │
┌──────────▼──────────┐    ┌──────────▼──────────┐    ┌──────────▼──────────┐
│   REPOSITORY LAYER  │    │     REDIS CACHE     │    │  EXTERNAL SERVICES  │
│  - 19 JPA Repos     │    │  - Master Data TTL  │    │  - Google Gemini AI │
│  - Pagination       │    │  - AI Rate Limit    │    │  - Cloudinary CDN   │
│  - Native Queries   │    │  - OTP Storage (5m) │    │  - Gmail SMTP       │
└──────────┬──────────┘    └─────────────────────┘    └─────────────────────┘
           │
┌──────────▼──────────────────────────────────────┐
│       DATABASE (TiDB Cloud / MySQL 8.0)         │
│  - 19 Tables · utf8mb4_unicode_ci collation     │
│  - Foreign Keys, Indexes, Cascades              │
└─────────────────────────────────────────────────┘
```

---

## 📂 Cấu trúc mã nguồn

```
smartrecipe-backend/
├── src/main/java/com/smartrecipe/smartrecipe_backend/
│   │
│   ├── config/                           # Cấu hình Spring Beans & External Adapters
│   │   ├── AppConfig.java                # PasswordEncoder (BCrypt), ObjectMapper JSR310
│   │   ├── CacheConfig.java              # Cấu hình Redis CacheManager, TTLs theo domain
│   │   ├── CloudinaryConfig.java         # Khởi tạo kết nối lưu trữ ảnh Cloudinary
│   │   ├── GeminiClient.java             # HTTP client gọi Gemini API, xử lý timeout/error
│   │   ├── GeminiConfig.java             # Quản lý cấu hình Gemini AI (key, model, limit)
│   │   └── WebConfig.java                # CORS origins, Allowed Headers & Methods
│   │
│   ├── controller/                       # 15 REST API Controllers
│   │   ├── AdminController.java          # KPI Dashboard, duyệt công thức, nguyên liệu, users, AI logs
│   │   ├── AiController.java             # Gợi ý Zero-Waste, gợi ý tùy biến, lưu kết quả AI
│   │   ├── AisleController.java          # Danh mục quầy hàng (9 kệ siêu thị)
│   │   ├── AuthController.java           # Đăng ký, đăng nhập, refresh token, OTP quên mật khẩu
│   │   ├── CommentController.java        # Bình luận công thức đa tầng
│   │   ├── FollowController.java         # Theo dõi tác giả & quản lý followers
│   │   ├── GroceryController.java        # Quản lý danh sách đi chợ, sinh tự động từ pantry/công thức
│   │   ├── IngredientController.java     # Quản lý nguyên liệu chuẩn, thêm nhanh cho user
│   │   ├── JournalController.java        # Nhật ký nấu ăn, tự động kích hoạt trừ kho FEFO
│   │   ├── NotificationController.java   # Thông báo người dùng (thích, bình luận, theo dõi)
│   │   ├── PantryController.java         # Quản lý tủ lạnh, cảnh báo cận hạn, dọn đồ quá hạn
│   │   ├── RecipeController.java         # CRUD công thức, tìm kiếm, nhân bản (clone), xuất Word
│   │   ├── TagController.java            # Quản lý thẻ chủ đề (Món Chay, Eat Clean, Quick...)
│   │   ├── UnitConversionController.java # Quản lý quy đổi đơn vị đo lường ẩm thực
│   │   └── UserController.java           # Quản lý profile, đổi mật khẩu, avatar, xóa tài khoản
│   │
│   ├── entity/                           # 19 JPA Entities
│   │   ├── AiSuggestionLog.java          # Lịch sử và payload JSON gọi AI Gemini
│   │   ├── Aisle.java                    # Quầy hàng siêu thị
│   │   ├── CookingJournal.java           # Lịch sử nấu nướng & hình ảnh thực tế
│   │   ├── Follow.java                   # Liên kết theo dõi người dùng
│   │   ├── GroceryItem.java              # Chi tiết nguyên liệu cần mua trong danh sách
│   │   ├── GroceryList.java              # Danh sách đi chợ cha
│   │   ├── GroceryListRecipe.java        # Liên kết giữa danh sách đi chợ và công thức chọn nấu
│   │   ├── Ingredient.java               # Bảng nguyên liệu tổng (dinh dưỡng USDA, baseUnit)
│   │   ├── Notification.java             # Thông báo hoạt động người dùng
│   │   ├── Recipe.java                   # Công thức nấu ăn tổng thể
│   │   ├── RecipeComment.java            # Bình luận và phản hồi
│   │   ├── RecipeIngredient.java         # Thành phần nguyên liệu trong từng công thức
│   │   ├── RecipeLike.java               # Lượt yêu thích công thức
│   │   ├── RecipeStep.java               # Các bước chế biến từng công thức kèm ảnh
│   │   ├── RecipeTag.java                # Bảng liên kết N-N giữa Recipe và Tag
│   │   ├── Tag.java                      # Thẻ phân loại công thức
│   │   ├── UnitConversion.java           # Hệ số quy đổi đơn vị theo nguyên liệu
│   │   ├── User.java                     # Người dùng hệ thống (USER, ADMIN)
│   │   └── UserPantry.java               # Kho lưu trữ nguyên liệu cá nhân theo ngày hết hạn
│   │
│   ├── repository/                       # 19 Spring Data JPA Repositories
│   ├── service/                          # Service Interfaces & Implementations
│   │   ├── UnitNormalizationService.java # Giải thuật BFS quy đổi đơn vị theo đồ thị
│   │   ├── RecipeExportService.java      # Render file Word (.docx) chuyên nghiệp với POI
│   │   └── impl/                         # 16 Service Implementations
│   │
│   ├── dto/                              # Request & Response Payload Data Transfer Objects
│   ├── enums/                            # Định nghĩa hằng số (Role, RecipeStatus, Difficulty...)
│   ├── exception/                        # Custom Exceptions & Global Exception Handler
│   └── security/                         # JWT Security Filter, UserDetails, SecurityConfig
│
├── src/main/resources/
│   └── application.yaml                  # Cấu hình Spring Profiles, Datasource, Redis, Gemini, Mail
├── src/test/java/                        # 48 Unit Test Suites (JUnit 5 + Mockito)
├── .docker/Dockerfile                    # Multi-stage build image
└── pom.xml                               # Quản lý thư viện Maven
```

---

## 🗃 Mô hình dữ liệu (19 Bảng)

Cơ sở dữ liệu được thiết kế theo chuẩn hóa bậc cao, sử dụng bộ mã ký tự `utf8mb4` và collation `utf8mb4_unicode_ci` (tương thích 100% cả TiDB Cloud và MySQL 8.0), bảo toàn chính xác dấu tiếng Việt:

```
┌─────────────────┐       ┌──────────────────────┐       ┌────────────────────────┐
│      users      │───1:N─│       recipes        │───1:N─│      recipe_steps      │
│ (id, username,  │       │(id, title, author_id,│       │(id, recipe_id, step_no,│
│  email, role)   │       │ status, difficulty)  │       │ instruction, image_url)│
└────────┬────────┘       └──────────┬───────────┘       └────────────────────────┘
         │                           │
         │                   1:N     │
         │         ┌─────────────────▼──┐                 ┌──────────────────────┐
         │         │ recipe_ingredients │──────N:1───────>│     ingredients      │
         │         │ (amount, unit,     │                 │ (id, name, base_unit,│
         │         │  ingredient_id)    │                 │  calories, aisle_id) │
         │         └────────────────────┘                 └──────────┬───────────┘
         │                                                           │
         │         ┌────────────────────┐                            │ N:1
         ├──1:N───<│    user_pantry     │─────────N:1────────────────┤
         │         │(quantity, unit,    │                            │
         │         │ expiry_date, user) │                 ┌──────────▼───────────┐
         │         └────────────────────┘                 │        aisles        │
         │                                                │(id, name, desc) [9]  │
         │         ┌────────────────────┐                 └──────────────────────┘
         ├──1:N───<│   grocery_lists    │
         │         └─────────┬──────────┘
         │                   │ 1:N
         │         ┌─────────▼──────────┐
         │         │   grocery_items    │
         │         │(amount, unit, is_  │
         │         │ purchased, aisle)  │
         │         └────────────────────┘
         │
         ├──1:N───<│ ai_suggestion_logs │ (input_ingredients, output_response, saved_recipe_id)
         ├──1:N───<│ cooking_journals   │ (recipe_id, rating, notes, actual_servings, photo_url)
         ├──1:N───<│ notifications      │ (recipient_id, actor_id, type, is_read)
         ├──1:N───<│ follows            │ (follower_id, following_id)
         ├──1:N───<│ recipe_likes       │ (recipe_id, user_id)
         ├──1:N───<│ recipe_comments    │ (recipe_id, user_id, content, parent_id)
         │         └────────────────────┘
         │
         │         ┌────────────────────┐                 ┌──────────────────────┐
         └─────────│  unit_conversions  │                 │    tags / rec_tags   │
                   │(from_unit, to_unit,│                 │(Phân loại chuyên đề) │
                   │ factor, ingredient)│                 └──────────────────────┘
                   └────────────────────┘
```

### 9 Kệ hàng (Aisles) Tiêu chuẩn

Hệ thống định hình sẵn 9 kệ hàng đại diện cho chuỗi cung ứng siêu thị thực phẩm Việt Nam:

| ID | Tên kệ hàng | Mô tả phân loại | Số lượng mẫu |
|:---:|---|---|:---:|
| **1** | Rau củ (Produce) | Rau xanh, củ quả tươi, nấm tươi, thảo mộc gia vị tươi | 74 |
| **2** | Thịt & Gia cầm (Meat & Poultry) | Thịt heo, bò, gà, vịt, thịt xay sơ chế | 47 |
| **3** | Hải sản (Seafood) | Cá biển, cá sông, tôm, mực, nghêu, sò, cua | 28 |
| **4** | Gia vị & Nước chấm (Spices & Sauces) | Nước mắm, hạt nêm, tiêu, đường, tương ớt, dầu hào | 36 |
| **5** | Đồ khô & Gạo (Grains & Dry Goods) | Gạo tẻ, gạo lứt, bún khô, miến, mì, bánh đa | 32 |
| **6** | Sữa & Trứng (Dairy & Eggs) | Trứng gà, trứng vịt, sữa tươi, phô mai, bơ lạt | 19 |
| **7** | Trái cây (Fruits) | Trái cây ăn trực tiếp và chế biến món ăn | 34 |
| **8** | Dầu mỡ & Chất béo (Oils & Fats) | Dầu thực vật, mỡ heo, dầu mè, dầu oliu | 9 |
| **9** | Các loại Hạt (Nuts & Seeds) | Hạt điều, đậu phộng, vừng, hạt chia, đậu xanh | 11 |

---

## 🔌 Tài liệu API chi tiết (15 Controllers)

> **Base URL:** `http://localhost:8080/api/v1`  
> Tất cả response thành công hoặc lỗi đều được gói chuẩn tắc trong:  
> `{ "success": boolean, "data": T, "message": string, "errorCode": string, "timestamp": string }`  
> **Ký hiệu quyền:** 🔓 Public · 🔐 Đăng nhập (JWT) · 👑 Quản trị viên (ADMIN)

---

### 1. Xác thực & Quản lý phiên (`/auth`)

| Method | Endpoint | Quyền | Mô tả & Chức năng |
|---|---|:---:|---|
| `POST` | `/auth/register` | 🔓 | Đăng ký tài khoản (`username`, `email`, `password`, `displayName`) |
| `POST` | `/auth/login` | 🔓 | Đăng nhập; nhận `accessToken` (24h) và `refreshToken` (7d) |
| `POST` | `/auth/refresh` | 🔓 | Cấp mới access token bằng refresh token hợp lệ |
| `POST` | `/auth/logout` | 🔐 | Thu hồi token và hủy phiên đăng nhập trong Redis |
| `POST` | `/auth/forgot-password` | 🔓 | Tạo mã OTP 6 chữ số (TTL 5m lưu Redis) gửi qua Gmail SMTP |
| `POST` | `/auth/reset-password` | 🔓 | Đặt lại mật khẩu mới bằng việc kiểm tra cặp `email` + `otp` |

---

### 2. Người dùng & Mạng xã hội (`/users`)

| Method | Endpoint | Quyền | Mô tả & Chức năng |
|---|---|:---:|---|
| `GET` | `/users/me` | 🔐 | Lấy toàn bộ thông tin tài khoản hiện tại |
| `PUT` | `/users/me` | 🔐 | Cập nhật hồ sơ (`displayName`, `bio`) |
| `PUT` | `/users/me/password` | 🔐 | Đổi mật khẩu cá nhân (kiểm tra mật khẩu cũ) |
| `POST` | `/users/me/avatar` | 🔐 | Upload ảnh đại diện cá nhân lên Cloudinary |
| `DELETE` | `/users/me` | 🔐 | Xóa vĩnh viễn tài khoản của chính mình (yêu cầu nhập lại pass) |
| `GET` | `/users/{id}/profile` | 🔐 | Xem hồ sơ công khai của người dùng khác kèm trạng thái follow |
| `POST` | `/users/{id}/follow` | 🔐 | Bắt đầu theo dõi một đầu bếp / người dùng khác |
| `DELETE` | `/users/{id}/follow` | 🔐 | Hủy theo dõi người dùng |
| `GET` | `/users/{id}/followers` | 🔐 | Danh sách người theo dõi của tài khoản (phân trang) |
| `GET` | `/users/{id}/following` | 🔐 | Danh sách người tài khoản đang theo dõi (phân trang) |

---

### 3. Công thức nấu ăn (`/recipes`)

| Method | Endpoint | Quyền | Mô tả & Chức năng |
|---|---|:---:|---|
| `GET` | `/recipes/public` | 🔐 | Lấy danh sách công thức đã duyệt (`status = PUBLIC`), sắp xếp theo tiêu chí |
| `GET` | `/recipes/my` | 🔐 | Lấy danh sách công thức của user (hỗ trợ lọc status: DRAFT, PENDING, PUBLIC) |
| `GET` | `/recipes/{id}` | 🔐 | Chi tiết công thức: nguyên liệu, bước làm, tags, dinh dưỡng, tác giả |
| `POST` | `/recipes` | 🔐 | Tạo công thức mới (mặc định trạng thái PENDING_REVIEW chờ duyệt) |
| `PUT` | `/recipes/{id}` | 🔐 | Cập nhật nội dung công thức (chỉ tác giả hoặc admin) |
| `DELETE` | `/recipes/{id}` | 🔐 | Xóa mềm công thức (`status = DELETED`) |
| `PATCH` | `/recipes/{id}/status` | 🔐 | Tự chuyển trạng thái công thức cá nhân (DRAFT $\leftrightarrow$ PENDING_REVIEW) |
| `GET` | `/recipes/search` | 🔐 | Tìm kiếm công thức theo từ khóa, nguyên liệu hoặc tag |
| `GET` | `/recipes/user/{userId}`| 🔐 | Xem danh sách công thức công khai của một tác giả cụ thể |
| `POST` | `/recipes/{id}/clone` | 🔐 | Sao chép công thức của người khác về tài khoản cá nhân để tùy biến |
| `POST` | `/recipes/{id}/cook` | 🔐 | **Nấu ngay:** Ghi nhận nhật ký nấu ăn và **tự động trừ kho (FEFO)** |
| `POST` | `/recipes/{id}/like` | 🔐 | Bày tỏ yêu thích công thức (tăng `like_count`, tạo thông báo) |
| `DELETE`| `/recipes/{id}/like` | 🔐 | Bỏ thích công thức |
| `GET` | `/recipes/liked-ids` | 🔐 | Danh sách ID các công thức người dùng đã thích (tối ưu UI toggle) |
| `POST` | `/recipes/{id}/image`| 🔐 | Upload ảnh bìa đại diện cho công thức (Cloudinary) |
| `POST` | `/recipes/{id}/steps/{stepNo}/image` | 🔐 | Upload ảnh chụp hướng dẫn cho bước chế biến cụ thể |
| `GET` | `/recipes/{id}/export/word` | 🔐 | **Xuất file Word (`.docx`)** chuyên nghiệp kèm bảng nguyên liệu & bước làm |

---

### 4. Bình luận công thức (`/comments`)

| Method | Endpoint | Quyền | Mô tả & Chức năng |
|---|---|:---:|---|
| `GET` | `/recipes/{recipeId}/comments` | 🔐 | Lấy danh sách bình luận kèm phân cấp trả lời theo cây |
| `POST` | `/recipes/{recipeId}/comments` | 🔐 | Gửi bình luận mới hoặc phản hồi bình luận trước đó |
| `PUT` | `/comments/{id}` | 🔐 | Chỉnh sửa nội dung bình luận cá nhân |
| `DELETE`| `/comments/{id}` | 🔐 | Xóa bình luận cá nhân |

---

### 5. Tủ nguyên liệu thông minh (`/pantry`)

| Method | Endpoint | Quyền | Mô tả & Chức năng |
|---|---|:---:|---|
| `GET` | `/pantry` | 🔐 | Lấy danh sách nguyên liệu trong tủ, **nhóm theo 9 kệ hàng**, lọc theo hạn |
| `GET` | `/pantry/summary` | 🔐 | Thống kê số lượng món an toàn, sắp hết hạn, và đã hết hạn |
| `GET` | `/pantry/expiring-soon` | 🔐 | Danh sách nguyên liệu sắp hết hạn trong vòng $N$ ngày (mặc định 7 ngày) |
| `POST` | `/pantry` | 🔐 | Thêm mới hoặc **cộng dồn** số lượng nếu cùng nguyên liệu & hạn dùng |
| `PUT` | `/pantry/{id}` | 🔐 | Cập nhật số lượng tồn, ngưỡng tối thiểu, hoặc ngày hết hạn |
| `DELETE`| `/pantry/{id}` | 🔐 | Xóa một mục nguyên liệu khỏi tủ |
| `DELETE`| `/pantry/expired` | 🔐 | **Dọn rác 1-click:** Xóa toàn bộ các nguyên liệu đã quá hạn sử dụng |

---

### 6. Danh sách đi chợ (`/grocery`)

| Method | Endpoint | Quyền | Mô tả & Chức năng |
|---|---|:---:|---|
| `GET` | `/grocery/lists` | 🔐 | Lấy tất cả danh sách đi chợ của người dùng |
| `GET` | `/grocery/lists/active` | 🔐 | Lấy danh sách đi chợ đang hoạt động gần nhất |
| `POST` | `/grocery/lists` | 🔐 | Tạo danh sách đi chợ mới rỗng |
| `GET` | `/grocery/lists/{id}` | 🔐 | Chi tiết danh sách đi chợ kèm các món gom nhóm theo quầy |
| `PUT` | `/grocery/lists/{id}` | 🔐 | Cập nhật tên hoặc ghi chú danh sách đi chợ |
| `DELETE`| `/grocery/lists/{id}` | 🔐 | Xóa toàn bộ danh sách đi chợ |
| `POST` | `/grocery/lists/{id}/items` | 🔐 | Thêm thủ công một nguyên liệu cần mua vào danh sách |
| `PUT` | `/grocery/items/{itemId}` | 🔐 | Chỉnh sửa số lượng hoặc đơn vị của một món đi chợ |
| `DELETE`| `/grocery/items/{itemId}` | 🔐 | Xóa một món ra khỏi danh sách đi chợ |
| `PATCH` | `/grocery/items/{itemId}/toggle` | 🔐 | Đánh dấu đã mua / chưa mua (checkbox khi ở siêu thị) |
| `DELETE`| `/grocery/lists/{id}/items` | 🔐 | Xóa trắng tất cả món trong danh sách |
| `POST` | `/grocery/lists/{id}/complete` | 🔐 | **Hoàn tất đi chợ:** Đóng list và **tự động nạp các món đã mua vào tủ pantry** |
| `POST` | `/grocery/lists/generate-from-pantry` | 🔐 | **Sinh tự động từ tủ:** Tạo list các món đang dưới mức cảnh báo tồn |
| `POST` | `/grocery/lists/generate-from-recipe/{id}` | 🔐 | **Sinh từ công thức:** Tính toán (Cần - Có = Mua) theo số phần ăn (`servings`) |

---

### 7. Trợ lý AI Gemini (`/ai`)

| Method | Endpoint | Quyền | Mô tả & Chức năng |
|---|---|:---:|---|
| `GET` | `/ai/suggest/pantry` | 🔐 | **Zero-Waste:** Lấy nguyên liệu trong tủ (ưu tiên cận date), yêu cầu Gemini sinh món |
| `POST` | `/ai/suggest/custom` | 🔐 | Gợi ý món ăn từ danh sách nguyên liệu người dùng tự nhập |
| `POST` | `/ai/save/{logId}` | 🔐 | **Lưu công thức AI:** Chuyển log AI thành công thức thật, áp dụng matching nguyên liệu |
| `GET` | `/ai/history` | 🔐 | Lấy lịch sử các lần tương tác với Trợ lý AI |
| `GET` | `/ai/remaining` | 🔐 | Kiểm tra số lượt gọi AI còn lại trong ngày (giới hạn 10 lượt/ngày qua Redis) |

---

### 8. Nhật ký nấu ăn (`/journals`)

| Method | Endpoint | Quyền | Mô tả & Chức năng |
|---|---|:---:|---|
| `GET` | `/journals` | 🔐 | Lấy lịch sử các lần nấu ăn cá nhân (phân trang) |
| `GET` | `/journals/{id}` | 🔐 | Xem chi tiết 1 lần nấu (đánh giá sao, hình ảnh, khẩu phần thực tế) |
| `POST` | `/journals` | 🔐 | Tạo nhật ký nấu món và **kích hoạt trừ kho nguyên liệu FEFO** |
| `PUT` | `/journals/{id}` | 🔐 | Cập nhật cảm nhận, ghi chú, hoặc số sao đánh giá |
| `DELETE`| `/journals/{id}` | 🔐 | Xóa nhật ký nấu ăn |
| `POST` | `/journals/{id}/image` | 🔐 | Upload ảnh thành phẩm món ăn thực tế đã nấu lên Cloudinary |

---

### 9. Trung tâm Thông báo (`/notifications`)

| Method | Endpoint | Quyền | Mô tả & Chức năng |
|---|---|:---:|---|
| `GET` | `/notifications` | 🔐 | Lấy danh sách thông báo hoạt động cá nhân (phân trang) |
| `GET` | `/notifications/unread-count` | 🔐 | Đếm số lượng thông báo chưa đọc (hiển thị badge header) |
| `PATCH` | `/notifications/{id}/read` | 🔐 | Đánh dấu một thông báo đã đọc |
| `PATCH` | `/notifications/read-all` | 🔐 | Đánh dấu tất cả thông báo là đã đọc |

---

### 10. Nguyên liệu dùng chung (`/ingredients`)

| Method | Endpoint | Quyền | Mô tả & Chức năng |
|---|---|:---:|---|
| `GET` | `/ingredients` | 🔐 | Danh sách 297+ nguyên liệu hệ thống (phân trang, sắp xếp theo tên) |
| `GET` | `/ingredients/search?q=` | 🔐 | Tìm kiếm nhanh nguyên liệu theo tên có dấu hoặc không dấu |
| `GET` | `/ingredients/aisle/{aisleId}` | 🔐 | Lọc danh sách nguyên liệu theo quầy hàng siêu thị |
| `GET` | `/ingredients/{id}` | 🔐 | Xem chi tiết dinh dưỡng chuẩn (Calories, Protein, Fat, Carbs) |
| `POST` | `/ingredients/quick` | 🔐 | **User thêm nhanh:** Tạo nguyên liệu mới (mặc định calo = 0 chờ duyệt) |
| `POST` | `/ingredients` | 👑 | Tạo nguyên liệu đầy đủ thông số dinh dưỡng chuẩn |
| `PUT` | `/ingredients/{id}` | 👑 | Chỉnh sửa dinh dưỡng và thông tin nguyên liệu |
| `PATCH` | `/ingredients/{id}/aisle` | 🔐 | Cập nhật quầy hàng cho nguyên liệu |
| `DELETE`| `/ingredients/{id}` | 👑 | Xóa nguyên liệu khỏi cơ sở dữ liệu dùng chung |

---

### 11. Quầy hàng siêu thị (`/aisles`)

| Method | Endpoint | Quyền | Mô tả & Chức năng |
|---|---|:---:|---|
| `GET` | `/aisles` | 🔐 | Lấy danh sách toàn bộ 9 quầy hàng siêu thị |
| `GET` | `/aisles/{id}` | 🔐 | Xem thông tin chi tiết một quầy hàng |
| `POST` | `/aisles` | 👑 | Tạo mới một quầy hàng |
| `PUT` | `/aisles/{id}` | 👑 | Sửa đổi tên hoặc định danh quầy hàng |
| `DELETE`| `/aisles/{id}` | 👑 | Xóa quầy hàng |

---

### 12. Thẻ phân loại (`/tags`)

| Method | Endpoint | Quyền | Mô tả & Chức năng |
|---|---|:---:|---|
| `GET` | `/tags` | 🔐 | Lấy tất cả các thẻ phân loại (Món Chay, Ăn Kiêng, Món Nhanh...) |
| `GET` | `/tags/{id}` | 🔐 | Xem chi tiết một thẻ |
| `POST` | `/tags` | 🔐 | Tạo thẻ mới |
| `PUT` | `/tags/{id}` | 🔐 | Cập nhật tên thẻ |
| `DELETE`| `/tags/{id}` | 🔐 | Xóa thẻ khỏi hệ thống |

---

### 13. Quy đổi đơn vị (`/unit-conversions`)

| Method | Endpoint | Quyền | Mô tả & Chức năng |
|---|---|:---:|---|
| `GET` | `/unit-conversions` | 🔐 | Lấy danh sách tất cả các quy tắc quy đổi đơn vị |
| `GET` | `/unit-conversions/generic` | 🔐 | Lấy các quy tắc quy đổi đơn vị phổ thông (kg $\rightarrow$ g, l $\rightarrow$ ml...) |
| `GET` | `/unit-conversions/ingredient/{id}` | 🔐 | Lấy quy tắc quy đổi đặc thù cho nguyên liệu (1 quả chanh $\rightarrow$ 60g) |
| `POST` | `/unit-conversions` | 👑 | Tạo quy tắc quy đổi mới |
| `PUT` | `/unit-conversions/{id}` | 👑 | Chỉnh sửa hệ số hoặc đơn vị |
| `DELETE`| `/unit-conversions/{id}` | 👑 | Xóa quy tắc quy đổi |

---

### 14. Quản trị hệ thống nâng cao (`/admin`)

> **Yêu cầu phân quyền:** Tất cả API bên dưới bắt buộc có quyền `ROLE_ADMIN` (`@PreAuthorize("hasRole('ADMIN')")`).

```
                              ┌────────────────────────┐
                              │  ADMIN CONTROL CENTER  │
                              └───────────┬────────────┘
         ┌──────────────────┬─────────────┴────────────┬──────────────────┐
         │                  │                          │                  │
┌────────▼───────┐ ┌────────▼────────┐        ┌────────▼───────┐ ┌────────▼───────┐
│ /stats         │ │ /recipes        │        │ /ingredients   │ │ /users         │
│ Platform KPIs  │ │ Moderation      │        │ Pending Review │ │ Role Guards    │
│ Waste Index    │ │ Approve / Hide  │        │ Nutrition Sync │ │ Account Safety │
└────────────────┘ └─────────────────┘        └────────────────┘ └────────────────┘
```

| Method | Endpoint | Mô tả chi tiết chức năng Quản trị |
|---|---|---|
| `GET` | `/admin/stats` | **Dashboard KPI:** Thống kê toàn diện nền tảng (tổng user, công thức, nguyên liệu), biểu đồ phân bổ độ khó (EASY/MEDIUM/HARD), chỉ số chống lãng phí kho Pantry (Waste Prevention Rate %), tần suất gọi AI hôm nay, điểm đánh giá nấu nướng trung bình và hàng đợi kiểm duyệt nhanh. |
| `GET` | `/admin/recipes` | **Duyệt công thức:** Lọc danh sách theo trạng thái (`PENDING_REVIEW`, `PUBLIC`, `PRIVATE`), hỗ trợ tìm kiếm theo tiêu đề/tác giả và phân trang. |
| `GET` | `/admin/recipes/{id}` | Xem chi tiết công thức trước khi phê duyệt (đầy đủ các bước và nguyên liệu). |
| `PATCH` | `/admin/recipes/{id}/status?action=` | Đổi trạng thái: `action=APPROVE` (công khai), `action=HIDE` (ẩn về riêng tư), `action=DELETE` (xóa mềm). |
| `GET` | `/admin/ingredients/pending-review` | **Hàng đợi nguyên liệu:** Lấy các nguyên liệu có `calories = 0` do người dùng thêm nhanh hoặc AI sinh ra cần chuẩn hóa dinh dưỡng. |
| `GET` | `/admin/ingredients` | Danh sách nguyên liệu dùng chung, tìm kiếm từ khóa và lọc theo quầy hàng. |
| `POST` | `/admin/ingredients` | Thêm nguyên liệu chuẩn vào master data với đầy đủ 4 chỉ số dinh dưỡng. |
| `PATCH` | `/admin/ingredients/{id}` | Chuẩn hóa dinh dưỡng và gán lại quầy hàng siêu thị cho nguyên liệu. |
| `DELETE`| `/admin/ingredients/{id}` | Xóa nguyên liệu khỏi hệ sinh thái. |
| `GET` | `/admin/users` | Danh sách người dùng hệ thống, lọc theo Role (`ALL`, `USER`, `ADMIN`), tìm kiếm tên/email, thống kê số công thức và số lần nấu của từng người. |
| `GET` | `/admin/users/{id}` | Chi tiết hồ sơ thành viên, chỉ số người theo dõi, cùng 5 công thức gần nhất. |
| `PATCH` | `/admin/users/{id}/role?role=` | **Cấp/Hạ quyền:** Chuyển đổi giữa `USER` $\leftrightarrow$ `ADMIN`. **Tích hợp Safety Guards:** Không cho phép Admin tự hạ quyền chính mình, không cho phép hạ quyền nếu chỉ còn duy nhất 1 Admin trong hệ thống. |
| `DELETE`| `/admin/users/{id}` | Xóa tài khoản thành viên (chặn tự xóa bản thân và chặn xóa Admin cuối cùng). |
| `GET` | `/admin/ai-logs` | **Giám sát Trợ lý AI:** Danh sách lịch sử prompt, loại gợi ý (`ZERO_WASTE` / `CUSTOM`), thời gian phản hồi, trạng thái đã được lưu thành công thức thật hay chưa. |

---

## 🧠 Thuật toán & Cơ chế cốt lõi

### 1. Thuật toán Khớp nguyên liệu 3 lớp (Ingredient Matching)

Khi người dùng lưu một gợi ý công thức từ AI (`POST /api/v1/ai/save/{logId}`), hệ thống phải ánh xạ các tên nguyên liệu do AI sinh ra (chuỗi tự do) về ID nguyên liệu chính xác trong cơ sở dữ liệu:

```
Tên nguyên liệu từ AI: "Thịt nạc vai heo"
               │
               ▼
┌──────────────────────────────────────────────┐
│ Lớp 1: Khớp chính xác (Exact Match)          │
│   findFirstByNameIgnoreCase(name)            │
│   → Trúng khớp 100%? Gán ID ngay!            │
└──────────────────────┬───────────────────────┘
                       │ Không khớp
                       ▼
┌──────────────────────────────────────────────┐
│ Lớp 2: Khớp Token & Trọng số (Token Match)   │
│   - Chuẩn hóa: bỏ dấu tiếng Việt, lowercase  │
│   - Tách từ: ["thit", "nac", "vai", "heo"]   │
│   - Quét toàn bộ 297 nguyên liệu trong DB    │
│   - Tính điểm: số token trùng lặp            │
│   → Điểm >= 2 và cao nhất? Chọn ID này!      │
└──────────────────────┬───────────────────────┘
                       │ Không đủ điểm khớp (< 2 tokens)
                       ▼
┌──────────────────────────────────────────────┐
│ Lớp 3: Tự động khởi tạo (Auto-Create)        │
│   - Tạo Ingredient mới: name = tên từ AI     │
│   - baseUnit = "g", caloriesPer100g = 0      │
│   - Gắn cờ chờ Admin kiểm duyệt dinh dưỡng   │
└──────────────────────────────────────────────┘
```

---

### 2. Cơ chế trừ kho thông minh FEFO (First-Expired, First-Out)

Khi người dùng nhấn **"Nấu ngay"** (`POST /recipes/{id}/cook`) hoặc tạo nhật ký nấu ăn:

1. **Duyệt nguyên liệu:** Hệ thống lấy toàn bộ thành phần của công thức theo tỉ lệ khẩu phần ăn đã chọn (`servings / baseServings`).
2. **Quy đổi chuẩn:** Gọi `UnitNormalizationService` để đổi số lượng cần sang `baseUnit` (`g` hoặc `ml`).
3. **Quét kho Pantry:** Truy vấn bảng `user_pantry` theo `ingredient_id` của user, sắp xếp theo **`expiry_date ASC`** (lô hàng cận hạn nhất được xếp trước).
4. **Trừ dần (Deduction Loop):**
   - Nếu lô hiện tại có số lượng $\ge$ lượng cần: Trừ phần cần, giữ lại phần dư.
   - Nếu lô hiện tại có số lượng $<$ lượng cần: Trừ cạn lô này (xóa bản ghi), lấy số lượng thiếu đi trừ tiếp vào lô có hạn dài hơn kế tiếp.
   - Nếu kho không đủ nguyên liệu: Trừ tối đa lượng đang có mà **không gây crash ứng dụng**.

---

### 3. Quy đổi đơn vị đa bước bằng Đồ thị BFS

Xử lý các đơn vị đo lường ẩm thực phức tạp thông qua thuật toán tìm đường đi ngắn nhất trên đồ thị vô hướng trọng số nhân:

```
Ví dụ: Quy đổi từ "chén dầu ăn" sang "ml"
Đồ thị biểu diễn:
  [chén] ──(x200)──> [muỗng canh] ──(x15)──> [ml]

Hàng đợi BFS:
  Bước 0: Start [chén, factor = 1.0]
  Bước 1: Đi tới [muỗng canh, factor = 200]
  Bước 2: Đi tới [ml, factor = 200 * 15 = 3000]
  Kết quả: 1 chén = 3000 (tùy thuộc vào bảng quy đổi đặc thù của nguyên liệu)
```

- **Độ sâu tối đa:** `MAX_HOPS = 4` nhằm ngăn ngừa vòng lặp đồ thị vô tận.
- **Hỗ trợ Alias linh hoạt:** Tự động nhận diện các biến thể phát âm: `muong ca phe`, `thia cafe`, `tsp`, `thia canh`, `tbsp`, `chen`, `bat`, `qua`, `trai`.

---

### 4. Bảo mật JWT & Xác thực OTP qua Email

- **Stateless Session:** Token JWT chứa định danh `username`, `roles` và được ký bằng thuật toán HMAC-SHA256 (`HS256`).
- **Xác thực 2 lớp khôi phục mật khẩu:**
  1. Người dùng yêu cầu lấy lại mật khẩu $\rightarrow$ Hệ thống sinh ngẫu nhiên mã số gồm 6 chữ số an toàn cryptographically.
  2. Lưu vào Redis với key `otp:{email}` và thời hạn tự hủy đúng **5 phút** (`Duration.ofMinutes(5)`).
  3. Gửi email định dạng HTML chứa mã OTP qua giao thức Gmail SMTP TLS (Port 587).
  4. Người dùng nhập mã mới cùng mật khẩu mới $\rightarrow$ Kiểm tra chuỗi Redis $\rightarrow$ Hợp lệ thì mã hóa BCrypt cập nhật vào Database và lập tức xóa key OTP khỏi Redis.

---

## ⚡ Chiến lược Caching (Redis)

Cấu hình chi tiết trong `CacheConfig.java` với `RedisCacheManager`:

| Tên Cache Region | Mục đích | TTL | Cơ chế Invalidate |
|---|---|:---:|---|
| `aisles`, `aisle` | Danh mục quầy hàng siêu thị | 60 phút | Xóa khi Admin cập nhật quầy hàng |
| `tags`, `tag` | Danh mục thẻ phân loại món ăn | 60 phút | Xóa khi tạo/sửa thẻ mới |
| `ingredients_by_aisle` | Danh sách nguyên liệu theo quầy | 60 phút | Xóa khi thêm/sửa nguyên liệu |
| `ingredient` | Chi tiết một nguyên liệu kèm dinh dưỡng | 60 phút | Xóa khi Admin chỉnh sửa dinh dưỡng |
| `unit_conversions` | Quy tắc quy đổi đơn vị | 60 phút | Xóa khi cập nhật quy tắc quy đổi |
| `ingredients_search` | Kết quả tìm kiếm nguyên liệu theo tên | 15 phút | Tự hết hạn sau 15 phút |
| *(Trực tiếp)* `ai:ratelimit:{userId}` | Đếm số lượt gọi AI Gemini trong ngày | Hết ngày | Tự reset vào 00:00:00 mỗi đêm |
| *(Trực tiếp)* `otp:{email}` | Mã số OTP khôi phục mật khẩu | 5 phút | Xóa ngay khi dùng hoặc sau 300 giây |

---

## 🧪 Kiểm thử tự động (Unit Tests)

Dự án sở hữu bộ kiểm thử tự động gồm **48 ca test** độc lập hoàn toàn với database thật (sử dụng JUnit 5, Mockito và AssertJ):

```bash
# Chạy toàn bộ test suite
./mvnw test
```

| Lớp Kiểm thử (Test Suite) | Số ca | Phạm vi & Nghiệp vụ kiểm tra |
|---|:---:|---|
| `AiServiceImplTest` | 14 | Kiểm tra rate limit 10 lần/ngày, parse JSON Gemini, thuật toán khớp nguyên liệu 3 lớp, truy xuất lịch sử |
| `PantryServiceImplTest` | 10 | Kiểm tra trừ kho theo nguyên tắc FEFO, xử lý cộng dồn hạn dùng, cảnh báo cận date, dọn dẹp hàng hết hạn |
| `AuthServiceImplOtpTest` | 8 | Kiểm tra phát sinh OTP, lưu Redis, kiểm tra sai OTP, hết hạn OTP, gửi email qua SMTP |
| `RecipeServiceImplTest` | 7 | Kiểm tra phân quyền chủ sở hữu khi sửa/xóa công thức, duyệt trạng thái, like/unlike, nhân bản (clone) |
| `GroceryServiceImplTest` | 4 | Kiểm tra công thức trừ kho (Cần - Có = Mua), phân loại vào 9 quầy hàng, hoàn tất chuyển vào pantry |
| `UnitNormalizationServiceTest` | 4 | Kiểm tra thuật toán BFS quy đổi đơn vị qua đồ thị, xử lý alias tên gọi, phát hiện chuỗi quy đổi hợp lệ |
| `BackendApplicationTests` | 1 | Kiểm tra Spring ApplicationContext khởi động thành công |
| **Tổng cộng** | **48** | **Độ phủ logic nghiệp vụ cốt lõi: 100% Pass** |

---

## ⚙️ Cài đặt & Chạy cục bộ (Local Development)

### Yêu cầu môi trường

- **Java Development Kit (JDK):** Phiên bản 21 trở lên
- **Maven:** Phiên bản 3.9+ (hoặc dùng trực tiếp `./mvnw`)
- **Docker & Docker Compose:** Dành cho việc khởi chạy MySQL & Redis cục bộ

### 1. Khởi chạy Database & Cache bằng Docker

Tại thư mục gốc của toàn bộ dự án (`SmartRecipe-Project/`):

```bash
docker-compose up -d mysql-db redis-cache
```

> File `sql/init_database.sql` sẽ tự động tạo đủ **19 bảng**, nạp sẵn **9 quầy hàng** và **12 tags**.

### 2. Nạp dữ liệu 297+ nguyên liệu và dữ liệu mẫu

Sử dụng file dữ liệu mẫu đã xuất đầy đủ từ hệ thống (`sql/export_local_data.sql`):

```bash
# Nạp toàn bộ dữ liệu mẫu (Users, Ingredients, Recipes, Journals, Pantry)
docker exec -i smartrecipe-mysql mysql -uroot -proot smart_recipe_db < sql/export_local_data.sql
```

### 3. Cấu hình biến môi trường

Tạo file `.env` tại thư mục `smartrecipe-backend/`:

```env
# Google Gemini AI API Key
GEMINI_API_KEY=your_gemini_api_key_here
GEMINI_MODEL=gemini-2.0-flash

# JWT Secret (Chuỗi ngẫu nhiên tối thiểu 256-bit)
JWT_SECRET=U21hcnRSZWNpcGVfU3VwZXJTZWNyZXRLZXlfMjAyNl8xMjM0NTY3ODkw
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Gmail SMTP gửi mã OTP
SPRING_MAIL_USERNAME=your_gmail@gmail.com
SPRING_MAIL_PASSWORD=your_app_password

# Cloudinary CDN lưu trữ ảnh
CLOUDINARY_URL=cloudinary://<api_key>:<api_secret>@<cloud_name>
```

### 4. Khởi chạy ứng dụng

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows PowerShell
.\mvnw spring-boot:run
```

Ứng dụng sẽ sẵn sàng phục vụ tại: **`http://localhost:8080`**  
Kiểm tra sức khỏe hệ thống: `http://localhost:8080/actuator/health`

---

## 🔧 Biến môi trường

| Biến môi trường | Bắt buộc | Giá trị mặc định | Mô tả |
|---|:---:|---|---|
| `SPRING_DATASOURCE_URL` | Không | `jdbc:mysql://localhost:3306/smart_recipe_db` | URL kết nối JDBC tới MySQL hoặc TiDB Cloud |
| `SPRING_DATASOURCE_USERNAME` | Không | `root` | Tên đăng nhập cơ sở dữ liệu |
| `SPRING_DATASOURCE_PASSWORD` | Không | `root` | Mật khẩu cơ sở dữ liệu |
| `SPRING_DATA_REDIS_HOST` | Không | `localhost` | Host máy chủ Redis |
| `SPRING_DATA_REDIS_PORT` | Không | `6379` | Port máy chủ Redis |
| `SPRING_DATA_REDIS_PASSWORD` | Không | `""` | Mật khẩu xác thực Redis (cần thiết trên Upstash) |
| `SPRING_DATA_REDIS_SSL_ENABLED`| Không| `false` | Bật SSL khi kết nối Upstash Redis (`true` trên production) |
| `GEMINI_API_KEY` | **Có** | `your_gemini_api_key` | Khóa API của Google AI Studio |
| `GEMINI_MODEL` | Không | `gemini-2.0-flash` | Tên model Gemini muốn sử dụng |
| `JWT_SECRET` | **Có** | Base64 mặc định | Khóa bí mật ký JWT Token |
| `SPRING_MAIL_USERNAME` | Không | — | Email dùng để gửi OTP khôi phục mật khẩu |
| `SPRING_MAIL_PASSWORD` | Không | — | App Password 16 chữ số của Google Gmail |
| `CLOUDINARY_URL` | Không | — | Kết nối Cloudinary dạng `cloudinary://key:secret@cloud` |

---

## 🐳 Docker & CI/CD Pipeline

### Multi-stage Dockerfile

Backend sử dụng Docker Multi-stage build nhằm giảm dung lượng ảnh từ 800MB xuống chỉ còn **~240MB**:

```dockerfile
# Stage 1: Build JAR với JDK 21
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline
COPY src src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime với JRE 21 siêu nhẹ
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
```

### GitHub Actions CI/CD Workflows

Hệ thống CI/CD tự động được thiết lập trong thư mục `.github/workflows/`:

1. **`ci-backend.yml` (Continuous Integration):**
   - Kích hoạt khi có commit/PR lên bất kỳ nhánh nào.
   - Thiết lập môi trường Temurin JDK 21 và cache dependency Maven.
   - Thực thi toàn bộ **48 unit tests** (không phụ thuộc vào database bên ngoài).
   - Đóng gói artifact file `.jar` và kiểm tra lỗi biên dịch.
2. **`cd-backend.yml` (Continuous Delivery):**
   - Kích hoạt tự động khi merge vào nhánh `main` và CI đã pass.
   - Gửi tín hiệu gọi **Render Deploy Hook** để trigger quá trình build container và cập nhật máy chủ.
   - Polling kiểm tra trạng thái `/actuator/health` trong 10 phút để xác nhận ứng dụng đã live an toàn.

---

*Phát triển bởi đội ngũ kỹ sư SmartRecipe — Spring Boot 4.1.0 & Java 21 LTS*
