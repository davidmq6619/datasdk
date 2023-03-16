package boot.spring.po;

import java.io.Serializable;

/**
 * @author mingqiang
 * @date 2022/10/13 - 13:43
 * @desc
 */
public class DataParam implements Serializable {

    private String ids;

    private String mark;

    private Integer limit;

    public String getIds() {
        return ids;
    }

    public void setIds(String ids) {
        this.ids = ids;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
