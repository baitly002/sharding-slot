/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rlynic.sharding.slot.example.services;

import com.rlynic.sharding.slot.example.ExampleService;
import com.rlynic.sharding.slot.example.entities.Order;
import com.rlynic.sharding.slot.example.entities.OrderItem;
import com.rlynic.sharding.slot.example.repositories.master.MasterOrderItemRepository;
import com.rlynic.sharding.slot.example.repositories.master.MasterOrderRepository;
import com.rlynic.sharding.slot.example.repositories.sharding.OrderItemRepository;
import com.rlynic.sharding.slot.example.repositories.sharding.OrderRepository;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
//@Primary
public class OrderServiceImpl implements ExampleService {
    private final static Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Resource
    private MasterOrderRepository masterOrderRepository;

    @Resource
    private MasterOrderItemRepository masterOrderItemRepository;

    @Resource
    private OrderRepository orderRepository;
    
    @Resource
    private OrderItemRepository orderItemRepository;
    
    @Override
    public void initEnvironment() throws SQLException {
//        orderRepository.dropTable();
//        orderItemRepository.dropTable();
        orderRepository.createTableIfNotExists();
        orderRepository.createTableIfNotExistsUndoLog();
        orderItemRepository.createTableIfNotExists();
        orderRepository.deleteAll();
        orderItemRepository.deleteAll();

        //MASTER库
//        masterOrderRepository.dropTable();
//        masterOrderItemRepository.dropTable();
        masterOrderRepository.createTableIfNotExists();
        masterOrderRepository.createTableIfNotExistsUndoLog();
        masterOrderItemRepository.createTableIfNotExists();
        masterOrderRepository.deleteAll();
        masterOrderItemRepository.deleteAll();
    }

    @Override
    public void selectIn() throws SQLException {
        OrderItem t1 = new OrderItem();
        t1.setOrderId(840637593400901632L);
        t1.setUserId(2);
        t1.setStatus("insertInIds");
        OrderItem t2 = new OrderItem();
        t2.setOrderId(840640867369746432L);
        t2.setUserId(2);
        t2.setStatus("insertInIds");
        OrderItem t3 = new OrderItem();
        t3.setOrderId(840637008681369600L);
        t3.setUserId(2);
        t3.setStatus("insertInIds");
        orderItemRepository.insert(t1);
        orderItemRepository.insert(t2);
        orderItemRepository.insert(t3);
        List<Long> insertIds = new ArrayList<>();
        insertIds.add(t1.getOrderId());
        insertIds.add(t2.getOrderId());
        insertIds.add(t3.getOrderId());
        int uret = orderItemRepository.updateInIds(insertIds);
        int dret = orderItemRepository.deleteInIds(insertIds);
        List<OrderItem> items = orderItemRepository.selectIn();
        List<OrderItem> items2 = orderItemRepository.selectInStatic();
        List<Long> ids = Arrays.asList(840637597557456896L, 840637598182408192L, 840637599944015872L);

        List<OrderItem> itemList = orderItemRepository.selectInIds(ids);
        List<OrderItem> itemList2 = orderItemRepository.selectInIdsStatic(ids);

        List<OrderItem> itemAliasList = orderItemRepository.selectInIdsAlias(ids);

        List<Long> iids = Arrays.asList(840637595078623233L, 840637605203673089L);
        List<OrderItem> itemManyInList = orderItemRepository.selectManyInIds(ids, iids);

        List<OrderItem> itemManyNotInList = orderItemRepository.selectManyNotInIds(ids, iids);

        List<Long> oids = Arrays.asList(840637599944015872L, 840637608592670720L, 840637918484627456L, 840637088687718400L, 840637091057500160L);
        List<OrderItem> itemManyTableInList = orderItemRepository.selectManyTableInIds(ids, oids);
        System.out.println(itemManyTableInList.size());


    }

    
    @Override
    public void cleanEnvironment() throws SQLException {
        orderRepository.dropTable();
        orderItemRepository.dropTable();
    }
    
    @Override
    @Transactional
    public void processSuccess() throws SQLException {
        System.out.println("-------------- Process Success Begin ---------------");
        List<Long> orderIds = insertData();
        printData();
////        deleteData(orderIds);
////        printData();
        System.out.println("-------------- Process Success Finish --------------");
    }

//    @GlobalTransactional(timeoutMills = 60000, name = "test-test")
//    @Transactional
    public void processSeataFail() throws Exception {
        List<Long> result = new ArrayList<>(10);
        for (int i = 1; i <= 10; i++) {
            Order order = new Order();
//            order.setOrderId(100+i);//设置主键  seata要求主键要传数据，要么是自增  sharding生成的主键数据拿不到
            order.setUserId(i);
            order.setAddressId(i);
            order.setStatus("INSERT_SEATA");
            orderRepository.insert(order);
            OrderItem item = new OrderItem();
//            item.setOrderItemId(200+i);
            item.setOrderId(order.getOrderId());
            item.setUserId(i);
            item.setStatus("INSERT_SEATA");
            orderItemRepository.insert(item);
            result.add(order.getOrderId());
        }
        Order order = new Order();
//        order.setOrderId(1000);
        order.setUserId(10);
        order.setAddressId(20);
        order.setStatus("INSERT_SEATA_MASTER");
        masterOrderRepository.insert(order);
        throw new RuntimeException("seata-测试异常回滚");
    }
    
    @Override
    @Transactional
    public void processFailure() throws SQLException {
        System.out.println("-------------- Process Failure Begin ---------------");
        insertData();
        System.out.println("-------------- Process Failure Finish --------------");
        throw new RuntimeException("Exception occur for transaction test.");
    }

    private List<Long> insertData() throws SQLException {
        System.out.println("---------------------------- Insert Data ----------------------------");
        List<Long> result = new ArrayList<>(10);
        for (int i = 1; i <= 10; i++) {
            Order order = new Order();
            order.setUserId(i);
            order.setAddressId(i);
            order.setStatus("INSERT_TEST");
            orderRepository.insert(order);
            OrderItem item = new OrderItem();
            item.setOrderId(order.getOrderId());
            item.setUserId(i);
            item.setStatus("INSERT_TEST");
            orderItemRepository.insert(item);
            result.add(order.getOrderId());
        }
        return result;
    }

    private void deleteData(final List<Long> orderIds) throws SQLException {
        System.out.println("---------------------------- Delete Data ----------------------------");
        for (Long each : orderIds) {
            orderRepository.delete(each);
            orderItemRepository.delete(each);
        }
    }
    
    @Override
    public void printData() throws SQLException {
        System.out.println("---------------------------- Print Order Data -----------------------");
        for (Object each : orderRepository.selectAll()) {
            System.out.println(each);
        }
        System.out.println("---------------------------- Print OrderItem Data -------------------");
        for (Object each : orderItemRepository.selectAll()) {
            System.out.println(each);
        }
    }
}
