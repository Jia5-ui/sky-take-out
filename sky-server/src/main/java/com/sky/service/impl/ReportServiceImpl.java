package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;


    /**
     * 营业额统计
     */
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        //获取从begin到end的所有日期，放入集合List<LocalDate>
        List<LocalDate> date = new ArrayList<>();

        while(!begin.isAfter(end)){
            date.add(begin);
            begin = begin.plusDays(1);
        }

        //生成字符串dateList
        String dateList = StringUtils.join(date, ",");

        //获取每天的营业额(已完成的订单)
        //select sum(amount) from orders where order_time between beginTime and endTime and status = 5
        List<Double> turnover = new ArrayList<>();

        for (LocalDate localDate : date) {
            //order_time是LocalDateTime类型; begin和end是LocalDate类型，需要转换
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            //把条件封装到map集合
            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);

            Double amountOneDay = orderMapper.getTurnoverByMap(map);

            //如果有一天没有营业额，amountOneDay为null，需要进行判断
            amountOneDay = amountOneDay==null?0.0:amountOneDay;

            turnover.add(amountOneDay);
        }

        //生成字符串turnoverList
        String turnoverList = StringUtils.join(turnover, ",");


        return TurnoverReportVO.builder()
                .dateList(dateList)
                .turnoverList(turnoverList)
                .build();
    }

    /**
     * 用户统计
     */
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        //获取从begin到end的所有日期，放入集合List<LocalDate>
        List<LocalDate> date = new ArrayList<>();

        while(!begin.isAfter(end)){
            date.add(begin);
            begin = begin.plusDays(1);
        }

        //生成字符串dateList
        String dateList = StringUtils.join(date, ",");

        //获取每天的用户数量
        //select count(*) from user where create_time between '2025-11-10 14:55:40' and '2027-11-20 14:55:40';
        List<Integer> userCount = new ArrayList<>();
        //获取每天的新增用户数量
        List<Integer> newUserCount = new ArrayList<>();

        for (LocalDate localDate : date) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            //把条件封装到map集合
            Map map = new HashMap();
            map.put("end", endTime);
            //总用户数量
            Integer count = userMapper.countByMap(map);
            count = count == null ? 0 : count;

            map.put("begin", beginTime);
            //新增用户数量
            Integer newCount = userMapper.countByMap(map);
            newCount = newCount == null ? 0 : newCount;

            newUserCount.add(newCount);
            userCount.add(count);
        }

        return UserReportVO.builder()
                .dateList(dateList)
                .totalUserList(StringUtils.join(userCount, ","))
                .newUserList(StringUtils.join(newUserCount, ","))
                .build();
    }

    /**
     * 订单统计
     */
    public OrderReportVO orderStatistics(LocalDate begin, LocalDate end) {
        //获取从begin到end的所有日期，放入集合List<LocalDate>
        List<LocalDate> date = new ArrayList<>();

        while(!begin.isAfter(end)){
            date.add(begin);
            begin = begin.plusDays(1);
        }

        //获取每天的订单数量，已完成的订单，从begin-end的总订单数量，已完成的订单
        List<Integer> orderCountDay = new ArrayList<>();
        List<Integer> validOrderCountDay = new ArrayList<>();
        Integer totalOrderCount = 0;
        Integer validOrderCount = 0;

        // 遍历日期集合
        for (LocalDate localDate : date) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            //每天订单数量
            Integer count = getOrderCount(beginTime, endTime, null);
            orderCountDay.add(count);

            //累计订单数量
            totalOrderCount+=count;


            //每天完成的订单数量
            Integer validCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);
            validOrderCountDay.add(validCount);

            //累计完成的订单数量
            validOrderCount+=validCount;

        }
        
        //// 对于累计订单数量，累计完成的订单数量可以用stream流进行求和
//        Integer totalOrderCount = orderCountDay.stream().reduce(0, Integer::sum);
//        Integer validOrderCount = validOrderCountDay.stream().reduce(0, Integer::sum);

        // 订单完成率
        Double rate = 0.0;
        if (totalOrderCount != 0) {
            rate = validOrderCount.doubleValue() / totalOrderCount;
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(date, ","))
                .orderCountList(StringUtils.join(orderCountDay, ","))
                .validOrderCountList(StringUtils.join(validOrderCountDay, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(rate)
                .build();
    }
    /**
     * 获取订单数量
     */
    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);

        Integer count = orderMapper.getOrderCountByMap(map);

        return count == null ? 0 : count;
    }
}
