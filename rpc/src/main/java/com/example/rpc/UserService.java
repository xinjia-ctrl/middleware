package com.example.rpc;

public interface UserService {

    User getUserByUserId(Integer userId);

    User findByName(String name);
}
