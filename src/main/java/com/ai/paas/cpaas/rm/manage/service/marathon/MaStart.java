package com.ai.paas.cpaas.rm.manage.service.marathon;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import com.ai.paas.cpaas.rm.util.AnsibleCommand;
import com.ai.paas.cpaas.rm.util.TaskUtil;
import com.ai.paas.cpaas.rm.vo.MesosInstance;
import com.ai.paas.cpaas.rm.vo.OpenResourceParamVo;

public class MaStart implements Tasklet {

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
    OpenResourceParamVo openParam = TaskUtil.createOpenParam(chunkContext);
    StringBuffer shellContext = TaskUtil.createBashFile();
    List<MesosInstance> mesosMaster = openParam.getMesosMaster();
    MesosInstance mesosInstance = openParam.getMesosMaster().get(0);
    String passwd = mesosInstance.getPasswd();
    for (int i = 0; i < mesosMaster.size(); i++) {
      MesosInstance masterInstance = mesosMaster.get(i);
      List<String> startVars = new ArrayList<String>();
      startVars.add("ansible_ssh_pass=" + passwd);
      startVars.add("ansible_become_pass=" + passwd);
      startVars.add("hosts=mesos-master" + (i + 1));
      startVars.add("hostname=" + masterInstance.getIp());
      AnsibleCommand masterStart = new AnsibleCommand(TaskUtil.filepath + "/hostname.yml", "rcmarathon", startVars);
      shellContext.append(masterStart.toString());
      shellContext.append(System.lineSeparator());
    }
    return RepeatStatus.FINISHED;
  }

}