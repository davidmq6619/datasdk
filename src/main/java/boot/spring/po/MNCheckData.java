package boot.spring.po;

import java.io.Serializable;

/**
 * @author mingqiang
 * @date 2023/4/18 - 11:24
 * @desc
 */
public class MNCheckData implements Serializable {

    private String testItemFt;

    private String initResult;

    private String itemName;

    private String itemResults;

    private String itemOkResults;

    private String itemUnit;

    private String result;

    private String normalL;

    private String normalH;

    private String bigCategory;

    private String smallCategory;

    private String itemNameComm;

    private String resultsDiscrete;

    private String unitComm;

    private String itemNo;

    private String itemId;

    private String remark;

    private String cleanStatus;

    private String normalLOK;

    private String normalHOK;

    public String getInitResult() {
        return initResult;
    }

    public void setInitResult(String initResult) {
        this.initResult = initResult;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemResults() {
        return itemResults;
    }

    public void setItemResults(String itemResults) {
        this.itemResults = itemResults;
    }

    public String getItemOkResults() {
        return itemOkResults;
    }

    public void setItemOkResults(String itemOkResults) {
        this.itemOkResults = itemOkResults;
    }

    public String getItemUnit() {
        return itemUnit;
    }

    public void setItemUnit(String itemUnit) {
        this.itemUnit = itemUnit;
    }

    public String getTestItemFt() {
        return testItemFt;
    }

    public void setTestItemFt(String testItemFt) {
        this.testItemFt = testItemFt;
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

    public String getBigCategory() {
        return bigCategory;
    }

    public void setBigCategory(String bigCategory) {
        this.bigCategory = bigCategory;
    }

    public String getSmallCategory() {
        return smallCategory;
    }

    public void setSmallCategory(String smallCategory) {
        this.smallCategory = smallCategory;
    }

    public String getItemNameComm() {
        return itemNameComm;
    }

    public void setItemNameComm(String itemNameComm) {
        this.itemNameComm = itemNameComm;
    }

    public String getResultsDiscrete() {
        return resultsDiscrete;
    }

    public void setResultsDiscrete(String resultsDiscrete) {
        this.resultsDiscrete = resultsDiscrete;
    }

    public String getUnitComm() {
        return unitComm;
    }

    public void setUnitComm(String unitComm) {
        this.unitComm = unitComm;
    }

    public String getItemNo() {
        return itemNo;
    }

    public void setItemNo(String itemNo) {
        this.itemNo = itemNo;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCleanStatus() {
        return cleanStatus;
    }

    public void setCleanStatus(String cleanStatus) {
        this.cleanStatus = cleanStatus;
    }

    public String getNormalLOK() {
        return normalLOK;
    }

    public void setNormalLOK(String normalLOK) {
        this.normalLOK = normalLOK;
    }

    public String getNormalHOK() {
        return normalHOK;
    }

    public void setNormalHOK(String normalHOK) {
        this.normalHOK = normalHOK;
    }
}
