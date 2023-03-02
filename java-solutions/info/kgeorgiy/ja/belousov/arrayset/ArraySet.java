package info.kgeorgiy.ja.belousov.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements SortedSet<E> {
    private final List<E> data;
    private final Comparator<? super E> comparator;

    public ArraySet() {
        data = new ArrayList<>(0);
        comparator = null;
    }

    public ArraySet(Collection<? extends E> input) throws NullPointerException, ClassCastException {
        this(input, null);
    }

    public ArraySet(Collection<? extends E> input, Comparator<? super E> comparator) throws NullPointerException, ClassCastException {
        Objects.requireNonNull(input);

        this.comparator = comparator;

        ArrayList<E> rawArray = new ArrayList<>(input);
        this.data = rawArray;

        data.sort(comparator); // If comparator is null, natural ordering is used
        removeDuplicates();

        rawArray.trimToSize();
    }

    public ArraySet(SortedSet<E> input) throws NullPointerException {
        Objects.requireNonNull(input);
        this.data = new ArrayList<>(input);
        this.comparator = input.comparator();
    }

    /**
     * Fast unchecked private constructor from a valid ArrayList, can be used to construct subsets by views
     *
     * @param validArray a valid ArrayList of elements ordered in ascending order according to {@code cmp}
     *                   without duplicates and null elements
     * @param cmp        a comparator that can be used to order elements in the exact same order given in {@code validArray}
     */
    private ArraySet(List<E> validArray, Comparator<? super E> cmp) {
        Objects.requireNonNull(validArray);
        this.data = validArray;
        this.comparator = cmp;
    }

    @Override
    public Iterator<E> iterator() {
        return data.iterator();
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) throws IllegalArgumentException {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("fromElement > toElement");
        }

        int startIndex = lowerBound(fromElement);
        int endIndex = lowerBound(toElement);

        List<E> view = this.data.subList(startIndex, endIndex);
        return new ArraySet<>(view, this.comparator);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        int endIndex = lowerBound(toElement);
        List<E> view = this.data.subList(0, endIndex);
        return new ArraySet<>(view, this.comparator);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        int startIndex = lowerBound(fromElement);
        List<E> view = this.data.subList(startIndex, this.data.size());
        return new ArraySet<>(view, this.comparator);
    }

    @Override
    public E first() throws NoSuchElementException {
        expectNotEmpty();
        return data.get(0);
    }

    @Override
    public E last() throws NoSuchElementException {
        expectNotEmpty();
        return data.get(data.size() - 1);
    }

    @Override
    public boolean contains(Object object) throws ClassCastException {
        if (object == null) {
            return false;
        }

        @SuppressWarnings("unchecked")
        E typedObject = (E) object;

        return findElement(typedObject) >= 0;
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException(); // Avoid calling object.equals()
    }

    private void expectNotEmpty() throws NoSuchElementException {
        if (data.isEmpty()) {
            throw new NoSuchElementException("Collection is empty!");
        }
    }

    private void removeDuplicates() throws NullPointerException, ClassCastException {
        int lastElement = 0;

        if (data.size() < 1) {
            return;
        }

        Objects.requireNonNull(data.get(lastElement));

        if (data.size() < 2) {
            return;
        }

        for (int i = 1; i < data.size(); i++) {
            E a = data.get(lastElement);
            E b = data.get(i);

            Objects.requireNonNull(b);

            if (compare(a, b) != 0) {
                lastElement++;
                data.set(lastElement, b);
            }
        }

        if (data.size() > lastElement + 1) {
            data.subList(lastElement + 1, data.size()).clear();
        }
    }

    private int compare(E a, E b) throws ClassCastException {
        if (comparator == null) {
            @SuppressWarnings("unchecked")
            Comparable<? super E> naturalA = (Comparable<? super E>) a;

            return naturalA.compareTo(b);
        } else {
            return comparator.compare(a, b);
        }
    }

    /**
     * Binary search implementation that finds the first element that is greater or equal than given
     *
     * @param element an element to find in a collection
     * @return index of a first element in {@code data} that is greater or equal to the given {@code element}
     * if no such element exists, then {@code data.size()} is returned
     */
    private int lowerBound(E element) {
        Objects.requireNonNull(element);

        int l = -1;
        int r = data.size();

        while (r - l > 1) {
            int m = l + (r - l) / 2;
            if (compare(data.get(m), element) < 0) {
                l = m;
            } else {
                r = m;
            }
        }

        return r;
    }

    /**
     * Binary search implementation for specified element
     *
     * @param element an element to find in a collection
     * @return index of such element if found, -1 otherwise
     */
    private int findElement(E element) {
        int bound = lowerBound(element);
        if (bound < data.size()) {
            if (compare(element, data.get(bound)) == 0) {
                return bound;
            }
        }
        return -1;
    }
}
