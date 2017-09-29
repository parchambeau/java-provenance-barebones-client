package com.mastercard.blockchain.data;

import com.mastercard.blockchain.model.User;
import org.junit.Test;

import java.util.Arrays;

public class DataHelperTest {

//    @Test
    public void testReadFile(){
        DataUtil dataHelper = new DataUtil();
        dataHelper.readFromFile();
        for(Object content : dataHelper.getData())
        {
            System.out.println((String)content);
        }
    }

//    @Test
    public void testUserWriteFile(){
        UserDataUtil userDataHelper = new UserDataUtil();
        userDataHelper.addAll(Arrays.asList(
                new User(),new User()
        ));
    }
}
