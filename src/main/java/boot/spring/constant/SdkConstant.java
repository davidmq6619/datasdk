package boot.spring.constant;

/**
 * @author mingqiang
 * @date 2022/10/11 - 18:01
 * @desc
 */
public class SdkConstant {
    //测试环境
    //public static String DES_KEY = "10bc8876d7bf7d9ebdde62b0cf24583e";
    //美年测试
    public static String DES_KEY ="29251ea7baaa482ffbbc61b05b83b9b9";
    public static String NULL_STR = "";
    public static String SPLIT_OTHER = "&";
    //演示环境
    //public static String USER_NAME = "oom";
    //美年测试环境
    public static String USER_NAME ="gdsrmyytj";
    //public static String URL="http://192.168.16.54:30190/v1/sdk/order/group/push";//测试二
    public static String URL="http://192.168.16.54:31001/v1/sdk/order/group/push";//美年测试
    //public static String URL="https://service.health-100.cn/v1/sdk/order/create";
    //public static String URL="http://192.168.16.186:32001/v1/sdk/order/group/push";//演示环境
    public static String PACKAGE_ID = "21152S";
    public static String SHOP_NO = "14825";
    public static String JDBC_DRIVER = "org.postgresql.Driver";
    //public static String JDBC_URL = "jdbc:postgresql://pgm-uf60r06h2h8cv295.pg.rds.aliyuncs.com:5432/etl1";
    //public static String JDBC_NAME = "whe_lkw";
    //public static String JDBC_PWD="linkaiwei#zhongkang004337ZKGroup";
    //医学验证
    //public static String TABLE_STR="yixueyanzheng";
    //public static String JDBC_URL = "jdbc:postgresql://192.168.16.57:54321/aimdt_etl_model";
    //public static String JDBC_NAME = "whe";
    //public static String JDBC_PWD="whe";
    //生产环境
    /*public static String DES_KEY = "46510e1a90ea77aa95a5854423ee6975";
    public static String NULL_STR = "";
    public static String SPLIT_OTHER = "&";
    public static String USER_NAME = "health100";
    public static String URL="https://service.health-100.cn/v1/sdk/order/create";
    public static String PACKAGE_ID = "21152S";
    public static String SHOP_NO = "17";
    public static String JDBC_DRIVER = "org.postgresql.Driver";
    public static String JDBC_URL = "jdbc:postgresql://pgm-uf60r06h2h8cv295.pg.rds.aliyuncs.com:5432/meinian_middle";
    *//*public static String JDBC_NAME = "whe_lkw";
    public static String JDBC_PWD="linkaiwei#zhongkang004337ZKGroup";*//*
    public static String TABLE_STR="health100";
    public static String JDBC_NAME = "meinian_middle";
    public static String JDBC_PWD="@meimidle#@zkang@2022##";*/
    //省人民医院数据推送用于数据拉取
    public static String TABLE_STR="gdsrmyy";
    public static String JDBC_URL = "jdbc:postgresql://192.168.16.57:54321/etl_hnygxkyy";
    public static String JDBC_NAME = "whe";
    public static String JDBC_PWD="whe";
}
