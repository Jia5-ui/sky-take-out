package com.sky.service;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;

import java.util.List;

public interface dishService {

    /**
     * 新增菜品
     * @param dishDto
     */
    void saveWithFlavor(DishDTO dishDto);

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    PageResult queryPage(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 删除菜品
     * @param ids
     */
    void deleteDish(List<Long> ids);
}
