package com.mastercard.blockchain.data;

import com.mastercard.blockchain.model.User;

public class UserDataUtil extends DataUtil<User> {
    private static String FILE_NAME = "User.bin";

    public UserDataUtil() {
        super();
    }

    public User getByAddress(String address){
        for(User user : getData())
        {
            if(user.getAddress().getIdentity().equals(address))
            {
                return user;
            }
        }
        return null;
    }

    public String fileName() {
        return FILE_NAME;
    }
}
