package net.legendofwar.firecord.tool;

public class SortHelper {


    static public double getSortScore(String value) {
        if (value == null){
            return 0.0d;
        }
        String subsString = value.substring(0, Math.min(8, value.length()));
        double total = 0.0d;
        char[] ca = subsString.toCharArray();
        for (int i = 0; i < ca.length; i++){
            total += Math.pow(256.0*256, ca.length-i-1) * ca[i];
        }
        return total;
    }
    
}
