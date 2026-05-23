package com.example.service;

import com.example.entity.User;
import java.util.List;

public interface UserService {

    User selectById(Integer id);

    List<User> selectAll();

    int insertUser(User user);

    void deleteById(Integer id);
}
