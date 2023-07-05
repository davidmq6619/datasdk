package boot.spring.po;

import java.io.Serializable;

/**
 * @author mingqiang
 * @date 2023/6/26 - 14:08
 * @desc
 */
public class CheckVo extends CheckDto implements Serializable {

    private String itemResults;

    private String traceData;

    private String traceDesc;

    public String getItemResults() {
        return itemResults;
    }

    public void setItemResults(String itemResults) {
        this.itemResults = itemResults;
    }

    public String getTraceDesc() {
        return traceDesc;
    }

    public void setTraceDesc(String traceDesc) {
        this.traceDesc = traceDesc;
    }

    public String getTraceData() {
        return traceData;
    }

    public void setTraceData(String traceData) {
        this.traceData = traceData;
    }
}
