package task2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

// демонстрация Reader/Writer, считывание строк и символов, пояснение bytes -> chars
public class Task2Demo {
  public static void main(String[] args) throws IOException {
    String input = "Hello\nмир\nJava\n";

    // граница bytes -> chars: InputStreamReader декодирует байты в символы по кодировке
    try (BufferedReader br = new BufferedReader(new StringReader(input))) {
      // считываем строки
      int lines = 0;
      String line;
      // копируем в отдельный ридер для демонстрации - делаем заново
    }

    try (BufferedReader br = new BufferedReader(new StringReader(input));
         BufferedWriter bw = new BufferedWriter(new StringWriter())) {
      String line;
      while ((line = br.readLine()) != null) {
        bw.write(line);
        bw.newLine();
      }
      System.out.println(bw.toString());
    }

    // считаем символы
    try (BufferedReader br = new BufferedReader(new StringReader(input))) {
      int count = TextProcessor.countChars(br);
      System.out.println("chars: " + count);
    }
  }
}
