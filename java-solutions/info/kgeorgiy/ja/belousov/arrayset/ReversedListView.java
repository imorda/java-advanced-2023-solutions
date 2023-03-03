package info.kgeorgiy.ja.belousov.arrayset;

import java.util.AbstractList;
import java.util.List;

public class ReversedListView<T> extends AbstractList<T> {
    final private List<T> data;

    public static <T> List<T> reverseList(List<T> original){
        if(original instanceof ReversedListView<T>){
            return ((ReversedListView<T>) original).data; // Optimization
        }
        return new ReversedListView<>(original);
    }

    private ReversedListView(List<T> data) {
        this.data = data;
    }

    @Override
    public T get(int index) {
        return data.get(convertIndex(index));
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public T set(int index, T element) {
        return data.set(convertIndex(index), element);
    }

    @Override
    public void add(int index, T element) {
        data.add(convertIndex(index) + 1, element);
    }

    @Override
    public T remove(int index) {
        return data.remove(convertIndex(index));
    }

    private int convertIndex(int i){
        return size() - 1 - i;
    }
}
