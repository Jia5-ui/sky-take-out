package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
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
}
