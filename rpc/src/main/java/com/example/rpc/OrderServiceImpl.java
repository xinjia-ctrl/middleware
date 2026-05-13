package com.example.rpc;

public class OrderServiceImpl implements OrderService {

    @Override
    public Integer getOrderCount(Integer userId) {
        return userId * 10;
    }
}
