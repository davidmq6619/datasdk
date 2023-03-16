package boot.spring;

import boot.spring.constant.SdkConstant;
import cn.hutool.core.util.StrUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Properties;

import static boot.spring.constant.SdkConstant.USER_NAME;


@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
//@EnableScheduling
// 启动异步任务
@EnableAsync
public class Application {

    public static void main(String[] args) {
        String userName = System.getProperty("user_name");
        String desKey = System.getProperty("des_key");
        String shopNo = System.getProperty("shop_no");
        String url = System.getProperty("url");
        String jdbcDriver = System.getProperty("jdbc_driver");
        String jdbcUrl = System.getProperty("jdbc_url");
        String jdbcName = System.getProperty("jdbc_name");
        String jdbcPwd = System.getProperty("jdbc_pwd");
        String tableStr = System.getProperty("table_str");
        String packageId = System.getProperty("package_id");
        if (StrUtil.isNotBlank(userName)) {
            SdkConstant.USER_NAME = userName;
        }
        if (StrUtil.isNotBlank(desKey)) {
            SdkConstant.DES_KEY = desKey;
        }
        if (StrUtil.isNotBlank(shopNo)){
            SdkConstant.SHOP_NO = shopNo;
        }
        if (StrUtil.isNotBlank(url)) {
            SdkConstant.URL = url;
        }
        if (StrUtil.isNotBlank(jdbcDriver)) {
            SdkConstant.JDBC_DRIVER = jdbcDriver;
        }
        if (StrUtil.isNotBlank(jdbcUrl)) {
            SdkConstant.JDBC_URL = jdbcUrl;
        }
        if (StrUtil.isNotBlank(jdbcName)) {
            SdkConstant.JDBC_NAME = jdbcName;
        }
        if (StrUtil.isNotBlank(jdbcPwd)) {
            SdkConstant.JDBC_PWD = jdbcPwd;
        }
        if(StrUtil.isNotBlank(tableStr)){
            SdkConstant.TABLE_STR = tableStr;
        }
        if (StrUtil.isNotBlank(packageId)) {
            SdkConstant.PACKAGE_ID = packageId;
        }
        SpringApplication.run(Application.class, args);
    }
}
