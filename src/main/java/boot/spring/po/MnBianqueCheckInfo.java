package boot.spring.po;

import java.io.Serializable;

/**
 * 美年离线数据实体
 */
public class MnBianqueCheckInfo implements Serializable {

    private String vid;

    private String item_ft;

    private String item_name;

    private String unit;

    private String result;

    private String normal_l;

    private String normal_h;

    private String big_category;

    public String getBig_category() {
        return big_category;
    }

    public void setBig_category(String big_category) {
        this.big_category = big_category;
    }

    public String getVid() {
        return vid;
    }

    public void setVid(String vid) {
        this.vid = vid;
    }

    public String getItem_ft() {
        return item_ft;
    }

    public void setItem_ft(String item_ft) {
        this.item_ft = item_ft;
    }

    public String getItem_name() {
        return item_name;
    }

    public void setItem_name(String item_name) {
        this.item_name = item_name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getNormal_l() {
        return normal_l;
    }

    public void setNormal_l(String normal_l) {
        this.normal_l = normal_l;
    }

    public String getNormal_h() {
        return normal_h;
    }

    public void setNormal_h(String normal_h) {
        this.normal_h = normal_h;
    }
}
