// Created by plusminus on 20:36:01 - 26.09.2008
package com.ikroshlab.ovtencoder;

public class MyMath{
    private MyMath() {
    }

    public static double gudermannInverse(double aLatitude) {
        return Math.log(Math.tan(0.7853981852531433D + 0.01745329238474369D * aLatitude / 2.0D));
    }

    public static double gudermann(double y) {
        return 57.295780181884766D * Math.atan(Math.sinh(y));
    }

    public static int mod(int number, int modulus) {
        if(number > 0) {
            return number % modulus;
        } else {
            while(number < 0) {
                number += modulus;
            }

            return number;
        }
    }
}
