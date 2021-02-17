package cn.lf.nacos.misc;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpConnection;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ServerSynchronizer implements  Synchronizer  {

    @Override
    public void send(final String serverIp, Message msg) {
        Map<String,String> params = new HashMap<>(2);
        params.put("serverStatus",msg.getData());

        String url="http://"+serverIp+"/server/status";

        try{
            HttpClientUtils.asyncHttpGet(url, null, params, new AsyncCompletionHandler() {
                @Override
                public Object onCompleted(Response response) throws Exception {
                    if(response.getStatusCode()!= HttpURLConnection.HTTP_OK){
                        log.warn("发送server状态失败，要发送给的server"+serverIp);
                        return 1;
                    }
                    log.info("发送server状态成功，要发送给的server"+serverIp);
                    return 0;
                }
            });
        }catch(Exception e){
            log.warn("发送server状态失败，要发送给的server："+serverIp,e);
        }
    }

    @Override
    public Message get(String serverIp, String key) {
        return null;
    }

    @Override
    public boolean syncData(String serverIp, byte[] data) {
        log.info("开始同步数据......");

        Map<String,String> headers=new HashMap<>(128);
        headers.put("Accept-Encoding","gzip,deflate,sdch");
        headers.put("Connection","Keep-Alive");
        headers.put("Content-Encoding","gzip");

        try{
            HttpClientUtils.HttpResult result=HttpClientUtils.httpPutLarge("http://"+serverIp+"/data/sync",headers,data);
            if(HttpURLConnection.HTTP_OK==result.code){
                log.info("同步数据成功");
                return true;
            }
            if(HttpURLConnection.HTTP_NOT_MODIFIED==result.code){
                return true;
            }
            throw new IOException("请求失败 API: "+"http://"+serverIp+"/data/sync"+". code"+
                    result.code+"msg:"+result.content);
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }
}
