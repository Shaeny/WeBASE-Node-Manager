/**
 * Copyright 2014-2021  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.webank.webase.node.mgr.lite.scheduler;


import com.webank.webase.node.mgr.lite.base.enums.DataStatus;
import com.webank.webase.node.mgr.lite.block.BlockService;
import com.webank.webase.node.mgr.lite.config.properties.ConstantProperties;
import com.webank.webase.node.mgr.lite.group.GroupService;
import com.webank.webase.node.mgr.lite.group.entity.TbGroup;
import com.webank.webase.node.mgr.lite.transaction.TransHashService;
import com.webank.webase.node.mgr.pro.statistic.StatService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * delete block/trans/monitorTrans data task
 * related: yml-constant-transRetainMax
 */
@Log4j2
@Component
@ConditionalOnProperty(name = "constant.isDeleteInfo", havingValue = "true")
public class DeleteInfoTask {

    @Autowired
    private GroupService groupService;
    @Autowired
    private BlockService blockService;
    @Autowired
    private TransHashService transHashService;
    @Autowired
    private ConstantProperties cProperties;
    @Autowired
    private StatService statService;


    @Scheduled(cron = "${constant.deleteInfoCron}")
    public void taskStart() {
       deleteInfoStart();
    }

    /**
     * start.
     */
    public void deleteInfoStart() {
        Instant startTime = Instant.now();
        log.debug("start deleteInfoStart. startTime:{}", startTime.toEpochMilli());
        //get group list
        List<TbGroup> groupList = groupService.getGroupList(DataStatus.NORMAL.getValue());
        if (groupList == null || groupList.size() == 0) {
            log.warn("DeleteInfoTask jump over, not found any group");
            return;
        }

        groupList.forEach(g -> deleteByGroupId(g.getGroupId()));

        log.debug("end deleteInfoStart useTime:{}",
            Duration.between(startTime, Instant.now()).toMillis());
    }

    /**
     * delete by groupId.
     */
    private void deleteByGroupId(int groupId) {
        //delete block
        deleteBlock(groupId);
        //delete transHash
        deleteTransHash(groupId);
    }


    /**
     * delete block.
     */
    private void deleteBlock(int groupId) {
        log.debug("start deleteBlock. groupId:{}", groupId);
        try {
            Integer removeCount = blockService.remove(groupId, cProperties.getBlockRetainMax());
            log.debug("end deleteBlock. groupId:{} removeCount:{}", groupId, removeCount);
        } catch (Exception ex) {
            log.error("fail deleteBlock. groupId:{}", groupId, ex);
        }
    }

    /**
     * delete transHash.
     */
    private void deleteTransHash(int groupId) {
        log.debug("start deleteTransHash. groupId:{}", groupId);
        try {
            Integer count = transHashService.queryCountOfTranByMinus(groupId);
            Integer removeCount = 0;
            if (count > cProperties.getTransRetainMax().intValue()) {
                Integer subTransNum = count - cProperties.getTransRetainMax().intValue();
                removeCount = transHashService.remove(groupId, subTransNum);
            }
            log.debug("end deleteTransHash. groupId:{} removeCount:{}", groupId, removeCount);
        } catch (Exception ex) {
            log.error("fail deleteTransHash. groupId:{}", groupId, ex);
        }
    }

}