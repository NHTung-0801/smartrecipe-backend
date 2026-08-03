package com.smartrecipe.smartrecipe_backend.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.smartrecipe.smartrecipe_backend.exception.BadRequestException;
import com.smartrecipe.smartrecipe_backend.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new BadRequestException("File không được để trống");
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new BadRequestException("Chỉ chấp nhận file hình ảnh (image/*)");
            }
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new BadRequestException("Kích thước file không được vượt quá 5MB");
            }

            // Tạo tên file độc nhất để không bị trùng lặp
            String publicId = UUID.randomUUID().toString();
            
            // Tham số cấu hình upload
            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", folder,
                    "public_id", publicId
            );

            // Tải lên file
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);

            // Trả về secure_url (URL HTTPS)
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new BadRequestException("Không thể tải ảnh lên Cloudinary: " + e.getMessage());
        }
    }
}
