package com.smartrecipe.smartrecipe_backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
    /**
     * Upload an image to Cloudinary
     *
     * @param file the multipart file to upload
     * @param folder the folder name in Cloudinary
     * @return the secure URL of the uploaded image
     */
    String uploadImage(MultipartFile file, String folder);
}
