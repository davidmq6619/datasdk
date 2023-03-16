package boot.spring.po;

import cn.afterturn.easypoi.excel.annotation.Excel;

public class City {
	@Excel(name = "编号", width=10)
	private Integer city_id;
	
	@Excel(name = "城市")
	private String city;
	
	@Excel(name = "最后更新日期", width=20)
	private String last_update;
	
	private Country country;
	
	public Integer getCity_id() {
		return city_id;
	}
	public void setCity_id(Integer city_id) {
		this.city_id = city_id;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
	}
	public Country getCountry() {
		return country;
	}
	public void setCountry(Country country) {
		this.country = country;
	}
	public String getLast_update() {
		return last_update;
	}
	public void setLast_update(String last_update) {
		this.last_update = last_update;
	}
	
	
}
