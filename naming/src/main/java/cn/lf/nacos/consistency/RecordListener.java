package cn.lf.nacos.consistency;

import cn.lf.nacos.core.Instances;

public interface RecordListener {

    void onChange(String key, Instances value);

    void onDelete(String key);

}
