package org.liquidplayer.androidjscoretest;

import org.liquidplayer.webkit.javascriptcore.JSArray;
import org.liquidplayer.webkit.javascriptcore.JSContext;
import org.liquidplayer.webkit.javascriptcore.JSObject;
import org.liquidplayer.webkit.javascriptcore.JSValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * Created by Eric on 6/11/16.
 */
public class JSArrayTest extends JSTest {
    JSArrayTest(MainActivity activity) {
        super(activity);
    }

    public void testJSArrayConstructors() throws TestAssertException {
        JSContext context = track(new JSContext(),"testJSArrayContructors:context");

        /**
         * new JSArray(context, JSValue[], cls)
         */
        JSValue [] initializer = new JSValue[] { new JSValue(context,1), new JSValue(context,"two")};
        JSArray<JSValue> array = new JSArray<JSValue>(context, initializer, JSValue.class);
        tAssert(array.size()==2 && array.get(0).equals(1) && array.get(1).equals("two"),
                "new JSArray(context, JSValue[], cls)");

        /**
         * new JSArray(context, cls)
         */
        JSArray<Integer> array2 = new JSArray<Integer>(context,Integer.class);
        array2.add(10);
        array2.add(20);
        tAssert(array2.size()==2 && array2.get(0).equals(10) && array2.get(1).equals(20),
                "new JSArray(context, cls)");

        /**
         * new JSArray(context, Object[], cls)
         */
        Object [] objinit = new Object [] { 1, 2.0, "three"};
        JSArray<JSValue> array3 = new JSArray<JSValue>(context, objinit, JSValue.class);
        tAssert(array3.size()==3 && array3.get(0).equals("1") && array3.get(1).isStrictEqual(2) &&
                array3.get(2).isStrictEqual("three"),
                "new JSArray(context, Object[], cls)");

        /**
         * new JSArray(context, List, cls)
         */
        List<String> list = new ArrayList<>();
        list.add("first");
        list.add("second");
        JSArray<String> array4 = new JSArray<String>(context,list,String.class);
        tAssert(array4.size()==2 && array4.get(0).equals("first") && array4.get(1).equals("second"),
                "new JSArray(context, List, cls)");

    }

