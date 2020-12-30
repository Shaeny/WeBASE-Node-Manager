/**
 * Copyright 2014-2020 the original author or authors.
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

package com.webank.webase.node.mgr.deploy.service;

import com.webank.webase.node.mgr.base.code.ConstantCode;
import com.webank.webase.node.mgr.base.enums.ScpTypeEnum;
import com.webank.webase.node.mgr.base.exception.NodeMgrException;
import com.webank.webase.node.mgr.base.properties.ConstantProperties;
import com.webank.webase.node.mgr.base.tools.cmd.ExecuteResult;
import com.webank.webase.node.mgr.base.tools.cmd.JavaCommandExecutor;
import com.webank.webase.node.mgr.deploy.service.docker.DockerOptionsCmdImpl;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class AnsibleService {

    @Autowired
    private ConstantProperties constant;
    @Autowired
    private PathService pathService;
    @Autowired
    private DockerOptionsCmdImpl dockerOptionsCmd;

    private static final String NOT_FOUND_FLAG = "not found";
    /**
     * check ansible installed
     */
    public void checkAnsible() {
        log.info("checkAnsible installed");
//        String command = "ansible --version";
        String command = "ansible --version | grep \"ansible.cfg\"";
        ExecuteResult result = JavaCommandExecutor.executeCommand(command, constant.getExecShellTimeout());
        if (result.failed()) {
            throw new NodeMgrException(ConstantCode.ANSIBLE_NOT_INSTALLED.attach(result.getExecuteOut()));
        }
    }

    /**
     * ansible exec command
     */
    public void exec(String ip, String command) {
        String ansibleCommand = String.format("ansible %s -m command -a \"%s\"", ip, command);
        ExecuteResult result = JavaCommandExecutor.executeCommand(ansibleCommand, constant.getExecShellTimeout());
        if (result.failed()) {
            throw new NodeMgrException(ConstantCode.ANSIBLE_COMMON_COMMAND_ERROR.attach(result.getExecuteOut()));
        }
    }

    /**
     * check ansible ping, code is always 0(success)
     * @case1: ip configured in ansible, output not empty. ex: 127.0.0.1 | SUCCESS => xxxxx
     * @case2: if ip not in ansible's host, output is empty. ex: Exec command success: code:[0], OUTPUT:[]
     */
    public void execPing(String ip) {
        // ansible webase(ip) -m ping
//        String command = String.format("ansible %s -m command -a \"ping %s\"", ip, ip);
        String command = String.format("ansible %s -m ping", ip);
        ExecuteResult result = JavaCommandExecutor.executeCommand(command, constant.getExecShellTimeout());
        // if success
        if (result.getExecuteOut().contains(ip)) {
            log.info("execPing success output:{}", result.getExecuteOut());
            return;
        } else {
            throw new NodeMgrException(ConstantCode.ANSIBLE_PING_NOT_REACH.attach(result.getExecuteOut()));
        }
    }


    /**
     * copy, fetch, file(dir
     * scp: copy from local to remote, fetch from remote to local
     * todo check fetch, ex: new group, new node
     */
    public void scp(ScpTypeEnum typeEnum, String ip, String src, String dst) {
        log.info("scp typeEnum:{},ip:{},src:{},dst:{}", typeEnum, ip, src, dst);
        boolean isSrcDirectory = Files.isDirectory(Paths.get(src));
        boolean isSrcFile = Files.isRegularFile(Paths.get(src));
        // exec ansible copy or fetch
        String command;
        if (typeEnum == ScpTypeEnum.UP) {
            // handle file's dir local or remote
            if (isSrcFile) {
                // if src is file, create parent directory of dst on remote
                String parentOnRemote = Paths.get(dst).getParent().toAbsolutePath().toString();
                this.execCreateDir(ip, parentOnRemote);
            }
            if (isSrcDirectory) {
                // if src is directory, create dst on remote
                this.execCreateDir(ip, dst);
            }
            // synchronized cost less time
//            command = String.format("ansible %s -m copy -a \"src=%s dest=%s\"", ip, src, dst);
            command = String.format("ansible %s -m synchronize -a \"src=%s dest=%s\"", ip, src, dst);
            log.info("exec scp copy command: [{}]", command);
            ExecuteResult result = JavaCommandExecutor.executeCommand(command, constant.getExecShellTimeout());
            if (result.failed()) {
                throw new NodeMgrException(ConstantCode.ANSIBLE_SCP_COPY_ERROR.attach(result.getExecuteOut()));
            }
        } else { // DOWNLOAD
            // fetch file from remote
            if (isSrcDirectory) {
                // fetch not support fetch directory
                log.error("ansible fetch not support fetch directory!");
                throw new NodeMgrException(ConstantCode.ANSIBLE_FETCH_NOT_DIR);
            }
            // use synchronize, mode=pull
            // command = String.format("ansible %s -m fetch -a \"src=%s dest=%s\"", ip, src, dst);
            command = String.format("ansible %s -m synchronize -a \"mode=pull src=%s dest=%s\"", ip, src, dst);
            log.info("exec scp copy command: [{}]", command);
            ExecuteResult result = JavaCommandExecutor.executeCommand(command, constant.getExecShellTimeout());
            if (result.failed()) {
                throw new NodeMgrException(ConstantCode.ANSIBLE_SCP_COPY_ERROR.attach(result.getExecuteOut()));
            }
        }
    }

    // optimize fetch
