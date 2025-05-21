package com.kmd.bussing.ui.home;

public class CardLists {
    private String BusNumber;
    private String FromLocation;
    private String ToWhereLocation;
    private int BusCompanyImage;
    private String CurrentLocation;
    private String DepartureTime;
    private String DriverName;
    private String ConductorName, Status;
    private int Capacity, Price;


    public CardLists(String busNumber, String fromLocation, String toWhereLocation, int busCompanyImage, String currentLocation, String departureTime,
                     String driver, String conductor, int capacity, int price, String status) {
        this.BusNumber = busNumber;
        this.FromLocation = fromLocation;
        this.ToWhereLocation = toWhereLocation;
        this.BusCompanyImage = busCompanyImage;
        this.CurrentLocation = currentLocation;
        this.DepartureTime = departureTime;
        this.DriverName = driver;
        this.ConductorName = conductor;
        this.Capacity = capacity;
        this.Price = price;
        this.Status = status;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }

    public int getPrice() {return Price;}

    public void setPrice(int price) {Price = price;}

    public String getBusNumber() {
        return BusNumber;
    }

    public void setBusNumber(String busNumber) {
        BusNumber = busNumber;
    }

    public String getFromLocation() {
        return FromLocation;
    }

    public void setFromLocation(String fromLocation) {
        FromLocation = fromLocation;
    }

    public String getToWhereLocation() {
        return ToWhereLocation;
    }

    public void setToWhereLocation(String toWhereLocation) {
        ToWhereLocation = toWhereLocation;
    }

    public int getBusCompanyImage() {
        return BusCompanyImage;
    }

    public void setBusCompanyImage(int busCompanyImage) {
        BusCompanyImage = busCompanyImage;
    }

    public String getCurrentLocation() {
        return CurrentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        CurrentLocation = currentLocation;
    }

    public String getDepartureTime() {
        return DepartureTime;
    }

    public void setDepartureTime(String departureTime) {
        DepartureTime = departureTime;
    }

    public String getDriverName() {
        return DriverName;
    }

    public void setDriverName(String driverName) {
        DriverName = driverName;
    }

    public String getConductorName() {
        return ConductorName;
    }

    public void setConductorName(String conductorName) {
        ConductorName = conductorName;
    }

    public void setCapacity(int capacity) {
        Capacity = capacity;
    }

    public int getCapacity() {
        return Capacity;
    }
}