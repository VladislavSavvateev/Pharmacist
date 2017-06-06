package vs.pharmacist.objects;

import java.io.Serializable;

public class PlaceDetails implements Serializable {

    public String status;
    public Place result;

    @Override
    public String toString() {
        if (result!=null) {
            return result.toString();
        }
        return super.toString();
    }
}