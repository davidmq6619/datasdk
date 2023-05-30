package boot.spring.po;

import java.util.List;

/**
 * @author mingqiang
 * @date 2023/4/18 - 12:06
 * @desc
 */
public class StandardResult {

    private String id;

    private String customerName;

    private String customerRecordId;

    private List<MNCheckData> pacsCheckResult;

    private List<MNCheckData> standardDiagnosisResults;

    private List<MNCheckData> standardTestResult;

    public String getCustomerRecordId() {
        return customerRecordId;
    }

    public void setCustomerRecordId(String customerRecordId) {
        this.customerRecordId = customerRecordId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public List<MNCheckData> getPacsCheckResult() {
        return pacsCheckResult;
    }

    public void setPacsCheckResult(List<MNCheckData> pacsCheckResult) {
        this.pacsCheckResult = pacsCheckResult;
    }

    public List<MNCheckData> getStandardDiagnosisResults() {
        return standardDiagnosisResults;
    }

    public void setStandardDiagnosisResults(List<MNCheckData> standardDiagnosisResults) {
        this.standardDiagnosisResults = standardDiagnosisResults;
    }

    public List<MNCheckData> getStandardTestResult() {
        return standardTestResult;
    }

    public void setStandardTestResult(List<MNCheckData> standardTestResult) {
        this.standardTestResult = standardTestResult;
    }
}
