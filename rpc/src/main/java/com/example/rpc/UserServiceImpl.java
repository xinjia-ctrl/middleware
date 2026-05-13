package com.example.rpc;

public class UserServiceImpl implements UserService {

    @Override
    public User getUserByUserId(Integer userId) {
        return new User(userId, "用户" + userId, "user" + userId + "@example.com");
    }
}
