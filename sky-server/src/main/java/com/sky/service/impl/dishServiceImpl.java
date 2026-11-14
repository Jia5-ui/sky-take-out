package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.annotation.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.enumeration.OperationType;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.FlavorMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.dishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class dishServiceImpl implements dishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private FlavorMapper flavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品
     *
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
        System.out.println("id" + dishFavorId);

        //2.保存菜品口味信息,需要flavor
        List<DishFlavor> flavors = dishDto.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(df -> df.setDishId(dishFavorId));
        }
        flavorMapper.insertBatch(flavors);

    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult queryPage(DishPageQueryDTO dishPageQueryDTO) {
        //TODO:改成MyBatis Plus的分页查询
//        Page<DishVO> page = new Page<>(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
//        Page<DishVO> result = dishMapper.queryPage(dishPageQueryDTO);

        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> result = dishMapper.queryPage(dishPageQueryDTO);

        return new PageResult(result.getTotal(), result.getResult());
    }

    /**
     * 删除菜品
     *
     * @param ids
     */
    @Transactional
    public void deleteDish(List<Long> ids) {

        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            //判断菜品是否在售,有一个在售就抛出业务异常
            if (Objects.equals(dish.getStatus(), StatusConstant.ENABLE)) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //判断关联套餐吗
        List<Long> relateIds = setmealDishMapper.getSetmealIdsByDishId(ids);
        if (relateIds != null && !relateIds.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //删除菜品及其关联口味
//        for (Long id : ids) {
//            dishMapper.deleteById(id);
//            flavorMapper.deleteByDishId(id);
//        }

        //批量删除菜品及其关联口味
        dishMapper.deleteByIds(ids);
        flavorMapper.deleteByDishIds(ids);


    }

    /**
     * 根据id查询菜品
     * @param id
     */
    public DishVO getByIdWithFlavor(Long id) {
        DishVO dishVO = new DishVO();

        //根据id查菜品
        Dish dish = dishMapper.getById(id);
        BeanUtils.copyProperties(dish,dishVO);

        //根据id查口味
        List<DishFlavor> flavors = flavorMapper.getByDishId(id);
        dishVO.setFlavors(flavors);

        return dishVO;
    }

    /**
     * 修改菜品
     * @param dishDto
     */
    public void updateWithFlavor(DishDTO dishDto) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDto,dish);

        //修改基础信息
        dishMapper.update(dish);

        Long id = dish.getId();
        //修改口味
        //先删除所有再重新添加
        flavorMapper.deleteByDishId(id);
        List<DishFlavor> flavors = dishDto.getFlavors();

        if (flavors!=null && !flavors.isEmpty()){
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(id));
            flavorMapper.insertBatch(flavors);
        }
    }


}
