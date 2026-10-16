import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

public class VerifyReports {
    public static void main(String[] arguments) throws Exception {
        Path root = Path.of(arguments[0]);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        for (String runtime : new String[] {"junit4", "jupiter5", "jupiter6"}) {
            int tests = 0;
            int failures = 0;
            int errors = 0;
            try (var files = Files.list(root.resolve(runtime))) {
                for (Path report : files.filter(path -> path.getFileName().toString().startsWith("TEST-")
                        && path.toString().endsWith(".xml")).toList()) {
                    var suite = factory.newDocumentBuilder().parse(report.toFile()).getDocumentElement();
                    tests += Integer.parseInt(suite.getAttribute("tests"));
                    failures += Integer.parseInt(suite.getAttribute("failures"));
                    errors += Integer.parseInt(suite.getAttribute("errors"));
                }
            }
            if (tests == 0 || failures != 0 || errors != 0) {
                throw new IllegalStateException(runtime + ": tests=" + tests + ", failures=" + failures + ", errors=" + errors);
            }
            System.out.println(runtime + ": " + tests + " tests, no failures or errors");
        }
        System.out.println("Reports: " + root);
    }
}