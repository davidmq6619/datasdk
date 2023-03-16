package boot.spring.po;

import java.io.Serializable;
import java.util.List;

/**
 * @author mingqiang
 * @date 2022/10/11 - 16:08
 * @desc
 */
public class MarketData implements Serializable {

    private CustomerDto customer;

    private List<CheckData> testData;

    private List<CheckData> checkData;
    private String orderNo;
    private String packageId;
    /**
     * 订单状态，0未支付，1支付中，2已支付，3退款中，4已退款
     */
    private Integer orderStatus;

    private String username;//机构账号
    private String nonceStr;//随机字符串
    private String signStr;//加密串

    public CustomerDto getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerDto customer) {
        this.customer = customer;
    }

    public List<CheckData> getTestData() {
        return testData;
    }

    public void setTestData(List<CheckData> testData) {
        this.testData = testData;
    }

    public List<CheckData> getCheckData() {
        return checkData;
    }

    public void setCheckData(List<CheckData> checkData) {
        this.checkData = checkData;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public Integer getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(Integer orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNonceStr() {
        return nonceStr;
    }

    public void setNonceStr(String nonceStr) {
        this.nonceStr = nonceStr;
    }

    public String getSignStr() {
        return signStr;
    }

    public void setSignStr(String signStr) {
        this.signStr = signStr;
    }
}
