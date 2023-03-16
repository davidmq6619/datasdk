package boot.spring.po;

import java.io.Serializable;

/**
 * @author mingqiang
 * @date 2022/10/11 - 16:10
 * @desc
 */
public class CheckData implements Serializable {

    private String itemNo;
    private String itemName;
    private String result;
    private String category;
    private String unit;
    private String normalL;
    private String normalH;
    /**
     * 2.5.2 科室
     */
    private String department;

    public String getItemNo() {
        return itemNo;
    }

    public void setItemNo(String itemNo) {
        this.itemNo = itemNo;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getNormalL() {
        return normalL;
    }

    public void setNormalL(String normalL) {
        this.normalL = normalL;
    }

    public String getNormalH() {
        return normalH;
    }

    public void setNormalH(String normalH) {
        this.normalH = normalH;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
