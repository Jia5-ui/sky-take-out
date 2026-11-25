package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Slf4j
@RestController
@Api("报表相关接口")
@RequestMapping("/admin/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 营业额统计
     */
    @GetMapping("/turnoverStatistics")
    @ApiOperation("营业额统计")
    public Result<TurnoverReportVO> turnoverStatistics(
            @DateTimeFormat(fallbackPatterns = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(fallbackPatterns = "yyyy-MM-dd")LocalDate end) {

        log.info("营业额统计，开始时间：{}，结束时间：{}", begin, end);
        TurnoverReportVO turnoverReportVO = reportService.turnoverStatistics(begin, end);

        return Result.success(turnoverReportVO);
    }

    /**
     * 用户统计
     */
    @GetMapping("/userStatistics")
    @ApiOperation("用户统计")
    public Result<UserReportVO> userStatistics(
            @DateTimeFormat(fallbackPatterns = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(fallbackPatterns = "yyyy-MM-dd")LocalDate end) {
        log.info("用户统计开始时间：{}，结束时间：{}", begin, end);
        UserReportVO userReportVO = reportService.userStatistics(begin, end);
        return Result.success(userReportVO);
    }

    /**
     * 订单统计
     */
    @GetMapping("/ordersStatistics")
    @ApiOperation("订单统计")
    public Result<OrderReportVO> orderStatistics(
            @DateTimeFormat(fallbackPatterns = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(fallbackPatterns = "yyyy-MM-dd") LocalDate end) {
        log.info("订单统计开始时间：{}，结束时间：{}", begin, end);
        OrderReportVO orderReportVO = reportService.orderStatistics(begin, end);
        return Result.success(orderReportVO);
    }

    /**
     * 销量排名
     */
    @GetMapping("/top10")
    @ApiOperation("销量排名")
    public Result<SalesTop10ReportVO> top10(
            @DateTimeFormat(fallbackPatterns = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(fallbackPatterns = "yyyy-MM-dd") LocalDate end) {
        log.info("销量排名开始时间：{}，结束时间：{}", begin, end);
        SalesTop10ReportVO salesTop10ReportVO = reportService.top10(begin, end);
        return Result.success(salesTop10ReportVO);
    }

}
