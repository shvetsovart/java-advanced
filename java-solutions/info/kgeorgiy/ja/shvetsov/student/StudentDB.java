package info.kgeorgiy.ja.shvetsov.student;

import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements info.kgeorgiy.java.advanced.student.StudentQuery {
    private final Comparator<Student> nameComparator = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparing(Student::getId, Comparator.reverseOrder());

    private <T> Stream<T> streamByFunction(final List<Student> students, final Function<Student, T> function) {
        return students.stream().map(function);
    }

    private <T> List<T> listByFunction(final List<Student> students, final Function<Student, T> function) {
        // :NOTE: .toList()
        return streamByFunction(students, function).collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return listByFunction(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return listByFunction(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return listByFunction(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return listByFunction(
                students,
                student -> String.format("%s %s",
                        student.getFirstName(),
                        student.getLastName()));
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return streamByFunction(students, Student::getFirstName)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream()
                // :NOTE: может, стоило вынести Comparator.comparingInt(Student::getId)?
                .max(Comparator.comparingInt(Student::getId))
                .map(Student::getFirstName)
                .orElse("");
    }

    private List<Student> sortStudentsByComparator(final Collection<Student> students,
                                                   final Comparator<Student> comparator) {
        return students.stream().sorted(comparator).toList();
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudentsByComparator(students, Comparator.comparing(Student::getId));
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudentsByComparator(students, nameComparator);
    }

    private <T> List<Student> findStudentsByAttribute(final Collection<Student> students,
                                                      final T value,
                                                      final Function<Student, T> attribute) {
        return students.stream()
                .filter(student -> attribute.apply(student).equals(value))
                .sorted(nameComparator)
                .toList();
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsByAttribute(students, name, Student::getFirstName);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsByAttribute(students, name, Student::getLastName);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return findStudentsByAttribute(students, group, Student::getGroup);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return findStudentsByGroup(students, group).stream()
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(Comparator.naturalOrder())
                ));
    }

    // :NOTE: зачем?
    @Override
    public List<Map.Entry<String, String>> findStudentNamesByGroupList(List<Student> students, GroupName group) {
        return StudentQuery.super.findStudentNamesByGroupList(students, group);
    }
}
