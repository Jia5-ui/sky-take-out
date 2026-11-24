package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;

import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import io.jsonwebtoken.lang.Collections;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户提交订单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        //1.处理业务异常（如地址簿为空、购物车为空等）
        //1.1 地址簿为空
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //1.2 购物车为空
        Long userId = BaseContext.getCurrentId();

        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new AddressBookBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        //2.向订单表插入一条订单
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setUserId(userId);
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(addressBook.getDetail());

        orderMapper.insert(orders);

        Long orderId = orders.getId();
        //3.向订单明细表插入n条数据
        List<OrderDetail> orderDetailList = new ArrayList<>();

        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orderId);

            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);

        //4.清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        //5.封装VO
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orderId)
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);


        // 发送消息
        Map map = new HashMap<>();
        map.put("type", 1);//1表示来单提醒 2表示客户催单
        map.put("OrderId", ordersDB.getId());
        map.put("content", "订单号：" + outTradeNo + "已支付，请及时处理");

        //转成json并发送
        String jsonString = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
    }

    /**
     * 查询历史订单
     *
     * @param ordersPageQueryDTO
     * @return
     */
    public PageResult pageQueryForUser(OrdersPageQueryDTO ordersPageQueryDTO) {
        Long userId = BaseContext.getCurrentId();

        ordersPageQueryDTO.setUserId(userId);

        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        //状态为5已完成 6已取消 7退款的
        Page<Orders> page = orderMapper.list(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();

        if (page.getResult() != null && !page.getResult().isEmpty()) {
            for (Orders order : page.getResult()) {
                {
                    Long orderId = order.getId();
                    List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);

                    OrderVO orderVO = new OrderVO();
                    BeanUtils.copyProperties(order, orderVO);
                    orderVO.setOrderDetailList(orderDetailList);

                    list.add(orderVO);
                }
            }
        }


        return new PageResult(page.getTotal(), list);
    }

    /**
     * 根据id查询订单详情
     *
     * @param orderId
     * @return
     */
    public OrderVO getOrdersAndDetail(Long orderId) {
        Long userId = BaseContext.getCurrentId();

        Orders orders = orderMapper.getById(orderId);

        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 取消订单
     *
     * @param orderId
     */
    @Transactional
    public void cancelOrder(Long orderId) throws Exception {
        //根据id查询订单
        Orders ordersDB = orderMapper.getById(orderId);

        //判断非空
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //判断订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > Orders.TO_BE_CONFIRMED) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        //如果是待接单状态，需要退款
        if (ordersDB.getStatus() == Orders.TO_BE_CONFIRMED) {
            //调用微信支付接口，申请退款
            weChatPayUtil.refund(
                    ordersDB.getNumber(), //商户订单号
                    ordersDB.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01));//原订单金额

            orders.setPayStatus(Orders.REFUND);
        }

        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason("用户取消订单");
        //动态更新订单状态，只更新状态、取消时间、取消原因
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     *
     * @param orderId
     */
    public void reSubmitOrder(Long orderId) {
        //获得用户id
        Long userId = BaseContext.getCurrentId();

        //先找到当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orderId);

        //将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingcarts = orderDetailList.stream().map(orderDetail -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            //将原订单详情里面的菜品信息重新复制到购物车对象中(除id)
            BeanUtils.copyProperties(orderDetail, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());
        //添加购物车到数据库
        shoppingCartMapper.insertBatch(shoppingcarts);
        System.out.println(shoppingcarts);
    }

    /**
     * 条件查询订单：版本1
     *
     * @param ordersPageQueryDTO
     * @return
     */
//    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
//        //把符合条件的订单查出来
//        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
//        Page<Orders> ordersList = orderMapper.list(ordersPageQueryDTO);
//
//        List<OrderVO> list = new ArrayList<>();
//        if (ordersList != null && !ordersList.isEmpty()) {
//            for (Orders orders : ordersList) {
//                OrderVO orderVO = new OrderVO();
//                BeanUtils.copyProperties(orders, orderVO);
//                //设置订单详情
//                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());
//                orderVO.setOrderDishes(orderDetailList.stream()
//                        .map(orderDetail -> {
//                            String dishName = orderDetail.getName();
//                            String dishFlavor = orderDetail.getDishFlavor();
//                            Integer number = orderDetail.getNumber();
//                            return "订单详情：" + dishName + " " + dishFlavor + " " + number;
//                        })
//                        .collect(Collectors.joining("；")));
//
//                list.add(orderVO);
//            }
//        }
//
//        return new PageResult(ordersList.getTotal(), list);
//    }

    /**
     * 条件查询订单：版本2
     *
     * @param ordersPageQueryDTO
     * @return
     */
    //封装结果对象并返回
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> page = orderMapper.list(ordersPageQueryDTO);

        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }

    // 将Orders转化为OrderVO
    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        List<Orders> ordersList = page.getResult();

        List<OrderVO> orderVOList = new ArrayList<>();
        if (!Collections.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                //将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);

                //设置订单详情字符串
                String orderDishesStr = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishesStr);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    //根据id获取订单菜品字符串
    private String getOrderDishesStr(Orders orders) {
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            String res = orderDetailList.stream().map(x -> {
                String dish = x.getName() + "*" + x.getNumber();
                return dish;
            }).collect(Collectors.joining(";"));
            return res;
        }
        return null;


    }

    /**
     * 订单统计方案一：使用 group by
     *
     * @return
     */
    //TODO:高并发情况测试
//    public OrderStatisticsVO statistics() {
//        List<Map<String, Object>> orderStatistics = orderMapper.getCountGroupByStatus();
////        group by 特点：**没有数据的状态，不会返回这条 map**，需要手动设置
//        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
//        orderStatisticsVO.setToBeConfirmed(0);
//        orderStatisticsVO.setConfirmed(0);
//        orderStatisticsVO.setDeliveryInProgress(0);
//
//        if (!CollectionUtils.isEmpty(orderStatistics)) {
//            for (Map<String, Object> map : orderStatistics) {
//                Integer status = (Integer) map.get("status");
//                Integer count = 0;
//                if (status != null) {
//                    Long countLong = (Long) map.get("count");
//                    count = countLong.intValue();
//                }
//
//                //包装类不能用 == 判断,大于 127 会新建对象，== 比较地址，会匹配失败。
//                if (Orders.TO_BE_CONFIRMED.equals(status)) {
//                    orderStatisticsVO.setToBeConfirmed(count);
//                } else if (Orders.CONFIRMED.equals(status)) {
//                    orderStatisticsVO.setConfirmed(count);
//                } else if (Orders.DELIVERY_IN_PROGRESS.equals(status)) {
//                    orderStatisticsVO.setDeliveryInProgress(count);
//                }
//            }
//        }
//
//        return orderStatisticsVO;
//    }

    /**
     * 订单统计方案二：分别查询
     *
     * @return
     */
    //TODO:高并发情况测试
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

}
