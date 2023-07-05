package boot.spring.po;

import java.io.Serializable;

/**
 * @author mingqiang
 * @date 2023/6/26 - 14:08
 * @desc
 */
public class CheckDto implements Serializable {

    private String category;

    private String itemName;

    private String normalL;

    private String result;

    private String unit;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getNormalL() {
        return normalL;
    }

    public void setNormalL(String normalL) {
        this.normalL = normalL;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
