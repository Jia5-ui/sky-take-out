package com.sky.aspect;


import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Component
@Aspect
@Slf4j
public class AutoFillAspect {

    /**
     * 定义一个切入点，表示匹配com.sky.mapper包下的所有方法，并且这些方法上必须有@AutoFill注解
     */
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {}


    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) throws Exception {
        log.info("进行自动填充");

        //获得数据库操作方法
        MethodSignature signature = (MethodSignature) joinPoint.getSignature(); //获得方法签名
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class); //获得方法上的AutoFill注解
        OperationType operationType = autoFill.value();

        //获得被拦截的实体对象
        Object[] args = joinPoint.getArgs();
        Object entity = args[0];

        //准备修改数据
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        //根据不同操作类型，进行自动填充
        if (operationType == OperationType.INSERT){
            Method setCreateTime = (Method) entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class).invoke(entity, now);
            Method setUpdateTime = (Method) entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class).invoke(entity, now);
            Method setUpdateUser = (Method) entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class).invoke(entity, currentId);
            Method setCreateUser = (Method) entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class).invoke(entity, currentId);

        }else if (operationType == OperationType.UPDATE){
            Method setUpdateTime = (Method) entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class).invoke(entity, now);
            Method setUpdateUser = (Method) entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class).invoke(entity, currentId);
        }
    }
}
