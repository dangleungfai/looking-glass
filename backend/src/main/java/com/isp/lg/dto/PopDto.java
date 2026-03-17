package com.isp.lg.dto;

public class PopDto {
    private String popCode;
    private String popName;
    private String country;
    private String city;

    public PopDto() {}
    public PopDto(String popCode, String popName, String country, String city) {
        this.popCode = popCode;
        this.popName = popName;
        this.country = country;
        this.city = city;
    }
    public String getPopCode() { return popCode; }
    public void setPopCode(String popCode) { this.popCode = popCode; }
    public String getPopName() { return popName; }
    public void setPopName(String popName) { this.popName = popName; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
