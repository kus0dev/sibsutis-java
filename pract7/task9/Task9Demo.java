package task9;

import src.Course;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// демонстрация версионируемого формата
public class Task9Demo {
  public static void main(String[] args) throws IOException {
    List<Course> courses = new ArrayList<>();
    Course c = new Course(1, "Java", 10);
    c.setCapacity(25);
    courses.add(c);

    // v2
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(baos)) {
      VersionedFormat.writeV2(out, courses);
    }
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
      List<Course> read = VersionedFormat.read(in);
      System.out.println("v2: " + read.get(0).capacity());
    }

    // миграция v1 -> v2
    ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(baos1)) {
      VersionedFormat.writeV1(out, courses);
    }
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(baos1.toByteArray()))) {
      List<Course> read = VersionedFormat.read(in);
      System.out.println("migrated: " + read.get(0).capacity());
    }

    // будущая версия не читается как старая
    ByteArrayOutputStream badv = new ByteArrayOutputStream();
    try (DataOutputStream out = new DataOutputStream(badv)) {
      out.writeInt(0x56455253);
      out.writeShort(99);
      out.writeInt(0);
      out.write(new byte[32]);
    }
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(badv.toByteArray()))) {
      VersionedFormat.read(in);
    } catch (IOException e) {
      System.out.println("future: " + e.getMessage());
    }

    // повреждение до публикации
    byte[] data = baos.toByteArray();
    data[data.length - 1] ^= 0xFF;
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
      VersionedFormat.read(in);
    } catch (IOException e) {
      System.out.println("corrupt: " + e.getMessage());
    }
  }
}
