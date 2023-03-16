package boot.spring.po;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public class Item {
    Long id;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    LocalDateTime gmtCreate;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    LocalDateTime gmtUpdate;

    Integer number;

    Integer version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(LocalDateTime gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public LocalDateTime getGmtUpdate() {
        return gmtUpdate;
    }

    public void setGmtUpdate(LocalDateTime gmtUpdate) {
        this.gmtUpdate = gmtUpdate;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }


}
