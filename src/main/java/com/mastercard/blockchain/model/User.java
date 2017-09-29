package com.mastercard.blockchain.model;

import java.io.Serializable;

public class User implements Serializable {
    Boolean isAuthority;
    String name;
    ChainAddress address;

    public Boolean isAuthority() {
        return isAuthority;
    }

    public void setAuthority(boolean authority) {
        isAuthority = authority;
    }

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

    @Override
    public String toString() {
        return String.format("%s [address: %s]", isAuthority ? name + " - Authority" : name, address.getIdentity());
    }
}