//    command = String.format("ansible %s -m fetch -a \"src=%s dest=%s\"", ip, src, dst);
//            log.info("exec scp fetch command: [{}]", command);
//    ExecuteResult result = JavaCommandExecutor.executeCommand(command, constant.getExecShellTimeout());
//            if (result.failed()) {
//        throw new NodeMgrException(ConstantCode.ANSIBLE_SCP_FETCH_ERROR.attach(result.getExecuteOut()));
//    }
//            log.info("scp download fetch success, now use JavaCommander to mv file and delete duplicated directory");
//            if (!dst.endsWith("/")) {
//        dst = dst + "/";
//    }
//    String localFetchDir = dst + ip + src;
//            log.info("fetch localFetchDir:{}", localFetchDir);
//    // ex: src is remote's "/data/1.txt", dest is local "/backup", host is 127.0.0.1
//    // ex: after fetch, file would save in local "/backup/127.0.0.1/data", concat locall and hostIp(domain) and remote dir
//
//    // mv /backup/{ip}/data/1.txt /backup/1.txt
//    String mvFileCommand = String.format("mv %s/* %s", localFetchDir, dst);
//            log.info("fetch mvFileCommand:{}", mvFileCommand);
//    ExecuteResult mvResult = JavaCommandExecutor.executeCommand(mvFileCommand, constant.getExecShellTimeout());
//            if (mvResult.failed()) {
//        throw new NodeMgrException(ConstantCode.ANSIBLE_SCP_FETCH_ERROR.attach(result.getExecuteOut()));
//    }
//    // rm /backup/{ip}
//    String rmFileCommand = String.format("rm -r %s/", dst + ip);
//            log.info("fetch rmFileCommand:{}", rmFileCommand);
//    ExecuteResult rmResult = JavaCommandExecutor.executeCommand(rmFileCommand, constant.getExecShellTimeout());
//            if (rmResult.failed()) {
//        throw new NodeMgrException(ConstantCode.ANSIBLE_SCP_FETCH_ERROR.attach(result.getExecuteOut()));
//    }

    /**
     * host_check shell
     * @param ip
     * @return
     */
    public void execHostCheckShell(String ip, int nodeCount) {
        log.info("execHostCheckShell ip:{},nodeCount:{}", ip, nodeCount);
        String command = String.format("ansible %s -m script -a \"%s -C %d\"", ip, constant.getHostCheckShell(), nodeCount);
        ExecuteResult result = JavaCommandExecutor.executeCommand(command, constant.getExecShellTimeout());
        if (result.failed()) {
            if (result.getExitCode() == 3) {
                throw new NodeMgrException(ConstantCode.EXEC_HOST_CHECK_SCRIPT_ERROR_FOR_MEM.attach(result.getExecuteOut()));
            }
            if (result.getExitCode() == 4) {
                throw new NodeMgrException(ConstantCode.EXEC_HOST_CHECK_SCRIPT_ERROR_FOR_CPU.attach(result.getExecuteOut()));
            }
            throw new NodeMgrException(ConstantCode.EXEC_CHECK_SCRIPT_FAIL_FOR_PARAM.attach(result.getExecuteOut()));
        }
    }

    public void execDockerCheckShell(String ip) {
        log.info("execDockerCheckShell ip:{}", ip);
        String command = String.format("ansible %s -m script -a \"%s\"", ip, constant.getDockerCheckShell());
        ExecuteResult result = JavaCommandExecutor.executeCommand(command, constant.getExecShellTimeout());
        if (result.failed()) {
            if (result.getExitCode() == 5) {
                throw new NodeMgrException(ConstantCode.EXEC_DOCKER_CHECK_SCRIPT_ERROR.attach(result.getExecuteOut()));
            }
            throw new NodeMgrException(ConstantCode.EXEC_DOCKER_CHECK_SCRIPT_ERROR.attach(result.getExecuteOut()));
        }
    }

    /**
     * operate include host_init
     * 1. run host_init_shell
     * 2. sudo mkdir -p ${node_root} && sudo chown -R ${user} ${node_root} && sudo chgrp -R ${user} ${node_root}
     * param ip        Required.
     * param chainRoot chain root on host, default is /opt/fisco/{chain_name}.
     */
    public void execHostInit(String ip, String chainRoot) {
        this.execHostInitScript(ip);
        this.execCreateDir(ip, chainRoot);
    }

    public void execHostInitScript(String ip) {
        log.info("execHostInitScript ip:{}", ip);
        String command = String.format("ansible %s -m script -a \"%s\"", ip, constant.getHostInitShell());
        ExecuteResult result = JavaCommandExecutor.executeCommand(command, constant.getExecShellTimeout());
        if (result.failed()) {
            throw new NodeMgrException(ConstantCode.ANSIBLE_PING_NOT_REACH.attach(result.getExecuteOut()));
        }
    }

    /**
     * mkdir directory on target ip
     * @param ip
     * @param dir absolute path
     * @return
     */
    public ExecuteResult execCreateDir(String ip, String dir) {
        log.info("execCreateDir ip:{},dir:{}", ip, dir);
        // mkdir todo not use sudo to make dir, check access
//        String mkdirCommand = String.format("sudo mkdir -p %s", dir);
        String mkdirCommand = String.format("mkdir -p %s", dir);
        String command = String.format("ansible %s -m command -a \"%s\"", ip, mkdirCommand);
        return JavaCommandExecutor.executeCommand(command, constant.getExecShellTimeout());
    }

    /* docker operation: checkImageExists, pullImage run stop */

    /**
     * if not found, ansible exit code not same as script's exit code, use String to distinguish "not found"
     * @param ip
     * @param imageFullName
     * @return
     */
    public boolean checkImageExists(String ip, String imageFullName) {
        log.info("checkImageExists ip:{},imageFullName:{}", ip, imageFullName);

//        String dockerListImageCommand = String.format("sudo docker images -a %s | grep -v 'IMAGE ID'", imageFullName);
        String command = String.format("ansible %s -m script -a \"%s -i %s\"", ip, constant.getAnsibleImageCheckShell(), imageFullName);
        ExecuteResult result = JavaCommandExecutor.executeCommand(command, constant.getDockerPullTimeout());
        if (result.failed()) {
            // NOT FOUND IMAGE
            if (result.getExecuteOut().contains(NOT_FOUND_FLAG)) {
                return false;
            }
            // PARAM ERROR
            if (result.getExitCode() == 2) {
                throw new NodeMgrException(ConstantCode.ANSIBLE_CHECK_DOCKER_IMAGE_ERROR.attach(result.getExecuteOut()));
            }
        }
        // found
        return true;
    }

    public boolean checkContainerExists(String ip, String containerName) {
        log.info("checkContainerExists ip:{},containerName:{}", ip, containerName);

        // sudo docker ps | grep "${containerName}"
        String command = String.format("ansible %s -m script -a \"%s -c %s\"", ip, constant.getAnsibleContainerCheckShell(), containerName);
        ExecuteResult result = JavaCommandExecutor.executeCommand(command, constant.getDockerPullTimeout());
        if (result.failed()) {
            // NOT FOUND CONTAINER
            if (result.getExecuteOut().contains(NOT_FOUND_FLAG)) {
                return false;
            }
            // PARAM ERROR
            if (result.getExitCode() == 2) {
                throw new NodeMgrException(ConstantCode.ANSIBLE_CHECK_CONTAINER_ERROR.attach(result.getExecuteOut()));
            }
        }
        // found
        return true;
    }

    /**
     * pull and load image by cdn
     * @param ip
     * @param outputDir
     * @param webaseVersion
     * @return
     */
    public void execPullDockerCdnShell(String ip, String outputDir, String imageTag, String webaseVersion) {
        log.info("execPullDockerCdnShell ip:{},outputDir:{},imageTag:{},webaseVersion:{}", ip, outputDir, imageTag, webaseVersion);
        boolean imageExist = this.checkImageExists(ip, imageTag);
        if (imageExist) {
            log.info("image of {} already exist, jump over pull", imageTag);
            return;
        }
        String command = String.format("ansible %s -m script -a \"%s -d %s -v %s\"", ip, constant.getDockerPullCdnShell(), outputDir, webaseVersion);
        ExecuteResult result = JavaCommandExecutor.executeCommand(command, constant.getDockerPullTimeout());
        if (result.failed()) {
            throw new NodeMgrException(ConstantCode.ANSIBLE_PULL_DOCKER_CDN_ERROR.attach(result.getExecuteOut()));
        }
    }


    public ExecuteResult execDocker(String ip, String dockerCommand) {
        String command = String.format("ansible %s -m command -a \"%s\"", ip, dockerCommand);
        ExecuteResult result = JavaCommandExecutor.executeCommand(command, constant.getDockerRestartPeriodTime());
        return result;
    }

    /**
     * mv dir on remote
     */
    public void mvDirOnRemote(String ip, String src, String dst){
        if (StringUtils.isNoneBlank(ip, src, dst)) {
            // String rmCommand = String.format("sudo mv -fv %s %s", src, dst);
            // rm sudo
            String rmCommand = String.format("mv -fv %s %s", src, dst);
            log.info("Remove config on remote host:[{}], command:[{}].", ip, rmCommand);
            this.exec(ip, rmCommand);
        }
    }

}
