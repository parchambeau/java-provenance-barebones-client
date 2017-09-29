package com.mastercard.blockchain.data;

import com.mastercard.blockchain.model.Product;
import com.mastercard.blockchain.model.Transfer;

import java.util.List;
import java.util.stream.Collectors;

public class TransferDataUtil extends DataUtil<Transfer> {
    private static String FILE_NAME = "Transfer.bin";

    public TransferDataUtil() {
        super();
    }

    public String fileName() {
        return FILE_NAME;
    }

    public List<Transfer> getListByProduct(Product product) {
        List<Transfer> transferHistory = getData().stream().filter(transfer -> transfer.getProductAddress().equals(product.getAddress().getIdentity()))
                .map(Transfer::new).collect(Collectors.toList());
        return transferHistory;
    }
}
