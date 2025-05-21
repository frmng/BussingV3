package com.kmd.bussing.ui.status;

public class StatusLists {
    private String BusNo;
    private String BusFrom;
    private String BusTo;
    private String BusDP;
    private String BusPrice;
    private String Status;

    public StatusLists(String busNo, String busFrom, String busTo, String busDP, String busPrice, String status) {
        BusNo = busNo;
        BusFrom = busFrom;
        BusTo = busTo;
        BusDP = busDP;
        BusPrice = busPrice;
        Status = status;
    }

    public StatusLists() {
    }

    public String getBusNo() {
        return BusNo;
    }

    public void setBusNo(String busNo) {
        BusNo = busNo;
    }

    public String getBusFrom() {
        return BusFrom;
    }

    public void setBusFrom(String busFrom) {
        BusFrom = busFrom;
    }

    public String getBusTo() {
        return BusTo;
    }

    public void setBusTo(String busTo) {
        BusTo = busTo;
    }

    public String getBusPrice() {
        return BusPrice;
    }

    public void setBusPrice(String busPrice) {
        BusPrice = busPrice;
    }

    public String getBusDP() {
        return BusDP;
    }

    public void setBusDP(String busDP) {
        BusDP = busDP;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }


}
