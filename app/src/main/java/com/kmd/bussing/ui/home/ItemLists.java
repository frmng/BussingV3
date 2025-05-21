package com.kmd.bussing.ui.home;

public class ItemLists {

    private String ItemText;
    private int Illustration;
    private String BackgroundColor;

    public ItemLists(String itemText, int illustration, String backgroundColor) {
        this.ItemText = itemText;
        this.Illustration = illustration;
        this.BackgroundColor = backgroundColor;
    }

    public ItemLists(){}

    public String getItemText() {
        return ItemText;
    }

    public void setItemText(String itemText) {
        ItemText = itemText;
    }

    public int getIllustration() {
        return Illustration;
    }

    public void setIllustration(int illustration) {
        Illustration = illustration;
    }

    public String getBackgroundColor() {
        return BackgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        BackgroundColor = backgroundColor;
    }
}
