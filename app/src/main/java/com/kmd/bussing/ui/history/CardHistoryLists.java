package com.kmd.bussing.ui.history;

public class CardHistoryLists {
    private String TransactionFrom, TransactionTo, TransactionTicketNo, TransactionPrice, TransactionTimeStamp;

    public CardHistoryLists(String transactionFrom, String transactionTo, String transactionTicketNo, String transactionPrice, String transactionTimeStamp) {
        this.TransactionFrom = transactionFrom;
        this.TransactionTo = transactionTo;
        this.TransactionTicketNo = transactionTicketNo;
        this.TransactionPrice = transactionPrice;
        this.TransactionTimeStamp = transactionTimeStamp;
    }

    public CardHistoryLists() {}

    public String getTransactionTo() {
        return TransactionTo;
    }

    public String getTransactionFrom() {
        return TransactionFrom;
    }

    public String getTransactionTicketNo() {
        return TransactionTicketNo;
    }

    public String getTransactionPrice() {
        return TransactionPrice;
    }

    public String getTransactionTimeStamp() {
        return TransactionTimeStamp;
    }
}
