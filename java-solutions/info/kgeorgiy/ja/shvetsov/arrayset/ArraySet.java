package info.kgeorgiy.ja.shvetsov.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements SortedSet<T> {
    private final List<T> set;
    private final Comparator<T> comparator;

    public ArraySet() {
        set = new ArrayList<>();
        this.comparator = null;
    }

    public ArraySet(int capacity) {
        set = new ArrayList<>(capacity);
        this.comparator = null;
    }

    public ArraySet(final Collection<T> collection) {
        Set<T> treeSet = new TreeSet<>(collection);

        set = new ArrayList<>(treeSet);
        this.comparator = null;
    }

    public ArraySet(final Collection<T> collection, final Comparator<T> comparator) {
        Set<T> treeSet = new TreeSet<>(comparator);
        treeSet.addAll(collection);

        set = new ArrayList<>(treeSet);
        this.comparator = comparator;
    }

    public ArraySet(final Comparator<T> comparator) {
        set = new ArrayList<>();

        this.comparator = comparator;
    }

    @Override
    public Comparator<? super T> comparator() {
        return this.comparator;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SortedSet<T> subSet(T fromElement, T toElement) {
        if (comparator != null) {
            if (comparator.compare(fromElement, toElement) > 0) {
                throw new IllegalArgumentException("Invalid arguments " +
                        "(fromElement, toElement): " + fromElement + ", " + toElement);
            }
        } else {
            if (fromElement instanceof Comparable &&
                    toElement instanceof Comparable &&
                    ((Comparable<T>) fromElement).compareTo(toElement) > 0) {
                throw new IllegalArgumentException("Invalid arguments " +
                        "(fromElement, toElement): " + fromElement + ", " + toElement);
            }
        }

        return new ArraySet<>(set.subList(getIndex(fromElement), getIndex(toElement)), comparator);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return (this.isEmpty() ?
                new ArraySet<>(comparator) :
                new ArraySet<>(set.subList(0, getIndex(toElement)), comparator));
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return (this.isEmpty() ?
                new ArraySet<>(comparator) :
                new ArraySet<>(set.subList(getIndex(fromElement), set.size()), comparator));
    }

    @Override
    public T first() {
        if (set.isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return set.get(0);
        }
    }

    @Override
    public T last() {
        if (set.isEmpty()) {
            throw new NoSuchElementException();
        } else {
            return set.get(set.size() - 1);
        }
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return Collections.binarySearch(set, (T) Objects.requireNonNull(o), comparator) >= 0;
    }

    @SuppressWarnings("unchecked")
    private int getIndex(Object o) {

        try {
            int index = Collections.binarySearch(set, (T) Objects.requireNonNull(o), comparator);
            if (index < 0) {
                index = -index - 1;
            }
            return index;
        } catch (ClassCastException e) {
            System.err.println("getIndex(o) ClassCastException: " + e.getMessage());
            return -1;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return set.iterator();
    }
}
