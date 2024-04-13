import random.io;
import random.Extension;
import FieldExample;

class Example extends Extension{

    int randomField;
    FieldExample fieldExample;


    public static void main(String[] anything){ // main method
        // Example of a comment
        int a ;
        int b ;
        int[] arr ; // example of an array initialization
        Example m;

        a = 1;
        b = 2;
        m = new Example();

        a = Extension.some();
        b = m.exampleMethod(a,b);

    }

    public int exampleMethod(int a, int b){ // example of a method
        return a + b;
    }



}