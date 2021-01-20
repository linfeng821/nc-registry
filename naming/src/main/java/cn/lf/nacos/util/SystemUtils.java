package cn.lf.nacos.util;


import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;


import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemUtils {

    public static void main(String[]args) throws IOException{
        System.out.println(readClusterConf());
        System.out.println(mappingMap);
    }

    private static BufferedReader bufferedReader;

    //mappingMap包含了serverIp和nettyServerIP的映射关系
    public static Map<String,String> mappingMap=new HashMap<>();


    //返回serversList包含server的服务器集群IP端口列表
    public static List<String> readClusterConf() throws IOException {
        List<String> serversList=new ArrayList<>();
        List<String> lines=new ArrayList<>();
        try {
            //读取resources目录下的集群文件
            Resource resource=new ClassPathResource("conf/cluster.conf");
            File file=resource.getFile();
            Reader reader = new InputStreamReader(new FileInputStream(file));
            bufferedReader=new BufferedReader(reader);
            String line=null;
            while((line=bufferedReader.readLine())!=null){
                lines.add(line.trim());
            }
            String comment="#";
            for(String line1:lines){
                String instance=line1.trim();
                //略过整行注释
                if(instance.startsWith(comment)){
                    continue;
                }
                //略过注释部分
                if(instance.contains(comment)){
                    instance=instance.substring(0,instance.indexOf(comment));
                    instance =instance.trim();
                }
                //配置，前为serverIp，后为nettyServerIp
                String serverIp=instance.split(",")[0];
                String nettyServerIp=instance.split(",")[1];
                //以localhost开头获取本地主机地址拼接端口号
                if(serverIp.startsWith("localhost")){
                    serverIp=InetAddress.getLocalHost().getHostAddress().toString()+serverIp.substring("localhost".length());
                }
                if(nettyServerIp.startsWith("localhost")) {
                    nettyServerIp = InetAddress.getLocalHost().getHostAddress().toString() + nettyServerIp.substring("localhost".length());
                }
                serversList.add(serverIp);
                mappingMap.put(serverIp,nettyServerIp);
            }
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }finally{
            bufferedReader.close();
        }

        return serversList;
   }
}
