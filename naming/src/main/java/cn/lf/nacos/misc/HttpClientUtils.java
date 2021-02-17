package cn.lf.nacos.misc;

import cn.lf.nacos.common.HttpMethod;
import cn.lf.nacos.utils.SystemUtils;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.springframework.util.StringUtils;
import sun.nio.ch.IOUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpClientUtils {

    private static final int TIME_OUT_MILLS= 10000;
    private static final int CON_TIME_OUT_MILLS=5000;


    private static AsyncHttpClient asyncHttpClient;

    private static CloseableHttpClient postClient;

    static {
        AsyncHttpClientConfig.Builder builder=new AsyncHttpClientConfig.Builder();
        builder.setMaximumConnectionsTotal(-1);
        builder.setMaximumConnectionsPerHost(128);
        builder.setAllowPoolingConnection(true);
        builder.setFollowRedirects(false);
        builder.setIdleConnectionTimeoutInMs(TIME_OUT_MILLS);
        builder.setConnectionTimeoutInMs(CON_TIME_OUT_MILLS);
        builder.setCompressionEnabled(true);
        builder.setIOThreadMultiplier(1);
        builder.setMaxRequestRetry(0);

        asyncHttpClient=new AsyncHttpClient(builder.build());
        HttpClientBuilder builder2= HttpClients.custom();
        builder2.setConnectionTimeToLive(CON_TIME_OUT_MILLS, TimeUnit.MILLISECONDS);
        builder2.setMaxConnPerRoute(-1);
        builder2.setMaxConnTotal(-1);
        builder2.disableAutomaticRetries();

        postClient= builder2.build();
    }

    public static void asyncHttpGet(String url,List<String> headers,Map<String,String> paramValues,AsyncCompletionHandler handler)throws Exception{
        asyncHttpRequest(url,headers,paramValues,handler,HttpMethod.GET);
    }

    public static void asyncHttpPost(String url,List<String> headers,Map<String,String> paramValues,AsyncCompletionHandler handler)throws Exception{
        asyncHttpRequest(url,headers,paramValues,handler,HttpMethod.POST);
    }

    public static void asyncHttpDelete(String url,List<String> headers,Map<String,String>paramValues,AsyncCompletionHandler handler)throws Exception{
        asyncHttpRequest(url,headers,paramValues,handler,HttpMethod.DELETE);
    }

    /**
     * 异步发送请求
     * @param url
     * @param headers
     * @param paramValues
     * @param handler
     * @param method
     * @throws Exception
     */
    public static void asyncHttpRequest(String url, List<String> headers, Map<String,String> paramValues, AsyncCompletionHandler handler,String method)throws Exception{

        if(!MapUtils.isEmpty(paramValues)){
            String encodedContent= encodingParams(paramValues,"UTF-8");
            url+=(encodedContent==null)?"":("?"+encodedContent);
        }
        AsyncHttpClient.BoundRequestBuilder builder;

        switch(method){
            case HttpMethod.GET:
                builder=asyncHttpClient.prepareGet(url);
                break;
            case HttpMethod.POST:
                builder=asyncHttpClient.preparePost(url);
                break;
            case HttpMethod.DELETE:
                builder=asyncHttpClient.prepareDelete(url);
                break;
            case HttpMethod.PUT:
                builder=asyncHttpClient.preparePut(url);
                break;
            default:
                throw new RuntimeException("not supported method"+method);
        }

        if(!CollectionUtils.isEmpty(headers)){
            for(String header:headers){
                builder.setHeader(header.split("=")[0],header.split("=")[1]);
            }
        }

        builder.setHeader("Accept-Charset","UTF-8");

        if(handler!=null){
            builder.execute(handler);
        }else{
            builder.execute();
        }
    }

    public static String encodingParams(Map<String,String> params,String encoding)throws UnsupportedEncodingException {

        StringBuilder sb=new StringBuilder();
        if(params==null|| params.isEmpty()){
            return null;
        }

        params.put("encoding",encoding);
        params.put("nofix","1");

        for(Map.Entry<String,String> entry:params.entrySet()){
            if(StringUtils.isEmpty(entry.getValue())){
                continue;
            }
            sb.append(entry.getKey()).append("=");
            sb.append(URLEncoder.encode(entry.getValue(),encoding));
            sb.append("&");
        }

        return sb.toString();
    }


    public static HttpResult httpPutLarge(String url,Map<String,String> headers,byte[]content){
        try{
            HttpClientBuilder builder=HttpClients.custom();
            builder.setConnectionTimeToLive(500,TimeUnit.MILLISECONDS);

            CloseableHttpClient httpClient=builder.build();
            HttpPut httpPut=new HttpPut(url);

            for(Map.Entry<String,String> entry:headers.entrySet()){
                httpPut.setHeader(entry.getKey(),entry.getValue());
            }
            httpPut.setEntity(new StringEntity(new String(content,"UTF-8"), ContentType.create("application/json","UTF-8")));

            HttpResponse response=httpClient.execute(httpPut);
            HttpEntity entity=response.getEntity();
            HeaderElement [] headerElements=entity.getContentType().getElements();
            String charset=headerElements[0].getParameterByName("charset").getValue();

            return new HttpResult(response.getStatusLine().getStatusCode(),
                    IOUtils.toString(entity.getContent(), charset), Collections.<String,String>emptyMap());
        }catch(Exception e){
            return new HttpResult(500,e.toString(),Collections.<String,String>emptyMap());
        }
    }

    /**
     * 返回结果的封装
     */
    public static class HttpResult{
        final public int code;
        final public String content;
        final private Map<String,String> respHeaders;

        public HttpResult(int code,String content,Map<String,String> respHeaders){
            this.code=code;
            this.content=content;
            this.respHeaders=respHeaders;
        }

        public String getHeader(String name){
            return respHeaders.get(name);
        }
    }
}
