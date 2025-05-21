package com.kmd.bussing.ui.wallet;

public class UserWalletList {

    private String userActivity, transactionTimeStamp, userId;
    private double transactionPrice;

    public UserWalletList(String userActivity, String transactionTimeStamp, String userId, double transactionPrice) {
        this.userActivity = userActivity;
        this.transactionTimeStamp = transactionTimeStamp;
        this.userId = userId;
        this.transactionPrice = transactionPrice;
    }

    public UserWalletList() {}

    public String getUserActivity() {
        return userActivity;
    }

    public void setUserActivity(String userActivity) {
        this.userActivity = userActivity;
    }

    public String getTransactionTimeStamp() {
        return transactionTimeStamp;
    }

    public void setTransactionTimeStamp(String transactionTimeStamp) {
        this.transactionTimeStamp = transactionTimeStamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getTransactionPrice() {
        return transactionPrice;
    }

    public void setTransactionPrice(double transactionPrice) {
        this.transactionPrice = transactionPrice;
    }
}
