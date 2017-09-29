package com.mastercard.blockchain.model;

import java.io.Serializable;

public class Transfer implements Serializable {
    String hash; // blockchain entry hash
    String slot;
    String productAddress;

    public Transfer() {}

    public Transfer(Transfer transfer) {
        this.hash = transfer.hash;
        this.slot = transfer.slot;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getSlot() {
        return slot;
    }

    public void setSlot(String slot) {
        this.slot = slot;
    }


    public String getProductAddress() {
        return productAddress;
    }

    public void setProductAddress(String productAddress) {
        this.productAddress = productAddress;
    }

    @Override
    public String toString() {
        return "Transfer: " +
                "hash='" + hash + "', " +
                "slot='" + slot + "', " +
                "productAddress='" + productAddress;

    }
}
