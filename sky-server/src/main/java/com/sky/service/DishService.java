package com.sky.service;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {

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

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);

    /**
     * 修改菜品状态
     * @param status
     */
    void updateStatus(Integer status, Long id);
}
