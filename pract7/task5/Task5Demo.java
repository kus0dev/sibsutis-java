package task5;

import src.Course;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// демонстрация бинарного формата с проверками
public class Task5Demo {
  public static void main(String[] args) throws IOException {
    List<Course> courses = new ArrayList<>();
    courses.add(new Course(1, "Java", 10));
    courses.add(new Course(2, "DB", 12));

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(baos)) {
      CatalogBinary.write(out, courses);
    }

    // чтение
    byte[] data = baos.toByteArray();
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
      List<Course> read = CatalogBinary.read(in);
      System.out.println("read: " + read.size());
    }

    // обрезанный файл
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data, 0, 10))) {
      CatalogBinary.read(in);
    } catch (IOException e) {
      System.out.println("truncated: " + e.getMessage());
    }

    // неизвестная версия
    ByteArrayOutputStream bad = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(bad)) {
      out.writeInt(0x4341544C);
      out.writeShort(99); // bad version
      out.writeInt(0);
    }
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bad.toByteArray()))) {
      CatalogBinary.read(in);
    } catch (IOException e) {
      System.out.println("version: " + e.getMessage());
    }
  }
}
