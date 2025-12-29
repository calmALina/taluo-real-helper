package com.taluo.taluorealhelper.ai.Tool;

import com.taluo.taluorealhelper.DTO.Result;
import com.taluo.taluorealhelper.service.IUserService;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SqlTool {

    @Resource
    private IUserService userService;

    /**
     * 创建用户工具
     * 注意：@Tool 注解只需要 value 参数（描述），不需要 name
     */
    @Tool("根据提供的手机号码创建一个新用户。当用户要求注册、创建账号或添加新用户时使用此工具。")
    public String createUserByPhone(
            @P("用户的手机号码，必须是有效的中国大陆手机号，例如：13800138000") String phone) {

        log.info("AI 调用创建用户工具，手机号: {}", phone);

        try {
            Result result = userService.CreateUser(phone);
            if (result.getSuccess()) {
                log.info("用户创建成功: {}", phone);
                return "成功创建用户！手机号: " + phone + "。用户信息: " + result.getData();
            } else {
                log.warn("用户创建失败: {}, 原因: {}", phone, result.getErrorMsg());
                return "创建用户失败: " + result.getErrorMsg();
            }
        } catch (Exception e) {
            log.error("创建用户时发生异常: {}", phone, e);
            return "创建用户时发生错误: " + e.getMessage();
        }
    }

    /**
     * 查询用户信息工具（可选添加）
     */
    @Tool("根据手机号查询用户信息。当需要查看用户详情或验证用户是否存在时使用。")
    public String getUserByPhone(
            @P("要查询的用户手机号") String phone) {

        log.info("AI 调用查询用户工具，手机号: {}", phone);

        try {
            // 假设你的 Service 有查询方法
            Result result = userService.getUserByPhone(phone);
            if (result.getSuccess()) {
                return "查询成功！用户信息: " + result.getData();
            } else {
                return "未找到该用户: " + phone;
            }
        } catch (Exception e) {
            log.error("查询用户时发生异常: {}", phone, e);
            return "查询用户时发生错误: " + e.getMessage();
        }
    }
}