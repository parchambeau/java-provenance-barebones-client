package com.mastercard.blockchain.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Product implements Serializable{
    String name;
    String manufacturerReference;
    ChainAddress address;
    String hash;
    List<Transfer> transfers = new ArrayList<Transfer>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ChainAddress getAddress() {
        return address;
    }

    public void setAddress(ChainAddress address) {
        this.address = address;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public List<Transfer> getTransfers() {
        return transfers;
    }

    public void setTransfers(List<Transfer> transfers) {
        this.transfers = transfers;
    }

    public String getManufacturerReference() {
        return manufacturerReference;
    }

    public void setManufacturerReference(String manufacturerReference) {
        this.manufacturerReference = manufacturerReference;
    }

    @Override
    public String toString() {
        return String.format("%s [entry hash: %s manufacturer ref: %s transfers: %d]", name, hash, manufacturerReference, transfers.size());
    }
}
