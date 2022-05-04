package com.example.catalogservice.service;

import com.example.catalogservice.jpa.CatalogEntity;

public interface CatalogService {
    Iterable<CatalogEntity> getAllCatalogs(); //Iterable 인터페이스 안에는 iterator 메소드가 추상메소드로 선언, Iterator 인터페이스는 컬렉션클래스의 데이터를 하나씩 읽어올 때 사용
}
