package boot.spring.po;


import java.util.List;

public class LableInfo {

    Integer id;

    Integer pid;

    String lable;
    // value是本级节点与下级所有节点的加和
    Integer value = 0;

    List<LableInfo> children;

    public LableInfo(Integer id, Integer pid, String lable) {
        this.id = id;
        this.pid = pid;
        this.lable = lable;
    }

    public LableInfo(Integer id, Integer pid, String lable, Integer value) {
        this.id = id;
        this.pid = pid;
        this.lable = lable;
        this.value = value;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPid() {
        return pid;
    }

    public void setPid(Integer pid) {
        this.pid = pid;
    }

    public String getLable() {
        return lable;
    }

    public void setLable(String lable) {
        this.lable = lable;
    }

    public List<LableInfo> getChildren() {
        return children;
    }

    public void setChildren(List<LableInfo> children) {
        this.children = children;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "LableInfo{" +
                "id=" + id +
                ", pid=" + pid +
                ", lable='" + lable + '\'' +
                ", value=" + value +
                ", children=" + children +
                '}';
    }
}
