package cn.lf.nacos.consistency;

/**
 * 标记实例的动作类
 */
public enum ApplyAction {

    //service数据更改(添加实例)
    CHANGE,

    //删除实例
    DELETE
}
