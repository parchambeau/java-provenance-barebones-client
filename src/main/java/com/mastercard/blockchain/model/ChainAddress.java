package com.mastercard.blockchain.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class ChainAddress implements Serializable{
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChainAddress that = (ChainAddress) o;
        return Arrays.equals(getPrivateKey(), that.getPrivateKey()) &&
                Objects.equals(getWif(), that.getWif()) &&
                Objects.equals(getIdentity(), that.getIdentity()) &&
                Arrays.equals(getPublicKey(), that.getPublicKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPrivateKey(), getWif(), getIdentity(), getPublicKey());
    }

    private byte[] privateKey;
    private String wif;
    private String identity;
    private byte[] publicKey;

    public ChainAddress(Address address) {
        this.identity = address.id;
        this.publicKey = address.publicKey;
        this.wif = address.wif;
        this.privateKey = address.privateKey;
    }

    public ChainAddress() {}

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public String getWif() {
        return wif;
    }

    public String getIdentity() {
        return identity;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }
}
