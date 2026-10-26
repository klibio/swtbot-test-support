import java.util.Locale;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import com.sun.source.util.DocTrees;

import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;
import jdk.javadoc.doclet.StandardDoclet;

public final class ApiDoclet extends StandardDoclet {
    private Reporter reporter;
    private boolean complete;

    @Override
    public void init(Locale locale, Reporter reporter) {
        super.init(locale, reporter);
        this.reporter = reporter;
    }

    @Override
    public boolean run(DocletEnvironment environment) {
        complete = true;
        for (Element element : environment.getIncludedElements()) {
            if (element instanceof TypeElement) check(element, environment);
        }
        return complete && super.run(environment);
    }

    private void check(Element element, DocletEnvironment environment) {
        if (!element.getModifiers().contains(Modifier.PUBLIC)
                && !element.getModifiers().contains(Modifier.PROTECTED)) return;
        DocTrees trees = environment.getDocTrees();
        if (element.getKind() != ElementKind.RECORD_COMPONENT
                && environment.getElementUtils().getOrigin(element) == Elements.Origin.EXPLICIT
                && trees.getTree(element) != null && trees.getDocCommentTree(element) == null) {
            reporter.print(Diagnostic.Kind.ERROR, element, "Missing explicit public API Javadoc: " + element);
            complete = false;
        }
        if (element instanceof TypeElement) {
            for (Element member : element.getEnclosedElements()) check(member, environment);
        }
    }
}