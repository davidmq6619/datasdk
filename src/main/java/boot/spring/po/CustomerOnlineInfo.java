package boot.spring.po;

import java.util.Date;

public class CustomerOnlineInfo {

    private String vid;

    private String cust_name;

    private String cust_sex;

    private Date cust_csrq;

    private String cust_sfzh;

    private Date check_date;

    private String shop_no;

    private String mobile;

    private String test_data;

    private String check_data;

    public String getVid() {
        return vid;
    }

    public void setVid(String vid) {
        this.vid = vid;
    }

    public String getCust_name() {
        return cust_name;
    }

    public void setCust_name(String cust_name) {
        this.cust_name = cust_name;
    }

    public String getCust_sex() {
        return cust_sex;
    }

    public void setCust_sex(String cust_sex) {
        this.cust_sex = cust_sex;
    }

    public Date getCust_csrq() {
        return cust_csrq;
    }

    public void setCust_csrq(Date cust_csrq) {
        this.cust_csrq = cust_csrq;
    }

    public String getCust_sfzh() {
        return cust_sfzh;
    }

    public void setCust_sfzh(String cust_sfzh) {
        this.cust_sfzh = cust_sfzh;
    }

    public Date getCheck_date() {
        return check_date;
    }

    public void setCheck_date(Date check_date) {
        this.check_date = check_date;
    }

    public String getShop_no() {
        return shop_no;
    }

    public void setShop_no(String shop_no) {
        this.shop_no = shop_no;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getTest_data() {
        return test_data;
    }

    public void setTest_data(String test_data) {
        this.test_data = test_data;
    }

    public String getCheck_data() {
        return check_data;
    }

    public void setCheck_data(String check_data) {
        this.check_data = check_data;
    }
}