package com.peppa.selfEditor;


public class Address {
	String province;
	String city;
	String area;

	public Address(String province, String city, String area) {
		this.province = province;
		this.city = city;
		this.area = area;
	}

	public Address() {
	}

	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getArea() {
		return area;
	}

	public void setArea(String area) {
		this.area = area;
	}

	@Override
	public String toString() {
		return "Address{" +
				"province='" + province + '\'' +
				", city='" + city + '\'' +
				", area='" + area + '\'' +
				'}';
	}
}
