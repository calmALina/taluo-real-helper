package com.taluo.taluorealhelper.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.taluo.taluorealhelper.DTO.LoginFormDTO;
import com.taluo.taluorealhelper.DTO.Result;
import com.taluo.taluorealhelper.entity.User;
import jakarta.servlet.http.HttpSession;

public interface IUserService extends IService<User> {
    Result CreateUser(String phone);

    Result getUserByPhone(String phone);
}
