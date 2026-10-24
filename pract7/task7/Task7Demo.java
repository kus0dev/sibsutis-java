package task7;

// демонстрация частично повреждённого импорта
public class Task7Demo {
  public static void main(String[] args) {
    PartialImporter importer = new PartialImporter();
    String bad = "1;Java;10\nbad;data\n2;DB;12\n";
    ImportResult res = importer.importAllOrNothing(bad);
    if (res instanceof ImportResult.Failure f) {
      System.out.println("errors: " + f.errors().size());
      for (ImportResult.ImportError e : f.errors()) {
        System.out.println("line " + e.lineNumber() + ": " + e.message());
      }
    } else if (res instanceof ImportResult.Success s) {
      System.out.println("ok: " + s.courses().size());
    }
  }
}
