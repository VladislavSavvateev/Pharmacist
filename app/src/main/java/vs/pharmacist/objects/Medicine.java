package vs.pharmacist.objects;

import java.io.Serializable;
import java.util.Random;

public class Medicine implements Serializable {
    private String name;
    private int count;
    private long ID;
    private CountType countType;

    public enum CountType {
        COUNT_PCS,
        COUNT_MILLIS
    }

    public Medicine(String name, CountType countType, int count, long ID) {
        this.name = name;
        this.countType = countType;
        this.count = count;
        this.ID = ID;
    }
    public Medicine(String name, CountType countType, int count) {
        this.name = name;
        this.countType = countType;
        this.count = count;
        generateRandomID();
    }
    public Medicine(String name) {
        this.name = name;
        countType = CountType.COUNT_PCS;
        count = 0;
        generateRandomID();
    }

    public String getName() {
        return name;
    }
    public int getCount() {
        return count;
    }
    public long getID() {
        return ID;
    }
    public CountType getCountType() {
        return countType;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setCount(int count) {
        this.count = count;
    }
    public void setID(long ID) {
        this.ID = ID;
    }
    public void setCountType(CountType countType) {
        this.countType = countType;
    }

    @Override
    public String toString() {
        switch (countType) {
            case COUNT_PCS:
                return "\"" + name + "\" (" + count + " ед.)";
            case COUNT_MILLIS:
                return "\"" + name + "\" (" + count + " мл.)";
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        Medicine m = (Medicine) obj;
        if (m.ID == this.ID) return true;
        return false;
    }

    public void generateRandomID() {
        ID = new Random().nextLong();
    }
}
