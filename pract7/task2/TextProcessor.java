package task2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

// обработка текста через Reader/Writer; граница bytes -> chars через InputStreamReader
public class TextProcessor {
  // считывает строки через BufferedReader
  public static int countLines(BufferedReader reader) throws IOException {
    if (reader == null) throw new IllegalArgumentException("reader must not be null");
    int count = 0;
    String line;
    while ((line = reader.readLine()) != null) {
      count++;
    }
    return count;
  }

  // считывает символы через Reader
  public static int countChars(BufferedReader reader) throws IOException {
    if (reader == null) throw new IllegalArgumentException("reader must not be null");
    int count = 0;
    int ch;
    while ((ch = reader.read()) != -1) {
      count++;
    }
    return count;
  }

  // копирует текст с использованием BufferedReader/BufferedWriter
  public static void copyText(InputStream in, OutputStream out) throws IOException {
    if (in == null || out == null) throw new IllegalArgumentException("streams must not be null");
    // bytes -> chars происходит в InputStreamReader по заданной кодировке
    try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
         BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
      String line;
      while ((line = br.readLine()) != null) {
        bw.write(line);
        bw.newLine();
      }
    }
  }

  // чтение по символам через BufferedReader
  public static String readAllChars(BufferedReader reader) throws IOException {
    if (reader == null) throw new IllegalArgumentException("reader must not be null");
    StringBuilder sb = new StringBuilder();
    int ch;
    while ((ch = reader.read()) != -1) {
      sb.append((char) ch);
    }
    return sb.toString();
  }
}
