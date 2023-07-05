package boot.spring.po;

import java.io.Serializable;

/**
 * @author mingqiang
 * @date 2023/6/26 - 14:06
 * @desc
 */
public class CheckDataVo extends CheckDataDto implements Serializable {


    private String diagnosis_data;

    public String getDiagnosis_data() {
        return diagnosis_data;
    }

    public void setDiagnosis_data(String diagnosis_data) {
        this.diagnosis_data = diagnosis_data;
    }
}
