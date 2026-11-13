package com.sky.controller.admin;


import com.github.pagehelper.Page;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.dishService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Api(tags = "菜品管理")
public class DishController {

    @Autowired
    private dishService dishService;

    /**
     * 新增菜品
     * @param dishDto
     */
    @PostMapping
    @ApiOperation("新增菜品")
    public Result saveWithFlavor(@RequestBody DishDTO dishDto) {

        log.info("新增菜品：{}", dishDto);

        dishService.saveWithFlavor(dishDto);

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

        return Result.success();
    }
}
