package com.sky.service;
import com.sky.dto.DishDTO;

public interface dishService {

    /**
     * 新增菜品
     * @param dishDto
     */
    void saveWithFlavor(DishDTO dishDto);
}
