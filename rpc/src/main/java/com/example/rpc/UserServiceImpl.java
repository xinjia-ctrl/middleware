package com.example.rpc;

public class UserServiceImpl implements UserService {

    @Override
    public User getUserByUserId(Integer userId) {
        return new User(userId, "用户" + userId, "user" + userId + "@example.com");
    }

    @Override
    public User findByName(String name) {
        return new User(99, name, name + "@example.com");
    }
}
