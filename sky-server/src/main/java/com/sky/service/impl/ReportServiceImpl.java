package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;


    /**
     * 营业额统计
     */
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        //获取从begin到end的所有日期，放入集合List<LocalDate>
        List<LocalDate> date = new ArrayList<>();

        while (!begin.isAfter(end)) {
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
            amountOneDay = amountOneDay == null ? 0.0 : amountOneDay;

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

        while (!begin.isAfter(end)) {
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

        while (!begin.isAfter(end)) {
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
            totalOrderCount += count;


            //每天完成的订单数量
            Integer validCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);
            validOrderCountDay.add(validCount);

            //累计完成的订单数量
            validOrderCount += validCount;

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

//    /**
//     * 销量排名方法一
//     */
//    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {
//
//        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
//        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
//
//        // 查询销量前十的菜品
//        List<Map<String, Object>> top10 = orderMapper.getTop10(beginTime, endTime);
//
//
//        List<String> name = new ArrayList<>();
//        List<BigDecimal> number = new ArrayList<>();
//
//////        //遍历List，获取菜品名称和数量
////        name = top10.stream().map(x -> (String) x.get("name")).collect(Collectors.toList());
////        number = top10.stream().map(x -> (Long) x.get("number")).collect(Collectors.toList());
//
//
//
//        top10.forEach(map->{
//            if(map!=null){
//                String dname = (String) map.get("name");
//                BigDecimal dnumber = (BigDecimal) map.get("number");
//                name.add(dname);
//                number.add(dnumber==null?BigDecimal.ZERO:dnumber);
//            }
//        });
//
//        return SalesTop10ReportVO.builder()
//                .nameList(StringUtils.join(name, ","))
//                .numberList(StringUtils.join(number, ","))
//                .build();
//    }

    /**
     * 销量排名方法二
     */
    public SalesTop10ReportVO top10(LocalDate begin, LocalDate end) {

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        // 查询销量前十的菜品
        List<GoodsSalesDTO> top10 = orderMapper.getTop10ByGoodsSalesDTO(beginTime, endTime);


        List<String> name = top10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> number = top10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());


        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(name, ","))
                .numberList(StringUtils.join(number, ","))
                .build();
    }

    /**
     * 导出业务数据
     */
    public void exportBusinessData(HttpServletResponse response) {
        //1.查询数据库(30天的业务数据)
        LocalDate begin = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);

        BusinessDataVO data = workspaceService.getBusinessData(LocalDateTime.of(begin, LocalTime.MIN), LocalDateTime.of(end, LocalTime.MAX));

        //2.通过POI将数据写入到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            //基于模板创建一个工作簿
            XSSFWorkbook excel = new XSSFWorkbook(in);
            //获取第一个工作表
            XSSFSheet sheet = excel.getSheet("sheet1");

            //填充时间
            sheet.getRow(1).getCell(1).setCellValue("日期：" + begin + "至" + end);

            //填充概览
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(data.getTurnover());
            row.getCell(4).setCellValue(data.getOrderCompletionRate());
            row.getCell(6).setCellValue(data.getNewUsers());

            row = sheet.getRow(4);

            row.getCell(4).setCellValue(data.getUnitPrice());
            row.getCell(2).setCellValue(data.getValidOrderCount());

            //填充明细
            for (int i = 0; i < 30; i++) {
                //获取每一天的数据
                LocalDate date = begin.plusDays(i);

                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                //获取某行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            //3.将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //4.关闭流
            out.close();
            excel.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
