package com.nxg.mall.api;

import com.nxg.mall.common.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

@SpringBootTest
public class JwtTokenGeneratorTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    public void batchGenerateTokens() {
        // 1. 指定你想把 tokens.txt 保存在本地的什么路径（比如D盘根目录）
        String filePath = "D:/tokens.txt";

        try (FileWriter fw = new FileWriter(filePath);
             PrintWriter pw = new PrintWriter(fw)) {

            // 2. 循环生成 50 个用户的 Token
            for (int i = 1; i <= 50; i++) {
                Long userId = 1000L + i;         // 模拟用户ID：1001, 1002...
                String username = "test_user_" + i; // 模拟用户名：test_user_1...
                String role = "ROLE_USER";          // 模拟角色标识

                // 3. 调用你的 JwtUtil 生成 Token
                String token = jwtUtil.generateToken(userId, username, role);

                // 4. 按行写入到文件中
                pw.println(token);
            }

            System.out.println("==================================================");
            System.out.println("🎉 成功！50个测试Token已写入文件: " + filePath);
            System.out.println("==================================================");

        } catch (IOException e) {
            System.err.println("写入文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
