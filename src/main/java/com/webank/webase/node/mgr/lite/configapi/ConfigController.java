/**
 * Copyright 2014-2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.webank.webase.node.mgr.lite.configapi;

import com.webank.webase.node.mgr.lite.base.code.ConstantCode;
import com.webank.webase.node.mgr.lite.base.entity.BaseResponse;
import com.webank.webase.node.mgr.lite.base.tools.IPUtil;
import com.webank.webase.node.mgr.lite.config.WebMvcConfig;
import com.webank.webase.node.mgr.lite.config.properties.ConstantProperties;
import com.webank.webase.node.mgr.lite.config.properties.VersionProperties;
import com.webank.webase.node.mgr.lite.configapi.entity.ServerInfo;
import lombok.extern.log4j.Log4j2;
import org.fisco.bcos.sdk.crypto.CryptoSuite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * return common configure of local server
 */
@Log4j2
@RestController
@RequestMapping("config")
public class ConfigController {

    @Autowired
    private CryptoSuite cryptoSuite;
    @Autowired
    private WebMvcConfig webMvcConfig;
    @Autowired
    private ConstantProperties constantProperties;
    @Autowired
    private VersionProperties versionProperties;

    /**
     * return encrypt type to web 0 is standard, 1 is guomi.
     */
    @GetMapping("/encrypt")
    public Object getEncryptType() {
        int encrypt = cryptoSuite.cryptoTypeConfig;
        log.info("getEncryptType:{}", encrypt);
        return new BaseResponse(ConstantCode.SUCCESS, encrypt);
    }

    /**
     * webase-web: when add first front, return version and tips.
     * 
     * @return
     */
    @GetMapping("/version")
    public String getServerVersion() {
        return versionProperties.getVersion();
    }

    /**
     * return server ipPort.
     */
    @GetMapping("/ipPort")
    public BaseResponse getIpPort() {
        log.info("getIpPort.");
        String ip = IPUtil.getLocalIp();
        Integer port = webMvcConfig.getPort();
        return new BaseResponse(ConstantCode.SUCCESS, new ServerInfo(ip, port));
    }

    /**
     * if deployed contract can be modified.
     */
    @GetMapping("/isDeployedModifyEnable")
    public BaseResponse isDeployedModifyEnable() {
        boolean isDeployedModifyEnable = constantProperties.isDeployedModifyEnable();
        log.info("isDeployedModifyEnable:{}", isDeployedModifyEnable);
        return new BaseResponse(ConstantCode.SUCCESS, isDeployedModifyEnable);
    }

}
