package boot.spring.po;

import java.io.Serializable;
import java.util.Date;

/**
 * @author mingqiang
 * @date 2022/10/11 - 16:11
 * @desc
 */
public class CustomerDto implements Serializable {

    private String vid;

    private String shopNo;
    private String mobile;
    private Date checkDate;

    private String custName;
    private String custSex;
    private Date custCsrq;
    private String custZy;
    private String custHy;
    private String custSfzh;
    private String agentMobile;
    private String companyId;//企业ID（团检客户）
    private String companyName;//企业名称（团检客户）
    private String ext1;//备用字段
    private String ext2;//备用字段
    private String ext3;//备用字段

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getVid() {
        return vid;
    }

    public void setVid(String vid) {
        this.vid = vid;
    }

    public String getShopNo() {
        return shopNo;
    }

    public void setShopNo(String shopNo) {
        this.shopNo = shopNo;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Date getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(Date checkDate) {
        this.checkDate = checkDate;
    }

    public String getCustName() {
        return custName;
    }

    public void setCustName(String custName) {
        this.custName = custName;
    }

    public String getCustSex() {
        return custSex;
    }

    public void setCustSex(String custSex) {
        this.custSex = custSex;
    }

    public Date getCustCsrq() {
        return custCsrq;
    }

    public void setCustCsrq(Date custCsrq) {
        this.custCsrq = custCsrq;
    }

    public String getCustZy() {
        return custZy;
    }

    public void setCustZy(String custZy) {
        this.custZy = custZy;
    }

    public String getCustHy() {
        return custHy;
    }

    public void setCustHy(String custHy) {
        this.custHy = custHy;
    }

    public String getCustSfzh() {
        return custSfzh;
    }

    public void setCustSfzh(String custSfzh) {
        this.custSfzh = custSfzh;
    }

    public String getAgentMobile() {
        return agentMobile;
    }

    public void setAgentMobile(String agentMobile) {
        this.agentMobile = agentMobile;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getExt1() {
        return ext1;
    }

    public void setExt1(String ext1) {
        this.ext1 = ext1;
    }

    public String getExt2() {
        return ext2;
    }

    public void setExt2(String ext2) {
        this.ext2 = ext2;
    }

    public String getExt3() {
        return ext3;
    }

    public void setExt3(String ext3) {
        this.ext3 = ext3;
    }
}
