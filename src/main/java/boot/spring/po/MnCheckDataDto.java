package boot.spring.po;

/**
 * @author mingqiang
 * @date 2023/6/28 - 10:55
 * @desc
 */
public class MnCheckDataDto {

    private String vid;

    private String category;

    private String item_name;

    private String result;

    public String getVid() {
        return vid;
    }

    public void setVid(String vid) {
        this.vid = vid;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getItem_name() {
        return item_name;
    }

    public void setItem_name(String item_name) {
        this.item_name = item_name;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
