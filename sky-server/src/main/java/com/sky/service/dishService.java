package com.sky.service;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

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

    /**
     * 根据id查询菜品
     * @param id
     */
    DishVO getByIdWithFlavor(Long id);

    /**
     * 修改菜品
     * @param dishDto
     */
    void updateWithFlavor(DishDTO dishDto);
}
