package com.mastercard.blockchain.data;

import com.mastercard.blockchain.model.ChainAddress;
import com.mastercard.blockchain.model.Product;

public class ProductDataUtil extends DataUtil<Product> {
    private static String FILE_NAME = "Product.bin";

    public ProductDataUtil() {
        super();
    }

    public String fileName() {
        return FILE_NAME;
    }

    public Product getByHash(String hash) {
        for (Product product : getData()) {
            if (product.getHash().equals(hash))
                return product;
        }
        return null;
    }

    public Product getByAddress(ChainAddress address)
    {
        for (Product product : getData()) {
            if (product.getAddress().equals(address))
                return product;
        }
        return null;
    }

}
