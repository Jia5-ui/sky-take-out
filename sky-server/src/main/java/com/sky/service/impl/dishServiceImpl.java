package com.sky.service.impl;

import com.sky.annotation.AutoFill;
import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import com.sky.mapper.DishMapper;
import com.sky.mapper.FlavorMapper;
import com.sky.service.dishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class dishServiceImpl implements dishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private FlavorMapper flavorMapper;

    /**
     * 新增菜品
     * @param dishDto
     */
    @Transactional
    public void saveWithFlavor(DishDTO dishDto) {
        //1.保存菜品基本信息,不需要flavor
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDto, dish);
        dishMapper.insert(dish);

        //获取菜品id，即为口味外键
        Long dishFavorId = dish.getId();
        System.out.println("id"+dishFavorId);

        //2.保存菜品口味信息,需要flavor
        List<DishFlavor> flavors = dishDto.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(df->df.setDishId(dishFavorId));
        }
        flavorMapper.insertBatch(flavors);

    }
}
