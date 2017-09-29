package com.mastercard.blockchain.data;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class DataUtil<T> extends BaseDataUtil {
    private List<T> data;
    private static String FILE_NAME = "data.bin";

    public DataUtil() {
        if (data == null) {
            data = new ArrayList<T>();
        }
        readFromFile();
    }

    public List<T> getData() {
        return data;
    }

    public void add(T object) {
        data.add(object);
        saveToFile();
    }

    public void clear() {
        data.clear();
        saveToFile();
    }

    public void saveToFile() {
        try {
            OutputStream file = new FileOutputStream(fileName());
            OutputStream buffer = new BufferedOutputStream(file);
            ObjectOutput output = new ObjectOutputStream(buffer);
            try {
                output.writeObject(data);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                output.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void readFromFile() {
        if (new File(fileName()).exists()) {
            try {
                InputStream file = new FileInputStream(fileName());
                InputStream buffer = new BufferedInputStream(file);
                ObjectInput input = new ObjectInputStream(buffer);
                try {
                    List<T> readData = (List<T>) input.readObject();
                    data.addAll(readData);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    input.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String list() {

        String printout = "";
        int startIndex = 1;
        for (T dataEntry : data) {
            printout += startIndex++ + ": " + dataEntry.toString() + "\n";
        }
        return printout;
    }

    public void addAll(Collection<T> appendList) {
        data.addAll(appendList);
        saveToFile();
    }

    public String fileName() {
        return FILE_NAME;
    }

    public T get(int index)
    {
        if (getData().size() >= index && index > 0)
            return getData().get(index - 1);
        else
            return null;
    }
}
