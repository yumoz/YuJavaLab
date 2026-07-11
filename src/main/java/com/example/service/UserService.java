package com.example.service;

import com.example.entity.User;
import java.util.List;

public interface UserService {

    User selectById(Integer id);

    List<User> selectAll();

    int insertUser(User user);

    int insertBatch(List<User> list);

    void deleteById(Integer id);
}
