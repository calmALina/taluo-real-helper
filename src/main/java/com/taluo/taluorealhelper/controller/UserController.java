package com.taluo.taluorealhelper.controller;

import com.taluo.taluorealhelper.DTO.LoginFormDTO;
import com.taluo.taluorealhelper.DTO.Result;
import com.taluo.taluorealhelper.DTO.UserDTO;
import com.taluo.taluorealhelper.entity.UserInfo;
import com.taluo.taluorealhelper.service.IUserInfoService;
import com.taluo.taluorealhelper.service.IUserService;
import com.taluo.taluorealhelper.utils.UserHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("ai")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

//    /**
//     * 发送手机验证码
//     */
//    @PostMapping("code")
//    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
//        //发送短信验证码并保存验证码
//        //return Result.fail("功能未完成");
//        return userService.sendCode(phone,session);
//    }

//    /**
//     * 登录功能
//     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
//     */
//    @PostMapping("/login")
//    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
//        // 实现登录功能
//        return userService.login(loginForm,session);
//    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(){
        // TODO 实现登出功能
        return Result.fail("功能未完成");
    }

    @GetMapping("/me")
    public Result me(){
        //获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

//    @GetMapping("/info/{id}")
//    public Result info(@PathVariable("id") Long userId){
//        // 查询详情
//        UserInfo info = userInfoService.getById(userId);
//        if (info == null) {
//            // 没有详情，应该是第一次查看详情
//            return Result.ok();
//        }
//        info.setCreateTime(null);
//        info.setUpdateTime(null);
//        // 返回
//        return Result.ok(info);
//    }
}
