/*
 * Copyright 2014-2019  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.webank.webase.node.mgr.transhash.entity;

import java.math.BigInteger;
import lombok.Data;

@Data
public class TransactionInfo {
    private String hash;
    private String nonce;
    private String blockHash;
    private BigInteger blockNumber;
    private int transactionIndex;
    private String from;
    private String to;
    private int value;
    private Long gasPrice;
    private Long gas;
    private String input;
    private int v;
    private String nonceRaw;
    private String blockNumberRaw;
    private String transactionIndexRaw;
    private String gasPriceRaw;
    private String gasRaw;
}