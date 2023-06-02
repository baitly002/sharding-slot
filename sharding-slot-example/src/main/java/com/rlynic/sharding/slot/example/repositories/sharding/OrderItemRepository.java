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

package com.rlynic.sharding.slot.example.repositories.sharding;


import com.rlynic.sharding.slot.example.entities.OrderItem;
import com.rlynic.sharding.slot.example.repositories.CommonRepository;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderItemRepository extends CommonRepository<OrderItem, Long> {

    public List<OrderItem> selectIn();

    public List<OrderItem> selectInStatic();

    public List<OrderItem> selectInIds(@Param("ids") List<Long> ids);

    public List<OrderItem> selectInIdsStatic(@Param("ids") List<Long> ids);

    public List<OrderItem> selectInIdsStaticForBracket(@Param("ids") List<Long> ids);

    public List<OrderItem> selectInIdsStaticForUpdate(@Param("ids") List<Long> ids);

    public int replaceIntoSingle(OrderItem item);

    public int replaceIntoList(@Param("items")List<OrderItem> items);

    public List<OrderItem> selectInIdsAlias(@Param("ids") List<Long> ids);

    public int updateInIds(@Param("ids") List<Long> ids);
    public int deleteInIds(@Param("ids") List<Long> ids);

    public List<OrderItem> selectManyInIds(@Param("ids") List<Long> ids, @Param("iids") List<Long> iids);

    public List<OrderItem> selectManyNotInIds(@Param("ids") List<Long> ids, @Param("iids") List<Long> iids);

    public List<OrderItem> selectManyTableInIds(@Param("ids") List<Long> ids, @Param("oids") List<Long> oids);
}
