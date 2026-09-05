# Database Seed Data

## Mục đích
File `seed_data.sql` chứa dữ liệu ban đầu cần thiết để ứng dụng hoạt động:

| Bảng | Số lượng | Ghi chú |
|------|----------|---------|
| `aisles` | ~10 kệ hàng | Phân loại nguyên liệu |
| `ingredients` | 293 nguyên liệu | Dataset chính — bắt buộc |
| `unit_conversions` | ~50 rules | Cả global (ingredient_id=NULL) và per-ingredient |

> ⚠️ **Không cần import `unit_conversions` từ file này** — `UnitConversionSeeder.java`
> sẽ tự động seed các global rules khi backend khởi động lần đầu.
> Tuy nhiên các per-ingredient rules (density riêng cho từng loại dầu ăn...)
> vẫn cần import thủ công từ file này.

## Khi nào cần chạy?
- Lần đầu deploy lên server (production/staging)
- Khi reset database về trạng thái ban đầu

## Cách chạy

### Option 1: Qua MySQL CLI
```bash
mysql -u<user> -p<password> <database_name> < seed_data.sql
```

### Option 2: Qua Docker (nếu dùng container)
```bash
docker exec -i <mysql_container> mysql -uroot -p<password> smart_recipe_db < seed_data.sql
```

### Option 3: Qua MySQL Workbench / DBeaver
Mở file `seed_data.sql` và chạy trực tiếp trong GUI.

## Lưu ý khi deploy
1. Chạy `seed_data.sql` **sau khi** Hibernate đã tạo xong các tables (lần đầu start backend)
2. File này có `INSERT IGNORE` — chạy nhiều lần không bị lỗi duplicate
3. `UnitConversionSeeder` tự chạy mỗi lần startup — không cần lo về global unit rules

## Cập nhật dataset
Khi thêm/sửa nguyên liệu trên local, xuất lại file này bằng lệnh:
```bash
docker exec smartrecipe-mysql mysqldump -uroot -proot \
  --no-tablespaces --skip-triggers --single-transaction \
  --set-gtid-purged=OFF \
  smart_recipe_db aisles ingredients unit_conversions > seed_data.sql
```
