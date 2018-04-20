package roll.main;

public class box <M> {
    M data;

    public box(int type) {
        switch (type) {
            case 0 : {
                this.data =(M) new Integer(5);
                break;
            }
            case 1 : {
                this.data = (M) new Boolean(true);
            }
            case 2 : {
                this.data = (M) new Object();
            }
        }
    }

    public M get(){
        return data;
    }
}
//package com.summershrimp.test;
//
//public class Main {
//
//    public static void main(String[] args) {
//        Integer a = (Integer) HelperClass.createClass(1).getElement();
//        String b = (String) HelperClass.createClass(2).getElement();
//        System.out.println(a);
//        System.out.println(b);
//    }
//}
//
//
//class box<M> {
//    BaseClass<M> base;
//    M get(){
//        return base.getElement();
//    }
//}
//class HelperClass {
//
//    static BaseClass<?> createClass(int options) {
//        if(options == 1) {
//            return new IntegerClass();
//        }
//        else {
//            return new StringClass();
//        }
//    }
//    int get(){
//
//    }
//}
//
//
//class BaseClass<T> {
//    T element;
//    public T getElement() {
//        return element;
//    }
//
//    public void setElement(T e){
//        element = e;
//    }
//}
//
//class IntegerClass extends BaseClass<Integer> {
//    IntegerClass(){
//        element = 1123123;
//    }
//}
//
//class StringClass extends BaseClass<String> {
//    StringClass(){
//        element = "aasdad";
//    }
//}