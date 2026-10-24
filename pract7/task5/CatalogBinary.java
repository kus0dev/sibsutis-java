package task5;

import src.Course;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// бинарный формат: magic, version, count, records
public class CatalogBinary {
  private static final int MAGIC = 0x4341544C; // 'CATL'
  private static final short VERSION = 1;

  public static void write(DataOutputStream out, List<Course> courses) throws IOException {
    if (out == null || courses == null) throw new IllegalArgumentException("null args");
    out.writeInt(MAGIC);
    out.writeShort(VERSION);
    out.writeInt(courses.size());
    for (Course c : courses) {
      out.writeLong(c.id());
      out.writeUTF(c.title());
      out.writeInt(c.durationHours());
      out.writeUTF(c.status().name());
    }
    out.flush();
  }

  public static List<Course> read(DataInputStream in) throws IOException {
    if (in == null) throw new IllegalArgumentException("in must not be null");
    // проверка magic
    int magic = in.readInt();
    if (magic != MAGIC) throw new IOException("bad magic: " + Integer.toHexString(magic));
    // проверка версии
    short version = in.readShort();
    if (version != VERSION) throw new IOException("unknown version: " + version);
    int count = in.readInt();
    if (count < 0) throw new IOException("negative count");
    List<Course> list = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      try {
        long id = in.readLong();
        String title = in.readUTF();
        int hours = in.readInt();
        String status = in.readUTF();
        Course c = new Course(id, title, hours);
        try {
          c.setStatus(Course.Status.valueOf(status));
        } catch (Exception ignored) {}
        list.add(c);
      } catch (Exception e) {
        throw new IOException("truncated file at record " + i, e);
      }
    }
    return list;
  }
}
