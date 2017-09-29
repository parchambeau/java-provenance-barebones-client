package com.mastercard.blockchain.data;

import com.mastercard.blockchain.model.Address;
import com.mastercard.blockchain.model.ChainAddress;
import com.mastercard.blockchain.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

public class UserDataUtilTest {
    static UserDataUtil userDataUtil;

//    @BeforeClass
    public static void setup(){
        userDataUtil = new UserDataUtil();
    }

//    @Test
    public void testAddEntry()
    {
        User user = new User();
        user.setName("James");
        Address address = new Address();
        address.id = "identity";
        ChainAddress chainAddress = new ChainAddress(address);
        user.setAddress(chainAddress);
        user.setAuthority(false);
        userDataUtil.add(user);
    }

//    @Test
    public void testListEntry()
    {
        System.out.println(userDataUtil.list());
    }

//    @Test
    public void clear(){
        userDataUtil.clear();
    }
}
