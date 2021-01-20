package cn.lf.nacos.core;

import cn.lf.nacos.pojo.Instance;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;


@Data
public class Instances {

    private List<Instance> instanceList= new ArrayList<>();

}
