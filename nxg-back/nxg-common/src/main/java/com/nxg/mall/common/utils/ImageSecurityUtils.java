package com.nxg.mall.common.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 商品图片路径安全清洗与提取工具类
 */
public class ImageSecurityUtils {

    // 统一定义系统内图片存放的基准安全相对路径
    private static final String BASE_UPLOAD_PATH = "api/uploads/";

    private ImageSecurityUtils() {
        // 私有构造，防止实例化
    }

    /**
     * 核心安全清洗：无论前端传什么，都强制只提取纯文件名，并拼接固定的系统存储前缀，彻底免于路径穿越攻击
     *
     * @param rawPath 前端传过来的图片原始字符串 (可以是全路径 url、相对路径、或空值)
     * @return 纯文件名清洗并拼接后安全的系统相对路径，形如 "api/uploads/p29.jpg"
     */
    public static String sanitizeAndExtractFileName(String rawPath) {
        if (rawPath == null || rawPath.trim().isEmpty()) {
            return "";
        }

        // 1. 提取出最后一部分（纯文件名）
        String fileName = rawPath;
        int lastSlash = Math.max(rawPath.lastIndexOf('/'), rawPath.lastIndexOf('\\'));
        if (lastSlash != -1) {
            fileName = rawPath.substring(lastSlash + 1);
        }

        // 2. 移除操作系统敏感或非法的特殊字符，防止本地文件系统写入漏洞
        fileName = fileName.replaceAll("[\\\\:*?\"<>|%]", "");

        if (fileName.trim().isEmpty()) {
            return "";
        }

        // 3. 终极防线：利用 NIO 路径规范化校验。检测是否能跑出我们在系统里定死的边界
        try {
            Path targetPath = Paths.get(BASE_UPLOAD_PATH, fileName).normalize();
            // 如果规范化后的路径不是以系统前缀开头，说明遭遇了绕过或路径穿越测试
            if (!targetPath.toString().replace("\\", "/").startsWith(BASE_UPLOAD_PATH)) {
                return ""; // 拒绝非法访问
            }
            return targetPath.toString().replace("\\", "/");
        } catch (Exception e) {
            return ""; // 异常路径一律拒绝
        }
    }

    /**
     * 批量处理多图（例如轮播图、图集字符串，以逗号分隔）的安全清洗与格式化
     *
     * @param subImages 以逗号分割的原始图片字符串
     * @return 清洗拼接后安全的逗号分隔相对路径字符串
     */
    public static String processMultiplePathsSecurely(String subImages) {
        if (subImages == null || subImages.trim().isEmpty()) {
            return "";
        }
        return Arrays.stream(subImages.split(","))
                .map(ImageSecurityUtils::sanitizeAndExtractFileName)
                .filter(path -> !path.isEmpty() && !path.equals(BASE_UPLOAD_PATH))
                .collect(Collectors.joining(","));
    }
}
