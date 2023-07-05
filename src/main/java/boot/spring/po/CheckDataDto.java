package boot.spring.po;

import java.io.Serializable;

/**
 * @author mingqiang
 * @date 2023/6/26 - 14:06
 * @desc
 */
public class CheckDataDto implements Serializable {
    private String vid;

    private String check_data;

    public String getVid() {
        return vid;
    }

    public void setVid(String vid) {
        this.vid = vid;
    }

    public String getCheck_data() {
        return check_data;
    }

    public void setCheck_data(String check_data) {
        this.check_data = check_data;
    }
}
