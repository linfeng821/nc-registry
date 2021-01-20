package cn.lf.nacos.pojo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class ServiceInfo {

    //服务的名字
   private String name;


   //健康集群列表
   private String clusters;

   //该服务下的实例列表
   private List<Instance> instances = new ArrayList<>();

}
