package info.kgeorgiy.ja.belousov.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.GroupQuery;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class StudentDB implements GroupQuery {
    private final static Comparator<? super Student> NAME_ORDERING = Comparator.comparing(Student::getLastName)
            .thenComparing(Student::getFirstName).reversed()
            .thenComparing(Comparator.naturalOrder());

    /**
     * Get the largest group by custom counting criteria with custom comparator on equal groups size
     *
     * @param students    Collection to search in
     * @param counter     Collector that accepts the whole group and counts some comparable parameter that is used
     *                    to determine the largest one
     * @param onEqualSize When there are few groups with the same largest "parameter" returned by {@code counter}
     *                    this comparator is used to find the biggest one.
     *                    It compares elements of type {@code Map.Entry<GroupName, C>}
     * @param <C>         Returned type of the {@code counter}
     * @return largest group name or null if there is no groups at all
     */
    private static <C extends Comparable<C>> GroupName getLargestGroupByCriteria(Collection<Student> students,
                                                                                 Collector<Student, ?, C> counter,
                                                                                 Comparator<Map.Entry<GroupName, C>> onEqualSize) {
        return students.stream().collect(Collectors.groupingBy(Student::getGroup, counter)).entrySet()
                .stream().max(Map.Entry.<GroupName, C>comparingByValue().thenComparing(onEqualSize))
                .map(Map.Entry::getKey).orElse(null);
    }

    private static <T> List<Student> findStudentsByCriteria(Collection<Student> students,
                                                            T template, Function<Student, T> supplier) {
        return students.stream().filter(student -> template.equals(supplier.apply(student)))
                .sorted(NAME_ORDERING).toList();
    }

    private static List<Group> getGroupByCriteria(Collection<Student> students,
                                                  Comparator<? super GroupName> groupOrder,
                                                  Comparator<? super Student> studentOrder) {
        return students.stream().collect(Collectors.groupingBy(Student::getGroup,
                        Collectors.toCollection(ArrayList::new))).entrySet().stream()
                .sorted(Map.Entry.comparingByKey(groupOrder))
                .map((key) -> new Group(key.getKey(), key.getValue().stream().sorted(studentOrder).toList())).toList();
    }

    private static List<Student> sortStudentByCriteria(Collection<Student> students, Comparator<? super Student> criteria) {
        return students.stream().sorted(criteria).toList();
    }


    private static <T> List<T> extractParam(List<Student> students, Function<Student, T> extractor) {
        return students.stream().map(extractor).toList();
    }

    /**
     * @return Unmodifiable list of students first names
     */
    @Override
    public List<String> getFirstNames(List<Student> students) {
        return extractParam(students, Student::getFirstName);
    }

    /**
     * @return Unmodifiable list of students last names
     */
    @Override
    public List<String> getLastNames(List<Student> students) {
        return extractParam(students, Student::getLastName);
    }

    /**
     * @return Unmodifiable list of groups
     */
    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return extractParam(students, Student::getGroup);
    }

    /**
     * @return Unmodifiable list of students full names
     */
    @Override
    public List<String> getFullNames(List<Student> students) {
        return extractParam(students, student -> student.getFirstName() + " " + student.getLastName());
    }

    /**
     * @return Modifiable sorted set (TreeSet) of students full names
     */
    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return students.stream()
                .map(Student::getFirstName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * @return Student with max id or "" if empty list is given
     */
    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream()
                .max(Comparator.naturalOrder())
                .map(Student::getFirstName)
                .orElse("");
    }

    /**
     * @return Unmodifiable list of students sorted by id
     */
    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudentByCriteria(students, Comparator.naturalOrder());
    }

    /**
     * @return Unmodifiable list of students sorted by name
     */
    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudentByCriteria(students, NAME_ORDERING);
    }

    /**
     * @return Unmodifiable list of students with the given first name
     */
    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsByCriteria(students, name, Student::getFirstName);
    }

    /**
     * @return Unmodifiable list of students with the given last name
     */
    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsByCriteria(students, name, Student::getLastName);
    }

    /**
     * @return Unmodifiable list of students with the given group
     */
    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return findStudentsByCriteria(students, group, Student::getGroup);
    }

    /**
     * @return Unmodifiable map of student last names mapped to the corresponding min first names
     */
    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return students.stream()
                .filter(student -> group.equals(student.getGroup()))
                .collect(Collectors.toUnmodifiableMap(
                        Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(Comparator.naturalOrder())));
    }

    /**
     * @return Unmodifiable list of groups sorted by group name with students sorted by name
     */
    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupByCriteria(students, Comparator.naturalOrder(), NAME_ORDERING);
    }

    /**
     * @return Unmodifiable list of groups sorted by group name with students sorted by id
     */
    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupByCriteria(students, Comparator.naturalOrder(), Comparator.naturalOrder());
    }

    /**
     * Gets the largest group by students count
     *
     * @return value or null if there is no groups at all
     */
    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return getLargestGroupByCriteria(students, Collectors.counting(), Map.Entry.comparingByKey());
    }

    /**
     * Gets the largest group by student first names count
     *
     * @return value or null if there is no groups at all
     */
    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupByCriteria(students,
                Collectors.mapping(Student::getFirstName,
                        Collectors.collectingAndThen(Collectors.toUnmodifiableSet(), Set::size)),
                Map.Entry.<GroupName, Integer>comparingByKey().reversed());
    }
}

// 1 3 5 7
// subset(2, 6) = 3 5
