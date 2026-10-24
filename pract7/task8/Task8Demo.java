package task8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

// потоковый импорт большого файла без readAllLines
public class Task8Demo {
  public static void main(String[] args) throws IOException {
    StringBuilder sb = new StringBuilder();
    for (int i = 1; i <= 100; i++) {
      sb.append(i).append(";Course").append(i).append(";").append(i % 5 + 1).append("\n");
    }
    sb.append("x;bad\n").append("y;bad\n").append("z;bad\n");

    StreamImporter importer = new StreamImporter(5, 2); // отмена после 2 ошибок
    StreamImporter.ImportStats stats = importer.importStream(new BufferedReader(new StringReader(sb.toString())));
    System.out.println("ok=" + stats.ok() + " bad=" + stats.bad() + " cancelled=" + stats.cancelled());
    for (String e : stats.errors()) {
      System.out.println(e);
    }
  }
}