    public void testJSArrayListMethods() throws TestAssertException {
        JSContext context = track(new JSContext(),"testJSArrayListMethods:context");

        List<Object> list = new JSArray<Object>(context,Object.class);
        /**
         * JSArray.add(value)
         */
        list.add("zero");
        list.add(1);
        list.add(2.0);
        list.add(new Integer [] {3});
        list.add(new JSObject(context));
        tAssert(list.get(0).equals("zero") && list.get(1).equals(1) && list.get(2).equals(2) &&
                ((JSValue)list.get(3)).isArray() && ((JSValue)list.get(4)).isObject(),
                "JSArray.add(value)");

        /**
         * JSArray.toArray()
         */
        Object[] array = list.toArray();
        tAssert(array[0].equals("zero") && array[1].equals(1) && array[2].equals(2) &&
                ((JSValue)array[3]).isArray() && ((JSValue)array[4]).isObject(),
                "JSArray.toArray()");

        /**
         * JSArray.get(index)
         */
        ((JSArray)list).propertyAtIndex(list.size(),"anotherone");
        tAssert(list.get(5).equals("anotherone"), "JSArray.get(index)");
        tAssert(((JSValue)list.get(3)).isArray(), "JSArray.get(index) -> array");

        /**
         * JSArray.size()
         */
        tAssert(list.size()==6,"JSArray.size()");

        /**
         * JSArray.isEmpty()
         */
        List<Integer> list2 = new JSArray<Integer>(context,Integer.class);
        tAssert(!list.isEmpty() && list2.isEmpty(), "JSArray.isEmpty()");

        /**
         * JSArray.contains(object)
         */
        tAssert(list.contains("zero") && list.contains(1) && list.contains(2.0) &&
                !list.contains(5),
                "JSArray.contains(object)");

        /**
         * JSArray.iterator()
         */
        int i=0;
        for(Iterator<Object> it = list.iterator(); it.hasNext(); i++) {
            Object next = it.next();
            tAssert(list.contains(next), "JSArray.iterator() -> " + next);
        }
        tAssert(i==list.size(), "JSArray.iterator()");

        /**
         * JSArray.toArray(Object[])
         */
        list2.add(0);
        list2.add(1);
        list2.add(2);
        Integer [] arr1 = new Integer[3];
        Integer [] arr2 = list2.toArray(arr1);
        tAssert(arr2.length==3 && arr2[0].equals(0) && arr2[1].equals(1) && arr2[2].equals(2),
                "JSArray.toArray(T[]) -> same size");
        list2.add(3);
        arr2 = list2.toArray(arr1);
        tAssert(arr2.length==4 && arr2[0].equals(0) && arr2[1].equals(1) && arr2[2].equals(2) &&
                arr2[3].equals(3),
                "JSArray.toArray(T[]) -> greater than arg size");
        list2.remove(3);
        list2.remove(2);
        arr2 = list2.toArray(arr1);
        tAssert(arr2.length==3 && arr2[0].equals(0) && arr2[1].equals(1) && arr2[2]==null,
                "JSArray.toArray(T[]) -> less than arg size");

        /**
         * JSArray.remove(object)
         */
        tAssert(list2.remove(Integer.valueOf(1)) && !list2.remove(Integer.valueOf(2)) &&
                !list2.contains(1),
                "JSArray.remove(object)");

        /**
         * JSArray.containsAll(collection)
         */
        Collection<Object> collection = new ArrayList<Object>();
        collection.add("zero");
        collection.add(1);
        collection.add(2);
        Collection<Object> collection2 = new ArrayList<Object>(collection);
        collection2.add(25.0);
        tAssert(list.containsAll(collection) && !list.containsAll(collection2),
                "JSArray.containsAll(collection)");

        /**
         * JSArray.addAll(collection)
         */
        int size = list.size();
        list.addAll(collection);
        tAssert(list.size() == size + collection.size(),
                "JSArray.addAll(collection)");

        /**
         * JSArray.removeAll(collection)
         */
        size = list.size();
        list.removeAll(collection);
        tAssert(list.size() == size - collection.size()*2,
                "JSArray.removeAll(collection)");

        /**
         * JSArray.retainAll(collection)
         */
        list.addAll(collection);
        list.retainAll(collection);
        tAssert(list.size()==collection.size() && list.containsAll(collection),
                "JSArray.retainAll(collection)");

        /**
         * JSArray.clear()
         */
        list.clear();
        tAssert(list.size()==0, "JSArray.clear()");

        /**
         * JSArray.set(index,object)
         */
        list.addAll(collection);
        Object last1;
        try {
            Object last2 = list.set(10, "bar");
            last1 = 0;
        } catch(IndexOutOfBoundsException e) {
            last1 = list.set(1,"foo");
        }
        tAssert(last1.equals(1) && list.get(1).equals("foo"),
                "JSArray.set(index,object)");

        /**
         * JSArray.add(index,object)
         */
        list.add(1,"hello");
        list.add(4,"world");
        try {
            list.add(10, 10.0);
        } catch (IndexOutOfBoundsException e) {

        }
        tAssert(list.get(1).equals("hello") && list.get(2).equals("foo") && list.size()==5 &&
                list.get(4).equals("world"), "JSArray.add(index,object)");

        /**
         * JSArray.remove(index)
         */
        list.remove(4);
        list.remove(1);
        tAssert(list.get(1).equals("foo") && list.size()==3, "JSArray.remove(index)");

        /**
         * JSArray.indexOf(object)
         */
        list.addAll(collection);
        tAssert(list.indexOf("zero")==0 && list.indexOf("foo")==1 && list.indexOf(2)==2 &&
                list.indexOf(1)==4 && list.indexOf("world")==-1,
                "JSArray.indexOf(object)");

        /**
         * JSArray.lastIndexOf(object)
         */
        tAssert(list.lastIndexOf("zero")==3 && list.lastIndexOf("foo")==1 && list.lastIndexOf(2)==5 &&
                list.lastIndexOf(1)==4 && list.lastIndexOf("world")==-1,
                "JSArray.lastIndexOf(object)");

        /**
         * JSArray.listIterator()
         */
        // List iterator is heavily used by underlying JSArray methods already tested.  Only
        // underlying methods untested are 'set' and 'add'
        for (ListIterator<Object> it = list.listIterator(); it.hasNext(); ) {
            Object dupe = it.next();
            it.set("changed");
            it.add(dupe);
        }
        tAssert(list.size()==12 && list.indexOf("changed")==0 && list.lastIndexOf("changed")==10,
                "JSArray.listIterator()");

        /**
         * JSArray.listIterator(index)
         */
        for (ListIterator<Object> it=list.listIterator(0); it.hasNext(); ) {
            if (it.next().equals("changed")) it.remove();
        }
        tAssert(list.listIterator(list.size()).previous().equals(list.listIterator(list.size()+10).previous()) &&
                list.size() == 6,
                "JSArray.listIterator(index)");

        /**
         * JSArray.subList(fromIndex, toIndex)
         */
        list.subList(1,4).clear();
        tAssert(list.size() == 3 && list.get(0).equals("zero") && list.get(1).equals(1) &&
                list.get(2).equals(2), "JSArray.subList(fromIndex,toIndex)");

        /**
         * JSArray.equals()
         */
        ArrayList<Object> arrayList = new ArrayList<Object>(collection);
        tAssert(list.equals(arrayList) && !list.equals(list2), "JSArray.equals()");

        /**
         * JSArray.hashCode()
         */
        JSArray<Object> hashList = new JSArray<Object>(context,collection,Object.class);
        ArrayList<Object> arrayList2 = new ArrayList<Object>();
        arrayList2.add("zero");
        arrayList2.add(1.0); // <-- Note: making these Doubles is necessary for hashCode match
        arrayList2.add(2.0); // <--
        tAssert(list.hashCode() == hashList.hashCode() && list.hashCode() != list2.hashCode() &&
                list.hashCode() == arrayList2.hashCode() && list.equals(arrayList2),
                "JSArray.hashCode()");
    }

    @Override
    public void run() throws TestAssertException {
        println("**** JSArray ****");
        testJSArrayConstructors();
        testJSArrayListMethods();
        println("-----------------");
    }

}
