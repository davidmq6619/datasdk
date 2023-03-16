package boot.spring.po;

import java.util.Date;

/**
 * @author mingqiang
 * @date 2022/9/30 - 15:17
 * @desc
 */

public class MarketDataRecord {


    private Long id;

    private String market_data;

    private Date created_at;

    private Date updated_at;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMarket_data() {
        return market_data;
    }

    public void setMarket_data(String market_data) {
        this.market_data = market_data;
    }

    public Date getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Date created_at) {
        this.created_at = created_at;
    }

    public Date getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(Date updated_at) {
        this.updated_at = updated_at;
    }
}
