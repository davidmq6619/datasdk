package boot.spring.po;

import java.io.Serializable;
import java.util.List;

/**
 * @author mingqiang
 * @date 2023/4/18 - 11:31
 * @desc
 */
public class StandardCheckData implements Serializable {
    private String code;
    private String message;
    private StandardResult result;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public StandardResult getResult() {
        return result;
    }

    public void setResult(StandardResult result) {
        this.result = result;
    }
}
