package com.ai.paas.cpaas.rm.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.springframework.batch.core.scope.context.ChunkContext;

import com.ai.paas.cpaas.rm.vo.OpenResourceParamVo;
import com.ai.paas.cpaas.rm.vo.TransResultVo;
import com.ai.paas.ipaas.PaasException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class TaskUtil {

  public static String filepath = "/root/test";
  private static final AtomicInteger counter = new AtomicInteger();

  public static int nextValue() {
    return counter.getAndIncrement();
  }

  public static OpenResourceParamVo createOpenParam(ChunkContext chunkContext) {
    String openParameter = (String) chunkContext.getStepContext().getJobParameters().get("openParameter");
    Gson gson = new Gson();
    OpenResourceParamVo openParam = gson.fromJson(openParameter, OpenResourceParamVo.class);
    return openParam;
  }

  public static StringBuffer createBashFile() {
    StringBuffer shellContext = new StringBuffer();
    shellContext.append("#!/bin/bash\n");
    return shellContext;
  }

  // �������ɴ���
  public static String generatePasswd() {
    return new SessionIdentifierGenerator().nextSessionId();
  }

  public static void executeCommand(StringEntity paramEntity,String type) throws ClientProtocolException, IOException, PaasException {
    HttpClient httpClient = HttpClients.createDefault();
    String url=new String();
    if(type.equals("upload"))
    {
    	url="http://10.1.241.127:8880/agent-web-api/simpFile/upload";
    }else{
    	url="http://10.1.241.127:8880/agent-web-api/simpCommand/exec";
    }
    HttpPost httpPost = new HttpPost(url);
    httpPost.setEntity(paramEntity);
    HttpResponse response = httpClient.execute(httpPost);
    HttpEntity entity = response.getEntity();
    String result = new String();
    if (entity != null) {
      InputStream instream = entity.getContent();
      InputStreamReader inputStream = new InputStreamReader(instream, "UTF-8");
      try {
        BufferedReader br = new BufferedReader(inputStream);
        result = br.readLine();
      } finally {
        instream.close();
      }
    }
    Gson gson = new Gson();
    TransResultVo resultVo = gson.fromJson(result, TransResultVo.class);
    if (resultVo.getCode().equals(ExceptionCodeConstants.TransServiceCode.ERROR_CODE)) {
      throw new PaasException(ExceptionCodeConstants.DubboServiceCode.SYSTEM_ERROR_CODE, resultVo.getMsg());
    }

  }

  public static StringEntity genFileParam(String content, String filename, String path) throws UnsupportedEncodingException {
    JsonObject object = new JsonObject();
    object.addProperty("aid", "dev");
    object.addProperty("content", content);
    object.addProperty("fileName", filename);;
    object.addProperty("path", path);
    StringEntity entity = new StringEntity(object.toString(), "application/json", "UTF-8");
    return entity;
  }

  public static StringEntity genCommandParam(String command) throws UnsupportedEncodingException {
    JsonObject object = new JsonObject();
    object.addProperty("aid", "dev");
    object.addProperty("command", command);
    StringEntity entity = new StringEntity(object.toString(), "application/json", "UTF-8");
    return entity;
  }

  public static void addExPermission(String fileName) throws ClientProtocolException, IOException {
    StringBuffer command = new StringBuffer();
    command.append("chmod u+x ");
    command.append(fileName);
    TaskUtil.genCommandParam(command.toString());
  }

  public static void executeFile(String filename, String content) throws ClientProtocolException, IOException, PaasException {

    // ����ִ���ļ�
    StringEntity fileEntity = TaskUtil.genFileParam(content, filename, TaskUtil.filepath);
    TaskUtil.executeCommand(fileEntity,"upload");


    // �����ļ�Ȩ��
    StringEntity addExEntity = TaskUtil.genCommandParam("chmod u+x " + TaskUtil.filepath + "/configAnsibleHosts");
    TaskUtil.executeCommand(addExEntity,"command");

    // ִ���ļ�
    StringEntity exFileEntity = TaskUtil.genCommandParam("bash " + TaskUtil.filepath + "/configAnsibleHosts");
    TaskUtil.executeCommand(exFileEntity,"command");
  }

  public static String genMasterName(int i) {
    return "mesos-master" + i;
  }

  public static String genSlaveName(int i) {
    return "mesos-slave" + i;
  }

  public static void getFile(String path) throws IOException {
    // InputStream in = TaskUtil.class.getResourceAsStream("/batch/river.yml");
    InputStream in = TaskUtil.class.getResourceAsStream(path);
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    try {
      StringBuilder sb = new StringBuilder();
      String line = br.readLine();

      while (line != null) {
        sb.append(line);
        sb.append(System.lineSeparator());
        line = br.readLine();
      }
      String everything = sb.toString();
      System.out.println(everything);
    } finally {
      br.close();
    }
  }
}