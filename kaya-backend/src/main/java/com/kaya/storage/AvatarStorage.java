package com.kaya.storage;

import com.kaya.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Local-disk avatar storage. Files are named with a random UUID and the image
 * type is detected from magic bytes instead of trusting the client.
 */
@Component
public class AvatarStorage {

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    private final Path root;
    private final String publicBasePath;

    public AvatarStorage(@Value("${storage.avatar-dir}") String directory,
                         @Value("${storage.public-base-path:/api}") String publicBasePath) {
        this.root = Paths.get(directory).toAbsolutePath().normalize();
        this.publicBasePath = publicBasePath.endsWith("/")
                ? publicBasePath.substring(0, publicBasePath.length() - 1)
                : publicBasePath;
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create avatar directory " + root, ex);
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(40001, "请选择要上传的头像");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(41301, "头像不能超过 5MB");
        }

        String extension = detectExtension(file);
        String filename = UUID.randomUUID() + "." + extension;
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(40001, "文件名不合法");
        }

        try (InputStream input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException(50001, "头像保存失败");
        }
        return publicBasePath + "/files/avatars/" + filename;
    }

    public Resource load(String filename) {
        if (filename == null || !FILENAME_PATTERN.matcher(filename).matches()) {
            throw new BusinessException(40402, "文件不存在");
        }
        Path target = root.resolve(filename).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            throw new BusinessException(40402, "文件不存在");
        }
        return new FileSystemResource(target);
    }

    private String detectExtension(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            if (header.length >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF) {
                return "jpg";
            }
            if (header.length >= 8
                    && (header[0] & 0xFF) == 0x89
                    && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                    && header[4] == 0x0D && header[5] == 0x0A
                    && header[6] == 0x1A && header[7] == 0x0A) {
                return "png";
            }
            if (header.length >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                return "webp";
            }
        } catch (IOException ex) {
            throw new BusinessException(40001, "无法读取上传文件");
        }
        throw new BusinessException(41502, "仅支持 JPG/PNG/WebP 图片");
    }
}
