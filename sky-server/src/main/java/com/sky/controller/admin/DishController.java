package com.sky.controller.admin;


import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Api(tags = "菜品管理")
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     * @param dishDto
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result saveWithFlavor(@RequestBody DishDTO dishDto) {

        log.info("新增菜品：{}", dishDto);

        dishService.saveWithFlavor(dishDto);

        //对展示菜品修改时，同时清理缓存
        String key = "dish_"+dishDto.getCategoryId();
//        redisTemplate.delete(key);
        cleanCache(key);

        return Result.success();
    }

    /**
     * 菜品分页查询
     * @return
     */

    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> queryPage(DishPageQueryDTO dishPageQueryDTO){
        log.info("菜品分页查询：{}", dishPageQueryDTO);

        PageResult pageResult = dishService.queryPage(dishPageQueryDTO);

        return Result.success(pageResult);
    }

    /**
     * 删除菜品
     * @param ids
     */
    @DeleteMapping
    @ApiOperation("删除菜品")
    public Result deleteDish(@RequestParam List<Long> ids){
        log.info("删除菜品");

        dishService.deleteDish(ids);

        //对展示菜品修改时，同时清理缓存,涉及多个分类或者不好获取分类id时全部删除
//        Set keys = redisTemplate.keys("dish_*");
//        redisTemplate.delete(keys);
        cleanCache("dish_*");

        return Result.success();
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id){
        log.info("根据id查询菜品:{}",id);

        DishVO dishVO = dishService.getByIdWithFlavor(id);

        return Result.success(dishVO);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDto){
        log.info("修改菜品");
        dishService.updateWithFlavor(dishDto);

        //对展示菜品修改时，同时清理缓存,涉及多个分类或者不好获取分类id时全部删除
//        Set keys = redisTemplate.keys("dish_*");
//        redisTemplate.delete(keys);
        cleanCache("dish_*");

        return Result.success();
    }

    /**
     * 修改菜品状态
     * @param status
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("修改菜品状态")
    public Result updateStatus(@PathVariable Integer status,Long id){
        log.info("修改菜品状态");
        dishService.updateStatus(status,id);

        //对展示菜品修改时，同时清理缓存,涉及多个分类或者不好获取分类id时全部删除
//        Set keys = redisTemplate.keys("dish_*");
//        redisTemplate.delete(keys);
        cleanCache("dish_*");

        return Result.success();
    }


    private void cleanCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }
}
