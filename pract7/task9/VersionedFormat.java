package task9;

import src.Course;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

// schema version, checksum, migration v1->v2
public class VersionedFormat {
  private static final int MAGIC = 0x56455253; // 'VERS'
  private static final short VERSION_1 = 1;
  private static final short VERSION_2 = 2;
  private static final short FUTURE = 99;

  public static void writeV2(DataOutputStream out, List<Course> courses) throws IOException {
    if (out == null || courses == null) throw new IllegalArgumentException("null args");
    ByteArrayData ba = new ByteArrayData();
    try (DataOutputStream tmp = new DataOutputStream(ba)) {
      tmp.writeInt(courses.size());
      for (Course c : courses) {
        tmp.writeLong(c.id());
        tmp.writeUTF(c.title());
        tmp.writeInt(c.durationHours());
        tmp.writeUTF(c.status().name());
        tmp.writeInt(c.capacity());
      }
    }
    byte[] payload = ba.toByteArray();
    byte[] checksum = sha256(payload);
    out.writeInt(MAGIC);
    out.writeShort(VERSION_2);
    out.writeInt(payload.length);
    out.write(payload);
    out.write(checksum);
    out.flush();
  }

  public static List<Course> read(DataInputStream in) throws IOException {
    if (in == null) throw new IllegalArgumentException("null");
    int magic = in.readInt();
    if (magic != MAGIC) throw new IOException("bad magic");
    short version = in.readShort();
    if (version > VERSION_2) throw new IOException("unknown future version: " + version); // не читаем как старую
    int payloadLen = in.readInt();
    if (payloadLen < 0) throw new IOException("bad length");
    byte[] payload = new byte[payloadLen];
    in.readFully(payload);
    byte[] given = new byte[32];
    in.readFully(given);
    byte[] calc = sha256(payload);
    if (!MessageDigest.isEqual(calc, given)) throw new IOException("checksum mismatch"); // повреждение до публикации
    try (DataInputStream din = new DataInputStream(new java.io.ByteArrayInputStream(payload))) {
      if (version == VERSION_1) {
        // миграция v1 -> v2
        int count = din.readInt();
        List<Course> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
          long id = din.readLong();
          String title = din.readUTF();
          int hours = din.readInt();
          String status = din.readUTF();
          Course c = new Course(id, title, hours);
          try { c.setStatus(Course.Status.valueOf(status)); } catch (Exception ignored) {}
          // в v1 не было capacity - мигрируем дефолтным
          c.setCapacity(30);
          list.add(c);
        }
        return list;
      } else if (version == VERSION_2) {
        int count = din.readInt();
        List<Course> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
          long id = din.readLong();
          String title = din.readUTF();
          int hours = din.readInt();
          String status = din.readUTF();
          int capacity = din.readInt();
          Course c = new Course(id, title, hours);
          try { c.setStatus(Course.Status.valueOf(status)); } catch (Exception ignored) {}
          c.setCapacity(capacity);
          list.add(c);
        }
        return list;
      }
    }
    throw new IOException("unsupported version");
  }

  // запись v1 для теста миграции
  public static void writeV1(DataOutputStream out, List<Course> courses) throws IOException {
    ByteArrayData ba = new ByteArrayData();
    try (DataOutputStream tmp = new DataOutputStream(ba)) {
      tmp.writeInt(courses.size());
      for (Course c : courses) {
        tmp.writeLong(c.id());
        tmp.writeUTF(c.title());
        tmp.writeInt(c.durationHours());
        tmp.writeUTF(c.status().name());
      }
    }
    byte[] payload = ba.toByteArray();
    byte[] checksum = sha256(payload);
    out.writeInt(MAGIC);
    out.writeShort(VERSION_1);
    out.writeInt(payload.length);
    out.write(payload);
    out.write(checksum);
    out.flush();
  }

  private static byte[] sha256(byte[] data) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return md.digest(data);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  static class ByteArrayData extends java.io.ByteArrayOutputStream {}
}
