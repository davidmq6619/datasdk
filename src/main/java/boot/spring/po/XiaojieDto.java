package boot.spring.po;

import java.io.Serializable;

/**
 * @author mingqiang
 * @date 2023/5/30 - 15:28
 * @desc
 */
public class XiaojieDto implements Serializable {
    private Boolean autoMatch;

    private String conclusionCode;

    private String conclusionName;

    private String majorPositive;

    private String positiveLevel;

    public Boolean getAutoMatch() {
        return autoMatch;
    }

    public void setAutoMatch(Boolean autoMatch) {
        this.autoMatch = autoMatch;
    }

    public String getConclusionCode() {
        return conclusionCode;
    }

    public void setConclusionCode(String conclusionCode) {
        this.conclusionCode = conclusionCode;
    }

    public String getConclusionName() {
        return conclusionName;
    }

    public void setConclusionName(String conclusionName) {
        this.conclusionName = conclusionName;
    }

    public String getMajorPositive() {
        return majorPositive;
    }

    public void setMajorPositive(String majorPositive) {
        this.majorPositive = majorPositive;
    }

    public String getPositiveLevel() {
        return positiveLevel;
    }

    public void setPositiveLevel(String positiveLevel) {
        this.positiveLevel = positiveLevel;
    }
}
