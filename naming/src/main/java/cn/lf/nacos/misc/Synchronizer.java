package cn.lf.nacos.misc;



public interface Synchronizer {

    /**
     * 向服务器发送消息
     */
    void send(String serverIp,Message msg);

    /**
     * 使用消息密钥从服务器获取消息
     */
    Message get(String serverIp,String key);

    /**
     * 同步实例数据
     */
    boolean syncData(String serverIp,byte[] data);
}
