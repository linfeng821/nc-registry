package cn.lf.nacos.misc;

public class ServerSynchronizer implements  Synchronizer  {

    @Override
    public void send(String serverIp, Message msg) {

    }

    @Override
    public Message get(String serverIp, String key) {
        return null;
    }

    @Override
    public boolean syncData(String serverIp, byte[] data) {
        return false;
    }
}
