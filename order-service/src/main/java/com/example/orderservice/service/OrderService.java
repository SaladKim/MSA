package com.example.orderservice.service;

import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.jpa.OrderEntity;

public interface OrderService {
    OrderDto createOrder(OrderDto orderDetails);

    OrderDto getOrderByOrderId(String orderId);

    Iterable<OrderEntity> getOrdersByUserid(String userId);//Iterable 인터페이스 안에는 iterator 메소드가 추상메소드로 선언, Iterator 인터페이스는 컬렉션클래스의 데이터를 하나씩 읽어올 때 사용
}
